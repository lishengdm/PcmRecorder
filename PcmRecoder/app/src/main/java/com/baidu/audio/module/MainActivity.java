package com.baidu.audio.module;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.baidu.audio.pcmrecoder.PcmRecoder;
import com.baidu.audio.pcmrecoder.R;


public class MainActivity extends Activity implements View.OnClickListener{

    // ui refs
    private Button btnStartRecord = null;
    private Button btnStopRecord = null;

    private PcmRecoder mPcmRecoder = null;
    private MyPcmRecorderListener mRecorderListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();

        mRecorderListener = new MyPcmRecorderListener();
        mPcmRecoder = PcmRecoder.getInstance(this, mRecorderListener);
        mPcmRecoder.initiateRecorder();
    }

    private void initUI() {
        btnStartRecord = (Button) findViewById(R.id.id_start_record_btn);
        btnStopRecord = (Button) findViewById(R.id.id_stop_record_btn);

        btnStartRecord.setOnClickListener(this);
        btnStopRecord.setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_start_record_btn:
                mPcmRecoder.startRecording();
                break;
            case R.id.id_stop_record_btn:
                mPcmRecoder.stopRecording();
                break;
            default:
                break;
        }
    }

    class MyPcmRecorderListener implements PcmRecoder.OnPcmRecorderListener {

        /**
         * invoked when error happens during recording
         */
        @Override
        public void onRecordError() {

        }

        /**
         * invoked when the recording finish
         */
        @Override
        public void onRecordFinish() {

        }
    }
}
