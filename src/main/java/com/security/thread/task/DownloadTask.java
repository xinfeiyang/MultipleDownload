package com.security.thread.task;

import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;

import com.security.thread.listener.DownloadListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 异步下载任务;
 */
public class DownloadTask extends AsyncTask<String,Integer,Integer> {


    private final static long CONNECT_TIMEOUT = 60;//超时时间，秒
    private final static long READ_TIMEOUT = 60;//读取时间，秒
    private final static long WRITE_TIMEOUT = 60;//写入时间，秒

    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSED=2;
    public static final int TYPE_CANCELED=3;
    private String DEFAULT_FILE_DIR;//默认下载目录

    private DownloadListener listener;//下载进度监听;

    private boolean isCanceled=false;//取消下载

    private boolean isPaused=false;//暂停下载;

    private int lastProgress;//上次的下载进度;

    public DownloadTask(DownloadListener listener){
        this.listener=listener;
    }

    /**
     * 在子线程中运行;
     * @param params:运行线程需要的参数;
     * @return :线程执行后的结果,如TYPE_SUCCESS、TYPE_FAILED等;
     */
    @Override
    protected Integer doInBackground(String... params) {
        InputStream is=null;
        RandomAccessFile savedFile=null;
        File file=null;
        try {
            long downloadedLength=0;//记录已下载的文件的长度;
            String downloadUrl=params[0];//下载任务地址;
            file=new File(getDefaultDownloadDirectory(),getFileName(downloadUrl));//文件保存地址;
            if(file.exists()){
                downloadedLength=file.length();//若文件已存在,获取已下载文件的大小;
            }
            long contentLength=getContentLength(downloadUrl);//获取文件的总大小;
            if(contentLength==0){
                return TYPE_FAILED;//文件下载失败;
            }else if(contentLength==downloadedLength){
                return TYPE_SUCCESS;//已下载字节和文件总字节相等,说明已下载完成;
            }
            OkHttpClient client=getOkHttpClient();
            //断点下载,指定从哪个字节开始下载;
            Request request=new Request.Builder()
                    .addHeader("RANGE","bytes="+downloadedLength+"-")
                    .url(downloadUrl).build();
            Response response=client.newCall(request).execute();
            if(response!=null&&response.isSuccessful()){//返回结果成功!
                is=response.body().byteStream();
                savedFile=new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);//跳转已下载的字节;
                byte[] buffer=new byte[1024];
                int total=0;
                int len=0;
                while((len=is.read(buffer))!=-1){
                    if(isCanceled){
                        return TYPE_CANCELED;
                    }else if(isPaused){
                        return TYPE_PAUSED;
                    }else{
                        total+=len;
                        savedFile.write(buffer,0,len);
                        //计算已下载的百分比;
                        int progress= (int) ((total+downloadedLength)*100/contentLength);
                        //发布已下载的进度;
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(is!=null){
                    is.close();
                }
                if(savedFile!=null){
                    savedFile.close();
                }
                if(isCanceled&&file!=null){//取消下载,并且已下载的文件存在,则删除已下载的文件;
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    /**
     * 更新下载进度;
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progress=values[0];
        if(progress>lastProgress){
            listener.onProgress(progress);
            lastProgress=progress;
        }
    }


    /**
     * 下载完成的回调结果
     * @param status:返回结果的回调;
     */
    @Override
    protected void onPostExecute(Integer status) {
        super.onPostExecute(status);
        switch (status){
            case TYPE_SUCCESS://下载成功！
                listener.onSuccess();
                break;

            case TYPE_FAILED://下载失败；
                listener.onFailed();
                break;

            case TYPE_PAUSED://暂停下载;
                listener.onPaused();
                break;

            case TYPE_CANCELED://取消下载;
                listener.onCanceled();
                break;
        }
    }

    /**
     * 暂停下载;
     */
    public void pauseDownload(){
        isPaused=true;
    }

    /**
     * 取消下载;
     */
    public void cancelDownload(){
        isCanceled=true;
    }

    /**
     * 获取文件的长度;
     * @param downloadUrl:下载地址;
     * @return
     */
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = getOkHttpClient();
        Request request=new Request.Builder().url(downloadUrl).build();
        Response response=client.newCall(request).execute();//执行同步任务;
        if(response!=null&&response.isSuccessful()){//执行结果成功;
            long contentLength=response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }

    /**
     * 获取OkHttpClient;
     * @return :返回OkHttpClient;
     */
    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();
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

    /**
     * 获取下载文件的名称
     * @param url
     * @return
     */
    public String getFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
