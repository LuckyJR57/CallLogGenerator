package com.calllog.generator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final String[] PHONE_PREFIXES = {
        "130","131","132","133","134","135","136","137","138","139",
        "150","151","152","153","155","156","157","158","159",
        "170","171","172","173","174","175","176","177","178",
        "180","181","182","183","184","185","186","187","188","189",
        "190","191","192","193","195","196","197","198","199"
    };
    private static final String[] LANDLINE_PREFIXES = {
        "010","020","021","022","023","024","025","027","028","029",
        "0311","0371","0512","0531","0571","0574","0591","0592","0755","0769"
    };
    private static final String[] LOCATIONS = {
        "北京","上海","广州","深圳","杭州","南京","成都","武汉","重庆","天津",
        "苏州","西安","长沙","青岛","郑州","大连","东莞","宁波","厦门","福州",
        "合肥","无锡","沈阳","济南"
    };

    private EditText etCount, etPhone, etMinDuration, etMaxDuration;
    private Spinner spLocation;
    private RadioGroup rgCallType, rgAnswerType;
    private CheckBox cbLandline;
    private Button btnGenerate;
    private TextView tvResult;
    private LinearLayout resultContainer;
    private Random random = new Random();

    private int callType = 0; // 0=随机, 1=呼入, 2=呼出, 3=未接
    private int answerType = 0; // 0=全部, 1=已接, 2=未接

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCount = findViewById(R.id.et_count);
        etPhone = findViewById(R.id.et_phone);
        etMinDuration = findViewById(R.id.et_min_duration);
        etMaxDuration = findViewById(R.id.et_max_duration);
        spLocation = findViewById(R.id.sp_location);
        rgCallType = findViewById(R.id.rg_call_type);
        rgAnswerType = findViewById(R.id.rg_answer_type);
        cbLandline = findViewById(R.id.cb_landline);
        btnGenerate = findViewById(R.id.btn_generate);
        tvResult = findViewById(R.id.tv_result);
        resultContainer = findViewById(R.id.result_container);

        // 城市列表
        List<String> cityList = new ArrayList<>();
        cityList.add("随机");
        cityList.addAll(Arrays.asList(LOCATIONS));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, cityList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLocation.setAdapter(adapter);

        rgCallType.setOnCheckedChangeListener((group, id) -> {
            if (id == R.id.rb_call_random) callType = 0;
            else if (id == R.id.rb_call_in) callType = 1;
            else if (id == R.id.rb_call_out) callType = 2;
            else if (id == R.id.rb_call_miss) callType = 3;
        });

        rgAnswerType.setOnCheckedChangeListener((group, id) -> {
            if (id == R.id.rb_answer_all) answerType = 0;
            else if (id == R.id.rb_answer_yes) answerType = 1;
            else if (id == R.id.rb_answer_no) answerType = 2;
        });

        btnGenerate.setOnClickListener(v -> generate());
    }

    private void generate() {
        int count;
        try {
            count = Integer.parseInt(etCount.getText().toString().trim());
        } catch (NumberFormatException e) {
            count = 10;
        }
        if (count < 1) count = 1;
        if (count > 500) count = 500;

        String specificPhone = etPhone.getText().toString().trim();
        if (specificPhone.isEmpty()) specificPhone = null;

        String location = spLocation.getSelectedItem().toString();
        if ("随机".equals(location)) location = null;

        int minDur = 5, maxDur = 300;
        try { minDur = Integer.parseInt(etMinDuration.getText().toString().trim()); } catch (Exception ignored) {}
        try { maxDur = Integer.parseInt(etMaxDuration.getText().toString().trim()); } catch (Exception ignored) {}

        boolean includeLandline = cbLandline.isChecked();

        List<ContentValues> logs = new ArrayList<>();
        long now = System.currentTimeMillis();
        long thirtyDaysAgo = now - 30L * 24 * 3600 * 1000;

        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();

            // 号码
            String phone = specificPhone != null ? specificPhone : randomPhone(includeLandline);
            values.put(CallLog.Calls.NUMBER, phone);

            // 类型
            int type = getCallType();
            values.put(CallLog.Calls.TYPE, type);

            // 时长
            int duration = (type == CallLog.Calls.MISSED_TYPE) ? 0 : minDur + random.nextInt(maxDur - minDur + 1);
            values.put(CallLog.Calls.DURATION, duration);

            // 时间
            long callTime = thirtyDaysAgo + (long)(random.nextDouble() * (now - thirtyDaysAgo));
            values.put(CallLog.Calls.DATE, callTime);

            // 新/旧标记
            values.put(CallLog.Calls.NEW, type == CallLog.Calls.MISSED_TYPE ? 1 : 0);

            // 归属地
            String loc = location != null ? location : randomLocation();
            values.put(CallLog.Calls.GEOCODED_LOCATION, loc);

            // 是否接通
            boolean answered = true;
            if (answerType == 1) answered = true;
            else if (answerType == 2) answered = false;
            else answered = (type != CallLog.Calls.MISSED_TYPE);

            logs.add(values);
        }

        // 写入系统通话记录
        int inserted = 0;
        for (ContentValues values : logs) {
            try {
                getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
                inserted++;
            } catch (SecurityException e) {
                // 权限不足
                break;
            }
        }

        // 显示结果
        resultContainer.setVisibility(View.VISIBLE);
        if (inserted == 0) {
            tvResult.setText("写入失败，请确保已授予通话记录权限。\n请到 设置 → 应用 → 通话记录生成器 → 权限 中开启。");
        } else {
            tvResult.setText("✅ 成功写入 " + inserted + " 条通话记录！\n请打开手机拨号/通话记录查看。");
        }
    }

    private String randomPhone(boolean includeLandline) {
        if (includeLandline && random.nextBoolean()) {
            String area = LANDLINE_PREFIXES[random.nextInt(LANDLINE_PREFIXES.length)];
            StringBuilder sb = new StringBuilder(area);
            for (int i = 0; i < 8; i++) sb.append(random.nextInt(10));
            return sb.toString();
        }
        String prefix = PHONE_PREFIXES[random.nextInt(PHONE_PREFIXES.length)];
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String randomLocation() {
        return LOCATIONS[random.nextInt(LOCATIONS.length)];
    }

    private int getCallType() {
        if (callType == 1) return CallLog.Calls.INCOMING_TYPE;
        if (callType == 2) return CallLog.Calls.OUTGOING_TYPE;
        if (callType == 3) return CallLog.Calls.MISSED_TYPE;
        // 随机
        int r = random.nextInt(3);
        if (r == 0) return CallLog.Calls.INCOMING_TYPE;
        if (r == 1) return CallLog.Calls.OUTGOING_TYPE;
        return CallLog.Calls.MISSED_TYPE;
    }
}
