package com.calllog.generator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
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

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_CONTACTS_PERMISSION = 101;

    private EditText etCount, etPhone, etMinDuration, etMaxDuration;
    private Spinner spLocation;
    private RadioGroup rgCallType;
    private CheckBox cbLandline, cbContacts;
    private Button btnGenerate, btnStartDate, btnEndDate, btnStartTime, btnEndTime, btnSelectContacts;
    private TextView tvResult, tvSelectedContacts;
    private LinearLayout resultContainer, contactsArea;
    private Random random = new Random();
    private int callType = 0;

    private Calendar startCal, endCal;
    private SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Selected contacts: name -> phone
    private List<String[]> selectedContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCount = (EditText) findViewById(R.id.et_count);
        etPhone = (EditText) findViewById(R.id.et_phone);
        etMinDuration = (EditText) findViewById(R.id.et_min_duration);
        etMaxDuration = (EditText) findViewById(R.id.et_max_duration);
        spLocation = (Spinner) findViewById(R.id.sp_location);
        rgCallType = (RadioGroup) findViewById(R.id.rg_call_type);
        cbLandline = (CheckBox) findViewById(R.id.cb_landline);
        cbContacts = (CheckBox) findViewById(R.id.cb_contacts);
        btnGenerate = (Button) findViewById(R.id.btn_generate);
        btnStartDate = (Button) findViewById(R.id.btn_start_date);
        btnEndDate = (Button) findViewById(R.id.btn_end_date);
        btnStartTime = (Button) findViewById(R.id.btn_start_time);
        btnEndTime = (Button) findViewById(R.id.btn_end_time);
        btnSelectContacts = (Button) findViewById(R.id.btn_select_contacts);
        tvResult = (TextView) findViewById(R.id.tv_result);
        tvSelectedContacts = (TextView) findViewById(R.id.tv_selected_contacts);
        resultContainer = (LinearLayout) findViewById(R.id.result_container);
        contactsArea = (LinearLayout) findViewById(R.id.contacts_area);

        // Init calendars
        startCal = Calendar.getInstance();
        startCal.add(Calendar.DAY_OF_MONTH, -30);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);

        endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);

        updateDateButtons();

        // Location spinner
        List<String> cityList = new ArrayList<>();
        cityList.add("随机");
        for (String loc : LOCATIONS) cityList.add(loc);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, cityList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spLocation.setAdapter(adapter);

        // Call type
        rgCallType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int id) {
                if (id == R.id.rb_call_random) callType = 0;
                else if (id == R.id.rb_call_in) callType = 1;
                else if (id == R.id.rb_call_out) callType = 2;
                else if (id == R.id.rb_call_miss) callType = 3;
            }
        });

        // Contacts checkbox
        cbContacts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                contactsArea.setVisibility(checked ? View.VISIBLE : View.GONE);
                if (!checked) {
                    selectedContacts.clear();
                    tvSelectedContacts.setText("未选择");
                }
            }
        });

        // Select contacts button
        btnSelectContacts.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                            new String[]{ Manifest.permission.READ_CONTACTS },
                            REQUEST_CONTACTS_PERMISSION
                        );
                        return;
                    }
                }
                showContactPicker();
            }
        });

        // Date pickers
        btnStartDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int y, int m, int d) {
                        startCal.set(Calendar.YEAR, y);
                        startCal.set(Calendar.MONTH, m);
                        startCal.set(Calendar.DAY_OF_MONTH, d);
                        updateDateButtons();
                    }
                }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH),
                   startCal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btnEndDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int y, int m, int d) {
                        endCal.set(Calendar.YEAR, y);
                        endCal.set(Calendar.MONTH, m);
                        endCal.set(Calendar.DAY_OF_MONTH, d);
                        updateDateButtons();
                    }
                }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH),
                   endCal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Time pickers
        btnStartTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int h, int m) {
                        startCal.set(Calendar.HOUR_OF_DAY, h);
                        startCal.set(Calendar.MINUTE, m);
                        updateDateButtons();
                    }
                }, startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE), true).show();
            }
        });

        btnEndTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int h, int m) {
                        endCal.set(Calendar.HOUR_OF_DAY, h);
                        endCal.set(Calendar.MINUTE, m);
                        updateDateButtons();
                    }
                }, endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE), true).show();
            }
        });

        // Generate
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkPermissionsAndGenerate();
            }
        });
    }

    private void showContactPicker() {
        final List<String[]> allContacts = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    if (number != null && number.length() > 0) {
                        allContacts.add(new String[]{ name, number });
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "读取通讯录失败", Toast.LENGTH_SHORT).show();
            return;
        } finally {
            if (cursor != null) cursor.close();
        }

        if (allContacts.isEmpty()) {
            Toast.makeText(this, "通讯录为空", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] items = new String[allContacts.size()];
        final boolean[] checked = new boolean[allContacts.size()];
        for (int i = 0; i < allContacts.size(); i++) {
            String[] c = allContacts.get(i);
            items[i] = c[0] + "  " + c[1];
        }

        new AlertDialog.Builder(this)
            .setTitle("选择联系人（可多选）")
            .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checked[which] = isChecked;
                }
            })
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    selectedContacts.clear();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            selectedContacts.add(allContacts.get(i));
                        }
                    }
                    if (selectedContacts.isEmpty()) {
                        tvSelectedContacts.setText("未选择");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (String[] c : selectedContacts) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(c[0]);
                        }
                        tvSelectedContacts.setText("已选 " + selectedContacts.size() + " 人: " + sb.toString());
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void updateDateButtons() {
        btnStartDate.setText(dateFmt.format(startCal.getTime()));
        btnEndDate.setText(dateFmt.format(endCal.getTime()));
        btnStartTime.setText(timeFmt.format(startCal.getTime()));
        btnEndTime.setText(timeFmt.format(endCal.getTime()));
    }

    private void checkPermissionsAndGenerate() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{ Manifest.permission.WRITE_CALL_LOG,
                                  Manifest.permission.READ_CALL_LOG,
                                  Manifest.permission.READ_PHONE_STATE },
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
            boolean ok = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) { ok = false; break; }
            }
            if (ok) doGenerate();
            else {
                resultContainer.setVisibility(View.VISIBLE);
                tvResult.setText("需要通话记录权限。\n请到 设置→应用→通话记录生成器→权限 中开启。");
            }
        } else if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showContactPicker();
            }
        }
    }

    private void doGenerate() {
        int count;
        try { count = Integer.parseInt(etCount.getText().toString().trim()); }
        catch (Exception e) { count = 10; }
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
        boolean useContacts = cbContacts.isChecked() && !selectedContacts.isEmpty();

        long startMs = startCal.getTimeInMillis();
        long endMs = endCal.getTimeInMillis();
        long range = endMs - startMs;
        if (range <= 0) range = 1000;

        int inserted = 0;
        try {
            for (int i = 0; i < count; i++) {
                ContentValues values = new ContentValues();

                String phone;
                if (specificPhone != null) {
                    phone = specificPhone;
                } else if (useContacts) {
                    String[] c = selectedContacts.get(random.nextInt(selectedContacts.size()));
                    phone = c[1];
                } else {
                    phone = randomPhone(includeLandline);
                }
                values.put(CallLog.Calls.NUMBER, phone);

                int type = getCallType();
                values.put(CallLog.Calls.TYPE, Integer.valueOf(type));

                int duration = (type == CallLog.Calls.MISSED_TYPE) ? 0
                    : minDur + random.nextInt(maxDur - minDur + 1);
                values.put(CallLog.Calls.DURATION, Long.valueOf(duration));

                long callTime = startMs + (long)(random.nextDouble() * range);
                values.put(CallLog.Calls.DATE, Long.valueOf(callTime));

                values.put(CallLog.Calls.NEW, Integer.valueOf(type == CallLog.Calls.MISSED_TYPE ? 1 : 0));

                try {
                    String loc = location != null ? location : randomLocation();
                    values.put(CallLog.Calls.GEOCODED_LOCATION, loc);
                } catch (Exception ignored) {}

                getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
                inserted++;
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("写入失败")
                .setMessage("错误: " + e.getMessage() + "\n请确保已授予通话记录权限。")
                .setPositiveButton("确定", null)
                .show();
            return;
        }

        // Success dialog
        new AlertDialog.Builder(this)
            .setTitle("生成成功 ✅")
            .setMessage("成功写入 " + inserted + " 条通话记录！\n请打开手机拨号查看。")
            .setPositiveButton("确定", null)
            .show();
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
