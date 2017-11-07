package com.security.thread.util;

/**
 * 下载监听
 */
public interface DownloadListener {

    /**
     * 下载完成;
     */
    void onFinished();

    /**
     * 展示进度
     * @param progress:当前进度
     */
    void onProgress(float progress);

    /**
     * 暂停
     */
    void onPause();

    /**
     * 取消
     */
    void onCancel();

}
