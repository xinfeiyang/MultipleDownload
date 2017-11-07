package com.security.thread.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.security.thread.R;
import com.security.thread.util.DownloadListener;
import com.security.thread.util.DownloadManager;

public class MainActivity extends AppCompatActivity {

    private TextView tv_file_name1, tv_progress1, tv_file_name2, tv_progress2;
    private Button btn_download1, btn_download2, btn_download_all;
    private ProgressBar pb_progress1, pb_progress2;

    private String wechatUrl = "http://dldir1.qq.com/weixin/android/weixin657android1040.apk";
    private String qqUrl = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";

    //下载管理器;
    private DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadManager=DownloadManager.getInstance();
        initViews();
        initDownloads();
    }

    /**
     * 下载;
     */
    private void initDownloads() {
        downloadManager.add(wechatUrl, new DownloadListener() {
            @Override
            public void onFinished() {
                Toast.makeText(MainActivity.this, "微信下载完成!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(float progress) {
                pb_progress1.setProgress((int) (progress * 100));
                tv_progress1.setText(String.format("%.2f", progress * 100) + "%");
            }

            @Override
            public void onPause() {
                Toast.makeText(MainActivity.this, "微信暂停了!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                tv_progress1.setText("0%");
                pb_progress1.setProgress(0);
                btn_download1.setText("微信下载");
                Toast.makeText(MainActivity.this, "微信下载已取消!", Toast.LENGTH_SHORT).show();
            }
        });

        downloadManager.add(qqUrl, new DownloadListener() {
            @Override
            public void onFinished() {
                Toast.makeText(MainActivity.this, "QQ下载完成!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(float progress) {
                pb_progress2.setProgress((int) (progress * 100));
                tv_progress2.setText(String.format("%.2f", progress * 100) + "%");
            }

            @Override
            public void onPause() {
                Toast.makeText(MainActivity.this, "QQ暂停了!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                tv_progress2.setText("0%");
                pb_progress2.setProgress(0);
                btn_download2.setText("QQ下载");
                Toast.makeText(MainActivity.this, "QQ下载已取消!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 初始化View;
     */
    private void initViews() {
        tv_file_name1 = (TextView) findViewById(R.id.tv_file_name1);
        tv_progress1 = (TextView) findViewById(R.id.tv_progress1);
        pb_progress1 = (ProgressBar) findViewById(R.id.pb_progress1);
        btn_download1 = (Button) findViewById(R.id.btn_download1);
        tv_file_name1.setText("微信");

        tv_file_name2 = (TextView) findViewById(R.id.tv_file_name2);
        tv_progress2 = (TextView) findViewById(R.id.tv_progress2);
        pb_progress2 = (ProgressBar) findViewById(R.id.pb_progress2);
        btn_download2 = (Button) findViewById(R.id.btn_download2);
        tv_file_name2.setText("qq");

        btn_download_all = (Button) findViewById(R.id.btn_download_all);
    }


    /**
     * 下载或暂停下载
     * @param view
     */
    public void downloadOrPause(View view) {
        switch (view.getId()) {
            case R.id.btn_download1:
                if (!downloadManager.isDownloading(wechatUrl)) {
                    downloadManager.download(wechatUrl);
                    btn_download1.setText("暂停");

                } else {
                    btn_download1.setText("下载");
                    downloadManager.pause(wechatUrl);
                }
                break;
            case R.id.btn_download2:
                if (!downloadManager.isDownloading(qqUrl)) {
                    downloadManager.download(qqUrl);
                    btn_download2.setText("暂停");
                } else {
                    btn_download2.setText("下载");
                    downloadManager.pause(qqUrl);
                }
                break;
        }
    }

    /**
     * 全部下载或者全部暂停;
     * @param view
     */
    public void downloadOrPauseAll(View view) {
        if (!downloadManager.isDownloading(wechatUrl, qqUrl)) {
            btn_download1.setText("暂停");
            btn_download2.setText("暂停");
            btn_download_all.setText("全部暂停");
            downloadManager.download(wechatUrl, qqUrl);//最好传入个String[]数组进去
        } else {
            downloadManager.pause(wechatUrl, qqUrl);
            btn_download1.setText("下载");
            btn_download2.setText("下载");
            btn_download_all.setText("全部下载");
        }
    }

    /**
     * 进入RxJava;
     * @param view
     */
    public void enter(View view){
        Intent intent=new Intent(MainActivity.this,RxJavaActivity.class);
        startActivity(intent);
    }

    public void enterService(View view){
        Intent intent=new Intent(MainActivity.this,ServiceDownloadActivity.class);
        startActivity(intent);
    }

    /**
     * 取消下载
     * @param view
     */
    public void cancel(View view) {

        switch (view.getId()) {
            case R.id.btn_cancel1:
                downloadManager.cancel(wechatUrl);
                break;
            case R.id.btn_cancel2:
                downloadManager.cancel(qqUrl);
                break;
        }
    }

    public void cancelAll(View view) {
        downloadManager.cancel(wechatUrl, qqUrl);
        btn_download1.setText("下载");
        btn_download2.setText("下载");
        btn_download_all.setText("全部下载");
    }


}
