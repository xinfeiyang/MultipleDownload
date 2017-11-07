package com.security.thread.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.Toast;
import com.security.thread.R;
import com.security.thread.activity.ServiceDownloadActivity;
import com.security.thread.listener.DownloadListener;
import com.security.thread.task.DownloadTask;
import java.io.File;

/**
 * 操作下载任务的Service;
 */

public class DownloadService extends Service {

    private DownloadTask downloadTask;
    private String downloadUrl;

    private DownloadListener listener=new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1,getNotification("Downloading...",progress));
        }

        @Override
        public void onSuccess() {
            downloadTask=null;
            //下载成功时将前台通知关闭,并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download Success",-1));
            Toast.makeText(DownloadService.this,"Download Success", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downloadTask=null;
            Toast.makeText(DownloadService.this, "Paused", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            downloadTask=null;
            //下载失败时将前台服务通知关闭,并创建一个下载失败的通知;
            stopForeground(true);
            getNotificationManager().notify(1,getNotification("Download Failed",-1));
            Toast.makeText(DownloadService.this,"Download Failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downloadTask=null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
        }
    };

    private DownloadBinder binder=new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * 自定义下载Binder;
     */
    public class DownloadBinder extends Binder{

        public void startDownload(String url){
            if(downloadTask==null){
                downloadUrl=url;
                downloadTask=new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
            }
        }

        public void pauseDownload(){
            if(downloadTask!=null){
                downloadTask.pauseDownload();
            }
        }

        public void cancelDownload(){
            if(downloadTask!=null){
                downloadTask.cancelDownload();
            }else{
                if(downloadUrl!=null){
                    File file=new File(getDefaultDownloadDirectory(),getFileName(downloadUrl));
                    if(file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String DEFAULT_FILE_DIR;//默认下载目录
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

    /**
     * 获取通知管理器;
     * @return :NotificationManager通知管理器;
     */
    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     *获取Notification;
     * @param title :标题;
     * @param progress :当前进度;
     * @return
     */
    private Notification getNotification(String title,int progress){
        Intent intent=new Intent(this, ServiceDownloadActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if(progress>0){
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }

}
