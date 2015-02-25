package com.baidu.audio.utility;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by lisheng on 14/12/8.
 */
public class CheckPerformanceThread extends Thread {

    private static final String TAG = CheckPerformanceThread.class.getSimpleName();

    private enum WritterType {
        MEM, CPU
    }

    public static final String STAT_OUTPUT_DIR = "/sdk_stat_output";
    public static final String MEM_OUTPUT_NAME = "/mem_output.txt";
    public static final String CPU_OUTPUT_NAME = "/cpu_output.txt";

    private static final int MEM_INDEX = 6;
    private static final int CPU_INDEX = 2;

    private String mProcessname;

    private volatile boolean isWorking;

    private BufferedWriter mMemWritter;
    private BufferedWriter mCpuWritter;

    private byte[] bufferWriiterLock = new byte[0];

    public CheckPerformanceThread(String processname) {
        mProcessname = processname;
        mMemWritter = getWritter(WritterType.MEM);
        mCpuWritter = getWritter(WritterType.CPU);

    }

    private BufferedWriter getWritter(WritterType writterType) {
        BufferedWriter writter = null;
        String outputFileName = null;
        switch (writterType) {
            case MEM:
                outputFileName = MEM_OUTPUT_NAME;
                break;
            case CPU:
                outputFileName = CPU_OUTPUT_NAME;
                break;
            default:
                break;
        }
        File outputDir = new File(Environment.getExternalStorageDirectory() + STAT_OUTPUT_DIR);
        if (outputDir.isDirectory() || outputDir.mkdirs()) {
            try {
                File outputFile = new File(outputDir.getAbsolutePath() + outputFileName);
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }
                writter = new BufferedWriter(new FileWriter(outputFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return writter;
    }

    @Override
    public void run() {
        isWorking = true;
        try {
            while (isWorking) {
                try {
                    Process process = Runtime.getRuntime().exec("top -n 1 -d 1");
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream())
                    );
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(mProcessname)) {
                            Log.e(TAG, "[current performance line is] " + line);
                            String[] performanceList = line.trim().split("\\s+");
                            String memUsage = performanceList[MEM_INDEX];
                            String cpuUsage = performanceList[CPU_INDEX];
                            writeStatsIntoFile(Double.valueOf(memUsage.trim().replace("K", "")),
                                    mMemWritter);
                            writeStatsIntoFile(Double.valueOf(cpuUsage.trim().replace("%", "")),
                                    mCpuWritter);
                            Log.d(TAG, "[current meme & cpu is] " + memUsage + "&" + cpuUsage);
                            break;
                        }
                    }
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeStatsIntoFile(Double aDouble, BufferedWriter mWritter)
            throws IOException {
        synchronized (bufferWriiterLock) {
            if (mWritter != null) {
                mWritter.write(aDouble + "\n");
                mWritter.flush();
            }
        }
    }

    public void stopWorking() {
        stopBufferedWritter();
        isWorking = false;
    }

    private void stopBufferedWritter() {
        try {
            synchronized (bufferWriiterLock) {
                mMemWritter.flush();
                mMemWritter.close();
                mMemWritter = null;

                mCpuWritter.flush();
                mCpuWritter.close();
                mCpuWritter = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}