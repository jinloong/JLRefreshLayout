package com.siyueyihao.refresh;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.siyueyihao.refresh.lib.JLRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private JLRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        refreshLayout = (JLRefreshLayout) findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(new JLRefreshLayout.OnRefreshListener() {
            @Override
            protected void onRefresh() {
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.stopRefresh(false);
                    }
                },2000);
            }
        });
        refreshLayout.startRefresh();
    }
}
