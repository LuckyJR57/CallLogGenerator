package com.calllog.generator;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.View;
import android.widget.*;
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

    private static final int REQUEST_PERMISSIONS = 100;
    private static final Uri CALL_LOG_URI = CallLog.Calls.CONTENT_URI;

    private EditText etCount, etPhone, etMinDuration, etMaxDuration;
    private Spinner spLocation;
    private RadioGroup rgCallType;
    private CheckBox cbLandline;
    private Button btnGenerate;
    private TextView tvResult;
    private LinearLayout resultContainer;
    private Random random = new Random();
    private int callType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            etCount = (EditText) findViewById(R.id.et_count);
            etPhone = (EditText) findViewById(R.id.et_phone);
            etMinDuration = (EditText) findViewById(R.id.et_min_duration);
            etMaxDuration = (EditText) findViewById(R.id.et_max_duration);
            spLocation = (Spinner) findViewById(R.id.sp_location);
            rgCallType = (RadioGroup) findViewById(R.id.rg_call_type);
            cbLandline = (CheckBox) findViewById(R.id.cb_landline);
            btnGenerate = (Button) findViewById(R.id.btn_generate);
            tvResult = (TextView) findViewById(R.id.tv_result);
            resultContainer = (LinearLayout) findViewById(R.id.result_container);

            List<String> cityList = new ArrayList<>();
            cityList.add("随机");
            cityList.addAll(Arrays.asList(LOCATIONS));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cityList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spLocation.setAdapter(adapter);

            rgCallType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int id) {
                    if (id == R.id.rb_call_random) callType = 0;
                    else if (id == R.id.rb_call_in) callType = 1;
                    else if (id == R.id.rb_call_out) callType = 2;
                    else if (id == R.id.rb_call_miss) callType = 3;
                }
            });

            btnGenerate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkPermissionsAndGenerate();
                }
            });
        } catch (Exception e) {
            // If layout inflation fails, show a simple toast via system
            e.printStackTrace();
        }
    }

    private void checkPermissionsAndGenerate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{
                        Manifest.permission.WRITE_CALL_LOG,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_PHONE_STATE
                    },
                    REQUEST_PERMISSIONS
                );
                return;
            }
        }
        doGenerate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                doGenerate();
            } else {
                resultContainer.setVisibility(View.VISIBLE);
                tvResult.setText("需要通话记录权限。\n请到 设置→应用→通话记录生成器→权限 中开启。");
            }
        }
    }

    private void doGenerate() {
        int count;
        try {
            count = Integer.parseInt(etCount.getText().toString().trim());
        } catch (Exception e) {
            count = 10;
        }
        if (count < 1) count = 1;
        if (count > 500) count = 500;

        String specificPhone = etPhone.getText().toString().trim();
        if (specificPhone.length() == 0) specificPhone = null;

        String location = null;
        try {
            String sel = spLocation.getSelectedItem().toString();
            if (!"随机".equals(sel)) location = sel;
        } catch (Exception ignored) {}

        int minDur = 5, maxDur = 300;
        try { minDur = Integer.parseInt(etMinDuration.getText().toString().trim()); } catch (Exception ignored) {}
        try { maxDur = Integer.parseInt(etMaxDuration.getText().toString().trim()); } catch (Exception ignored) {}
        if (minDur < 0) minDur = 0;
        if (maxDur < minDur) maxDur = minDur + 60;

        boolean includeLandline = cbLandline.isChecked();

        long now = System.currentTimeMillis();
        long thirtyDaysAgo = now - 30L * 24 * 3600 * 1000;

        int inserted = 0;
        try {
            for (int i = 0; i < count; i++) {
                ContentValues values = new ContentValues();

                String phone = specificPhone != null ? specificPhone : randomPhone(includeLandline);
                values.put(CallLog.Calls.NUMBER, phone);

                int type = getCallType();
                values.put(CallLog.Calls.TYPE, Integer.valueOf(type));

                int duration = (type == CallLog.Calls.MISSED_TYPE) ? 0
                    : minDur + random.nextInt(maxDur - minDur + 1);
                values.put(CallLog.Calls.DURATION, Long.valueOf(duration));

                long callTime = thirtyDaysAgo + (long)(random.nextDouble() * (now - thirtyDaysAgo));
                values.put(CallLog.Calls.DATE, Long.valueOf(callTime));

                values.put(CallLog.Calls.NEW, Integer.valueOf(type == CallLog.Calls.MISSED_TYPE ? 1 : 0));

                // GEOCODED_LOCATION may not exist on older Android versions, wrap safely
                try {
                    String loc = location != null ? location : randomLocation();
                    values.put(CallLog.Calls.GEOCODED_LOCATION, loc);
                } catch (Exception ignored) {}

                getContentResolver().insert(CALL_LOG_URI, values);
                inserted++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        resultContainer.setVisibility(View.VISIBLE);
        if (inserted == 0) {
            tvResult.setText("写入失败！\n1. 请到 设置→应用→通话记录生成器→权限 开启通话记录权限\n2. 部分手机需在拨号设置中允许第三方修改通话记录");
        } else {
            tvResult.setText("成功写入 " + inserted + " 条通话记录！\n请打开手机拨号/通话记录查看。");
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
        int r = random.nextInt(3);
        if (r == 0) return CallLog.Calls.INCOMING_TYPE;
        if (r == 1) return CallLog.Calls.OUTGOING_TYPE;
        return CallLog.Calls.MISSED_TYPE;
    }
}
