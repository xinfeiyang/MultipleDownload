package com.security.thread.listener;

/**
 * 下载的监听
 */
public interface DownloadListener {

    /**
     * 下载进度
     * @param progress:当前的进度;
     */
    void onProgress(int progress);


    /**
     * 下载完成;
     */
    void onSuccess();


    /**
     * 下载暂停;
     */
    void onPaused();


    /**
     * 下载失败;
     */
    void onFailed();


    /**
     * 取消下载;
     */
    void onCanceled();
}
