package com.baidu.audio.pcmrecoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by lisheng on 15/1/22.
 */
public class PcmRecoder {

    private static final String LOG_TAG = PcmRecoder.class.getSimpleName();


    public static class WorkStatus {
        public static final int START_INIT_OUTPUT_FILE_FAIL = 0;
        public static final int START_RECORDER_NOT_INITIALIZED = 2;
        public static final int START_RECORDER_SUCCESS = 3;
        public static final int CALBACK_CAN_NOT_WRITE_OUTPUT_STREAM = 1;
    }

    public interface OnPcmRecorderListener {
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
            + File.separator + "PcmRecorder";

    private volatile boolean mIsRecorkding = false;

    private AudioRecord mAudioRecord = null;
    private Context mContext = null;

    private final byte[] mAudioRecordLock = new byte[0];
    private Handler mUIHandler = null;

    private OnPcmRecorderListener mListener;

    private static PcmRecoder mPcmRecorderInstance = null;

    public static PcmRecoder getInstance(Context context, OnPcmRecorderListener listener) {
        if (mPcmRecorderInstance == null) {
            synchronized (PcmRecoder.class) {
                if (mPcmRecorderInstance == null) {
                    mPcmRecorderInstance = new PcmRecoder(context, listener);
                }
            }
        }
        return mPcmRecorderInstance;
    }

    private PcmRecoder(Context ctx, OnPcmRecorderListener listener) {
        this.mContext = ctx;
        this.mListener = listener;
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
                mAudioSource, mAudioSampleRate, mAudioChannel, mAudioFormat, minimumBufferSize * 2
        );
    }

    /**
     * start recorder, this method must be invoked after {@link #initiateRecorder()}
     */
    public int startRecording() {
        if (mAudioRecord == null) {
            return WorkStatus.START_RECORDER_NOT_INITIALIZED;
        }

        if (!initOutputFile(mAudioOutputFileDirPath, mAudioOutputFileName)) {
            return WorkStatus.START_INIT_OUTPUT_FILE_FAIL;
        }

        new RecorderThread().start();
        return WorkStatus.START_RECORDER_SUCCESS;
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
            // prepare for start record
            mIsRecorkding = true;
            short sData[] = new short[minimumBufferSize];
            BufferedOutputStream bufferedOutputStream = null;
            // start record
            mAudioRecord.startRecording();
            try {
                // initiate output stream
                bufferedOutputStream = new BufferedOutputStream(
                        new FileOutputStream(mAudioOutputFileDirPath + File.separator
                                + mAudioOutputFileName)
                );
                while (mIsRecorkding) {
                    synchronized (mAudioRecordLock) {
                        if (mAudioRecord != null) {
                            int readLen = mAudioRecord.read(sData, 0, minimumBufferSize);
                            Log.d(LOG_TAG, "read len [" + readLen + "] shorts");
                            byte bData[] = short2byte(sData);
                            bufferedOutputStream.write(bData, 0, minimumBufferSize * 2);
                            bufferedOutputStream.flush();
                        }
                    }
                }
            } catch (IOException e) {
                // can't write to the output file
                e.printStackTrace();
                mUIHandler.obtainMessage(WorkStatus.CALBACK_CAN_NOT_WRITE_OUTPUT_STREAM)
                        .sendToTarget();
            } finally {
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException e) {
                        // can't close the output stream
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * initiate the output file according to the {@link #mAudioOutputFileName}
     *
     * @return if the output file has been created successfully
     */
    private boolean initOutputFile(String dirPath, String fileName) {
        File outputFileDir = new File(dirPath);
        // if the output dir isn't exist and make dir fail, return false
        if (!outputFileDir.isDirectory() && !outputFileDir.mkdirs()) {
            return false;
        }
        File outputFile = new File(dirPath + File.separator + fileName);
        // if the file does't exist, create it
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                // can't create the output file
                return false;
            }
        }
        return true;
    }

    public void stopRecording() {
        if (mIsRecorkding) {
            synchronized (mAudioRecordLock) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
            mIsRecorkding = false;
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

    public static void releaseInstance() {
        if (mPcmRecorderInstance != null) {
            synchronized (PcmRecoder.class) {
                if (mPcmRecorderInstance != null) {
                    mPcmRecorderInstance = null;
                }
            }
        }
    }
}
