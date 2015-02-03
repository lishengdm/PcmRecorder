package com.baidu.audio.pcmrecoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.LogRecord;

/**
 * Created by lisheng on 15/1/22.
 */
public class PcmRecoder {

    static class HandlerMsg {
        public static final int MSG_ERROR_INIT_OUTPUT_STREAM_FAIL = 0;
        public static final int MSG_START_RECORDING = 1;
        public static final int MSG_ERROR_AUDIO_RECORD_NOT_INITIALIZED = 2;
    }

    interface OnPcmRecorderListener {
        /**
         * invoked when error happens during recording
         */
        public void onRecordError();

        /**
         * invoked when the recording finish
         */
        public void onRecordFinish();
    }

    public static final int SAMPLE_RATE_8K = 8000;
    public static final int SAMPLE_RATE_16K = 16000;

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mAudioSampleRate = SAMPLE_RATE_8K;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mAudioChannel = AudioFormat.CHANNEL_IN_MONO;
    private int minimumBufferSize;

    private String mAudioOutputFileName = "sample.pcm";
    private String mAudioOutputFileDirPath = Environment.getExternalStorageDirectory()
            + "PcmRecorder";

    private volatile boolean mIsRecorkding = false;

    private AudioRecord mAudioRecord = null;
    private Context mContext = null;

    private byte[] mAudioRecordLock = new byte[0];
    private Handler mUIHandler;

    private Preference.OnPreferenceChangeListener mListener;

    public PcmRecoder(Context ctx, Preference.OnPreferenceChangeListener listener) {
        if (ctx == null) {
            this.mContext = ctx;
        }

        if (listener == null) {
            this.mListener = listener;
        }

        // all the call-back method should be invoked in the main looper
        initMainHandler();
    }

    private void initMainHandler() {
        mUIHandler = new Handler(mContext.getMainLooper()) {
            /**
             * Subclasses must implement this to receive messages.
             *
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /**
     * initiate the recorder
     */
    public void initiateRecorder() {
        minimumBufferSize = AudioRecord.getMinBufferSize(
                mAudioSampleRate, mAudioChannel, mAudioFormat
        );
        mAudioRecord = new AudioRecord(
                mAudioSource, mAudioSampleRate, mAudioChannel, mAudioFormat, minimumBufferSize
        );
    }

    /**
     * start recorder, this method must be invoked after {@link #initiateRecorder()}
     */
    public void startRecording() {
        if (mAudioRecord == null) {
            mUIHandler.obtainMessage(HandlerMsg.MSG_ERROR_AUDIO_RECORD_NOT_INITIALIZED)
                    .sendToTarget();
        }

        mUIHandler.obtainMessage(HandlerMsg.MSG_START_RECORDING).sendToTarget();
    }

    public void setAudioChannel(int mAudioChannel) {
        this.mAudioChannel = mAudioChannel;
    }

    public void setAudioSource(int mAudioSource) {
        this.mAudioSource = mAudioSource;
    }

    public void setAudioSampleRate(int mAudioSampleRate) {
        this.mAudioSampleRate = mAudioSampleRate;
    }

    public void setAudioFormat(int mAudioFormat) {
        this.mAudioFormat = mAudioFormat;
    }

    public void setAudioOutputFileName(String mAudioOutputFileName) {
        this.mAudioOutputFileName = mAudioOutputFileName;
    }

    private class RecorderThread extends Thread {

        /**
         * Calls the <code>run()</code> method of the Runnable object the receiver
         * holds. If no Runnable is set, does nothing.
         *
         * @see Thread#start
         */
        @Override
        public void run() {
            if (initOutputFile()) {
                short sData[] = new short[minimumBufferSize / 2];

                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(mAudioOutputFileDirPath + mAudioOutputFileName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                while (mIsRecorkding) {
                    synchronized (mAudioRecordLock) {
                        if (mAudioRecord != null) {
                            mAudioRecord.read(sData, 0, minimumBufferSize);
                            try {
                                byte bData[] = short2byte(sData);
                                os.write(bData, 0, minimumBufferSize);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                mUIHandler.obtainMessage(HandlerMsg.MSG_ERROR_INIT_OUTPUT_STREAM_FAIL)
                        .sendToTarget();
            }
        }
    }

    /**
     * initiate the output file according to the {@link #mAudioOutputFileName}
     *
     * @return if the output file has been created successfully
     */
    private boolean initOutputFile() {
        File outputFileDir = new File(mAudioOutputFileDirPath);

        if (outputFileDir.isDirectory() || outputFileDir.mkdirs()) {
            File outputFile = new File(mAudioOutputFileDirPath + mAudioOutputFileName);
            if (!outputFile.exists()) {
                try {
                    if (outputFile.createNewFile()) {
                        return true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public void stopRecording() {
        mIsRecorkding = false;
        synchronized (mAudioRecordLock) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private byte[] short2byte(short[] shortArray) {
        final int length = shortArray.length;
        ByteBuffer buffer = ByteBuffer.allocate(shortArray.length * 2);
        buffer.clear();
        buffer.order(ByteOrder.nativeOrder());
        for(int i = 0; i < length; i++) {
            buffer.putShort(i  * 2, shortArray[i]);
        }
        return buffer.array();
    }
}
