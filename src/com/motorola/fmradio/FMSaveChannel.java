package com.motorola.fmradio;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FMSaveChannel extends Activity implements View.OnClickListener {
    private static final String TAG = "FMSaveChannel";

    public static final String EXTRA_PRESET_ID = "preset";
    public static final String EXTRA_FREQUENCY = "frequency";
    public static final String EXTRA_RDS_NAME = "rds_name";

    private static final int SAVE_ID = 1;
    private static final int DISCARD_ID = 2;

    private EditText mNameField;
    private TextView mFrequencyField;
    private Button mDiscardButton;
    private Button mSaveButton;
    private Spinner mPresetSpinner;

    private float mFrequency;
    private String mRdsInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.save_ch);
        setTitle(R.string.save_as_preset);

        mSaveButton = (Button) findViewById(R.id.btn_save);
        mSaveButton.setOnClickListener(this);
        mDiscardButton = (Button) findViewById(R.id.btn_discard);
        mDiscardButton.setOnClickListener(this);

        mFrequencyField = (TextView) findViewById(R.id.save_frequency);
        mPresetSpinner = (Spinner) findViewById(R.id.ch_spinner);
        mNameField = (EditText) findViewById(R.id.ch_name_edit);

        initNameFilter();
        initData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, SAVE_ID, Menu.FIRST, R.string.btn_save).setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, DISCARD_ID, Menu.FIRST + 1, R.string.btn_discard).setIcon(R.drawable.ic_menu_discard);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SAVE_ID:
                doSave();
                break;
            case DISCARD_ID:
                doDiscard();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View button) {
        switch (button.getId()) {
            case R.id.btn_save:
                doSave();
                break;
            case R.id.btn_discard:
                doDiscard();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            return super.onKeyDown(keyCode, event);
        }

        int act = event.getAction();
        noticeFMRadioMainUpdateVol("com.motorola.fmradio.setvolume", act, keyCode);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            return super.onKeyUp(keyCode, event);
        }

        int act = event.getAction();
        noticeFMRadioMainUpdateVol("com.motorola.fmradio.setvolume", act, keyCode);
        return true;
    }

    private void initNameFilter() {
        InputFilter[] oldFilter = mNameField.getFilters();
        int oldLen = oldFilter.length;
        InputFilter[] newFilter = new InputFilter[oldLen + 1];

        System.arraycopy(newFilter, 0, oldFilter, 0, oldLen);
        newFilter[oldLen] = new LengthFilter(40);
        mNameField.setFilters(newFilter);
    }

    private void initData() {
        Intent intent = getIntent();
        int frequency = intent.getIntExtra(EXTRA_FREQUENCY, FMUtil.MIN_FREQUENCY);
        int preset = intent.getIntExtra(EXTRA_PRESET_ID, 0);
        mRdsInfo = intent.getStringExtra(EXTRA_RDS_NAME);

        mFrequency = (float) frequency / 1000.0F;
        final String freqString = FMUtil.formatFrequency(this, mFrequency);
        mFrequencyField.setText(freqString);

        if (!TextUtils.isEmpty(mRdsInfo)) {
            mNameField.setText(mRdsInfo);
        } else {
            mNameField.setText(freqString);
        }

        Cursor cursor = getContentResolver().query(FMUtil.CONTENT_URI, FMUtil.PROJECTION, null, null, null);
        if (cursor != null) {
            ArrayList<String> results = new ArrayList<String>();
            int i = 1;

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                results.add(FMUtil.getPresetUiString(this, cursor, i));
                cursor.moveToNext();
                i++;
            }
            cursor.close();

            ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, results);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            mPresetSpinner.offsetLeftAndRight(3);
            mPresetSpinner.setAdapter(adapter);
            mPresetSpinner.setSelection(preset);
        }
    }

    private void noticeFMRadioMainUpdateVol(String cmd, int action, int keyCode) {
        Intent intent = new Intent(cmd);
        intent.putExtra("event_action", action);
        intent.putExtra("event_keycode", keyCode);
        sendBroadcast(intent);
    }

    private void doSave() {
        ContentValues cv = new ContentValues();
        int id = mPresetSpinner.getSelectedItemPosition();

        cv.put("CH_Freq", mFrequency);
        cv.put("CH_Name", mNameField.getText().toString());
        cv.put("CH_RdsName", TextUtils.isEmpty(mRdsInfo) ? "" : mRdsInfo);

        getContentResolver().update(FMUtil.CONTENT_URI, cv, "ID=" + id, null);

        Intent result = new Intent();
        result.putExtra(EXTRA_PRESET_ID, id);
        setResult(RESULT_OK, result);
        finish();
    }

    private void doDiscard() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
