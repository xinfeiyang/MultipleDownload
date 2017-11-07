package com.security.thread.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 联网工具类,基于OKHttp;
 */
public class HttpUtil {

    private OkHttpClient okHttpClient;
    private static HttpUtil instance;
    private final static long CONNECT_TIMEOUT = 60;//超时时间，秒
    private final static long READ_TIMEOUT = 60;//读取时间，秒
    private final static long WRITE_TIMEOUT = 60;//写入时间，秒

    /**
     * 分段下载;
     * @param url:文件下载的url;
     * @param start:下载起始位置
     * @param end :瞎子结束位置;
     * @param callback:结果回调
     */
    public void downloadByRange(String url,long start,long end,Callback callback){
        Request request=new Request.Builder()
                .header("RANGE","bytes="+start+"-"+end)
                .url(url)
                .build();
        doAsync(request,callback);
    }



    /**
     * 获得文件长度;
     * @param url:下载文件的url
     * @param callback:结果回调
     * @throws IOException :异常
     */
    public void getContentLength(String url, Callback callback) throws IOException {
        // 创建一个Request
        Request request = new Request.Builder()
                .url(url)
                .build();
        doAsync(request, callback);
    }

    /**
     * 同步GET请求
     */
    public void doGetSync(String url) throws IOException {
        //创建一个Request
        Request request = new Request.Builder()
                .url(url)
                .build();
        doSync(request);
    }

    /**
     * 执行同步请求
     */
    private Response doSync(Request request) throws IOException {
        //创建请求会话
        Call call = okHttpClient.newCall(request);
        //同步执行会话请求
        return call.execute();
    }


    /**
     * 执行异步请求;
     */
    private void doAsync(Request request, Callback callback){
        //创建请求会话
        Call call = okHttpClient.newCall(request);
        //同步执行会话请求
        call.enqueue(callback);
    }



    /**
     * 私有化构造方法,配置OkHttpClient;
     */
    private HttpUtil(){
        okHttpClient=new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT,TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 单利模式;
     * @return :HttpUtil
     */
    public static HttpUtil getInstance(){
        if(instance==null){
            synchronized (HttpUtil.class){
                if(instance==null){
                    instance=new HttpUtil();
                }
            }
        }
        return instance;
    }
}
