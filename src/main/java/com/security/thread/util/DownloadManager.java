package com.security.thread.util;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 下载管理器，断点续传；
 */
public class DownloadManager {

    //文件下载任务索引,Url为键,用来唯一区别并操作下载的文件
    private Map<String, DownloadTask> downloadTasks;

    private static DownloadManager downloadManager;

    /**
     * 下载文件;
     * @param urls
     */
    public void download(String...urls){
        for(int i=0,lenth=urls.length;i<lenth;i++){
            String url=urls[i];
            if(downloadTasks.containsKey(url)){
                downloadTasks.get(url).start();
            }
        }
    }

    /**
     * 暂停
     */
    public void pause(String... urls) {
        //单任务暂停或多任务暂停下载
        for (int i = 0, length = urls.length; i < length; i++) {
            String url = urls[i];
            if (downloadTasks.containsKey(url)) {
                downloadTasks.get(url).pause();
            }
        }
    }

    /**
     * 取消下载
     */
    public void cancel(String... urls) {
        //单任务取消或多任务取消下载
        for (int i = 0, length = urls.length; i < length; i++) {
            String url = urls[i];
            if (downloadTasks.containsKey(url)) {
                downloadTasks.get(url).cancel();
            }
        }
    }

    /**
     * 添加下载任务
     */
    public void add(String url,DownloadListener listener) {
        if(downloadTasks.get(url)==null){
            downloadTasks.put(url,new DownloadTask(url,listener));
        }
    }

    /**
     *这里传一个url就是判断一个下载任务
     *多个url数组适合下载管理器判断是否作操作全部下载或全部取消下载
     * @param urls
     * @return
     */
    public boolean isDownloading(String...urls){
        boolean result=false;
        for (int i = 0, length = urls.length; i < length; i++) {
            String url = urls[i];
            if (downloadTasks.containsKey(url)) {
                result = downloadTasks.get(url).isDownloading();
            }
        }
        return result;
    }


    /**
     * 私有化构造方法;
     */
    private DownloadManager(){
        downloadTasks=new HashMap<>();
    }

    /**
     * 单例模式;
     */
    public static DownloadManager getInstance(){
        if(downloadManager==null){
            synchronized (DownloadManager.class){
                if(downloadManager==null){
                    downloadManager=new DownloadManager();
                }
            }
        }
        return downloadManager;
    }

}
