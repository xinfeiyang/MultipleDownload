package com.security.thread.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.security.thread.R;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 测试RxJava;
 */
public class RxJavaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rxjava);
        textRxJava();
    }

    private static String TAG="TAG";

    private void textRxJava() {

        Observable.just("冯朗","郑玲玲","郑艳玲").subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.i(TAG, "onSubscribe:"+Thread.currentThread().getName());
            }

            @Override
            public void onNext(String s) {
                Log.i(TAG, "onNext:"+s+";"+Thread.currentThread().getName());
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, "onError:"+e+";"+Thread.currentThread().getName());
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete"+Thread.currentThread().getName());
            }
        });


    }
}
