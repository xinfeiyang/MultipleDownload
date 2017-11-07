package com.security.thread.util;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Call;
import okhttp3.Response;

/**
 * RandomAccessFile的mode:
 * "rw":打开以便读取和写入。如果该文件尚不存在，则尝试创建该文件。
 * "rwd":打开以便读取和写入，对于 "rw"，还要求对文件内容的每个更新都同步写入到底层存储设备
 */

/**
 * 多线程下载任务;
 */
public class DownloadTask extends Handler {

    public String TAG="TAG";

    private String DEFAULT_FILE_DIR;//默认下载目录

    private final int THREAD_COUNT = 4;//下载线程数量
    private boolean isDownloading = false;//文件是否正在下载
    private boolean pause;//是否暂停
    private boolean cancel;//是否取消下载

    private int childCancelCount;//子线程取消数量
    private int childPauseCount;//子线程暂停数量
    private int childFinishCount;//子线程完成下载数量

    private long fileLength;//文件大小;
    private DownloadListener listener;//下载监听;

    private String url;//文件下载地址;
    private long[] mProgress;//各个子线程下载进度集合
    private File[] mCacheFiles;//各个子线程下载缓存数据文件
    private HttpUtil httpUtil;

    private File tempFile;//临时占位文件;

    private final int MSG_PROGRESS = 1;//下载进度
    private final int MSG_FINISH = 2;//完成下载
    private final int MSG_PAUSE = 3;//暂停下载
    private final int MSG_CANCEL = 4;//取消下载


    /**
     * 构造方法;
     * @param url
     * @param listener
     */
    public DownloadTask(String url,DownloadListener listener){
        this.url=url;
        this.listener=listener;
        this.mProgress = new long[THREAD_COUNT];
        this.mCacheFiles = new File[THREAD_COUNT];
        httpUtil=HttpUtil.getInstance();
    }

    /**
     * 开始下载;
     */
    public synchronized void start(){
        try {
            if(isDownloading){//若当前正在下载,直接返回;
                return;
            }
            isDownloading=true;//设定当前正在下载;
            httpUtil.getContentLength(url, new okhttp3.Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.code()!=200){//网络连接失败;
                        close(response.body());
                        resetStatus();
                        return;
                    }
                    fileLength = response.body().contentLength();
                    close(response.body());//关闭资源;
                    //在本地创建一个与资源同样大小的文件来占位
                    tempFile=new File(getDefaultDownloadDirectory(),getFileName(url)+".tmp");
                    if(!tempFile.getParentFile().exists()){
                        tempFile.getParentFile().mkdirs();//若文件不存在,则创建文件;
                    }
                    RandomAccessFile tempAccessFile=new RandomAccessFile(tempFile,"rw");
                    tempAccessFile.setLength(fileLength);//设置文件大小;

                    //将下载任务分配到各个线程;
                    long blockSize=fileLength/THREAD_COUNT;//计算每个线程的理论下载数量;

                    //为每个线程分配下载任务;
                    for(int threadId=0;threadId<THREAD_COUNT;threadId++){
                        long start=threadId*blockSize;//每个线程分配下载的开始位置;
                        long end=(threadId+1)*blockSize-1;//每个线程分配下载的开始位置;
                        if(threadId==(THREAD_COUNT-1)){//如果是最后一个线程
                            end=fileLength-1;
                        }
                        download(threadId,start,end);//开启线程下载;
                    }

                }


                @Override
                public void onFailure(Call call, IOException e) {
                    resetStatus();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            //连接失败,下载状态重置;
            resetStatus();
        }
    }

    /**
     * 线程下载;
     * @param threadId:线程id;
     * @param start:线程开始下载位置;
     * @param end:线程结束下载位置;
     */
    private void download(final int threadId, final long start, long end) throws IOException {
        long newStartIndex=start;
        //分段请求网络连接,分段将文件保存到本地.
        //加载下载位置缓存数据文件
        final File cacheFile=new File(getDefaultDownloadDirectory(),"thread"+threadId+"_"+getFileName(url)+".cache");
        mCacheFiles[threadId] = cacheFile;//缓存文件;
        final RandomAccessFile cacheAccessFile = new RandomAccessFile(cacheFile,"rwd");
        if(cacheFile.exists()){//如果文件存在;
            String startIndexStr = cacheAccessFile.readLine();
            try {
                newStartIndex = Integer.parseInt(startIndexStr);//重新设置下载起点
                Log.i(TAG, threadId+"start:"+newStartIndex+";"+cacheAccessFile.length());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        //重新设置文件的起始下载位置;
        final long finalStartIndex=newStartIndex;
        //分段下载;
        httpUtil.downloadByRange(url, finalStartIndex, end, new okhttp3.Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code()!=206){//206:请求部分资源成功码,表示服务器支持断点续传
                    resetStatus();
                    return;
                }
                InputStream is=response.body().byteStream();//获取下载资源输入流;
                RandomAccessFile tempAccessFile=new RandomAccessFile(tempFile,"rw");//获取前面已经创建的文件;
                //设置到此文件开头测量到的文件指针偏移量,在该位置发生下一个读取或写入操作。
                tempAccessFile.seek(finalStartIndex);
                //将网络流写入本地;
                byte[] buffer=new byte[1024];
                int length=-1;
                int total=0;//记录本次文件下载的大小;
                int progress= (int) finalStartIndex;
                while((length=is.read(buffer))!=-1){
                    if(cancel){//取消下载;
                        close(cacheAccessFile,is,response.body());//关闭资源;
                        cleanFile(cacheFile);//清除下载的临时文件;
                        sendEmptyMessage(MSG_CANCEL);
                        return;
                    }
                    if (pause) {//暂停下载;
                        //关闭资源
                        close(cacheAccessFile, is, response.body());
                        //发送暂停消息
                        sendEmptyMessage(MSG_PAUSE);
                        return;
                    }
                    //将数据写入RandomAccessFile中;
                    tempAccessFile.write(buffer,0,length);
                    total+=length;
                    progress+=length;

                    //将当前现在到的位置保存到单个线程临时文件中
                    cacheAccessFile.seek(0);
                    cacheAccessFile.write((progress + "").getBytes("UTF-8"));
                    //发送进度消息
                    mProgress[threadId] = progress-start;
                    sendEmptyMessage(MSG_PROGRESS);
                }

                //关闭资源
                close(cacheAccessFile,is,response.body());
                // 删除临时文件
                cleanFile(cacheFile);
                //发送完成消息
                sendEmptyMessage(MSG_FINISH);

            }

            @Override
            public void onFailure(Call call, IOException e) {
                isDownloading=false;
            }
        });
    }


    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if(listener==null){//接口回调为null,直接返回;
            return;
        }

        switch (msg.what){
            case MSG_PROGRESS://进度;
                long progress=0;
                for(int i=0,length=mProgress.length;i<length;i++){
                    progress+=mProgress[i];
                }
                listener.onProgress(progress*1.0f/fileLength);
                break;

            case MSG_PAUSE://暂停;
                childPauseCount++;
                //等待所有的线程完成暂停，才是真正意义的暂停;
                if(childPauseCount%THREAD_COUNT!=0){
                    return;
                }
                resetStatus();
                listener.onPause();
                break;

            case MSG_FINISH://完成;
                childFinishCount++;
                //等待所有的线程完成，才是真正意义的下载完成;
                if(childFinishCount%THREAD_COUNT!=0){
                    return;
                }
                //下载完毕后，重命名目标文件名
                tempFile.renameTo(new File(getDefaultDownloadDirectory(),getFileName(url)));
                resetStatus();
                listener.onFinished();
                break;

            case MSG_CANCEL://取消;
                childCancelCount++;
                //等待所有的线程取消操作，才是真正意义的取消下载;
                if(childCancelCount%THREAD_COUNT!=0){
                    return;
                }
                resetStatus();
                mProgress=new long[THREAD_COUNT];
                listener.onCancel();
                break;

        }

    }

    /**
     * 发送消息到轮回器
     * @param what
     */
    private void sendMessage(int what) {
        //发送暂停消息
        Message message = new Message();
        message.what = what;
        sendMessage(message);
    }

    /**
     * 关闭资源
     * @param closeables
     */
    private void close(Closeable... closeables) {
        int length = closeables.length;
        try {
            for (int i = 0; i < length; i++) {
                Closeable closeable = closeables[i];
                if (closeable!=null)
                    closeables[i].close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            for (int i = 0; i < length; i++) {
                closeables[i] = null;
            }
        }
    }



    /**
     * 删除临时文件
     */
    private void cleanFile(File... files) {
        for (int i = 0, length = files.length; i < length; i++) {
            if (files[i]!=null){
                files[i].delete();
            }
        }
    }

    /**
     * 获取下载状态
     * @return boolean
     */
    public boolean isDownloading() {
        return isDownloading;
    }


    /**
     * 暂停下载
     */
    public void pause() {
        pause = true;
    }

    /**
     * 取消
     */
    public void cancel() {
        cancel = true;
        cleanFile(tempFile);
        if (!isDownloading) {
            if (listener!=null) {
                cleanFile(mCacheFiles);
                resetStatus();
                listener.onCancel();
            }
        }
    }

    /**
     * 重置下载状态;
     */
    private void resetStatus(){
        pause = false;
        cancel = false;
        isDownloading = false;
    }


    /**
     * 获取下载文件的名称
     * @param url
     * @return
     */
    public String getFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * 默认下载目录
     * @return
     */
    private String getDefaultDownloadDirectory() {
        if (TextUtils.isEmpty(DEFAULT_FILE_DIR)) {
            DEFAULT_FILE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "ThreadDownloadTest" + File.separator;
        }
        return DEFAULT_FILE_DIR;
    }
}
