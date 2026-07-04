package com.calllog.generator;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Hello World - 通话记录生成器 v1.0");
        tv.setTextSize(20);
        tv.setPadding(32, 32, 32, 32);
        setContentView(tv);
    }
}
