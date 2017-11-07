package com.security.thread.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import com.security.thread.R;
import com.security.thread.service.DownloadService;

/**
 *利用服务操作下载任务;
 */
public class ServiceDownloadActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_start;
    private Button btn_pause;
    private Button btn_cancel;

    private DownloadService.DownloadBinder binder;

    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder= (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servicedownload);
        initView();
    }

    /**
     * 初始化View;
     */
    private void initView() {
        btn_start = (Button) findViewById(R.id.start_download);
        btn_pause = (Button) findViewById(R.id.pause_download);
        btn_cancel = (Button) findViewById(R.id.cancel_download);

        btn_start.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        Intent intent=new Intent(this, DownloadService.class);
        startService(intent);//开启服务;
        bindService(intent,connection,BIND_AUTO_CREATE);//绑定服务;
    }

    @Override
    public void onClick(View v) {
        if(binder==null){
            return;
        }
        switch (v.getId()){
            case R.id.start_download:
                String url="https://dldir1.qq.com/qqfile/qq/QQ8.9.5/22062/QQ8.9.5.exe";
                binder.startDownload(url);
                break;

            case R.id.pause_download:
                binder.pauseDownload();
                break;

            case R.id.cancel_download:
                binder.cancelDownload();
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(connection!=null){
            unbindService(connection);
        }
    }
}
