package com.example.android.tflitecamerademo.Utils;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by gustavomonteiro on 01/09/17.
 */

public class MeasurePerformanceUtil {

    private ArrayList<Long> timeLogList;

    public MeasurePerformanceUtil() {
        timeLogList = new ArrayList<>();
    }

    public void addStartTime() {
        timeLogList.add(System.currentTimeMillis());
    }

    public void logMeasuredTime(String logMessage) {
        long totalTime = System.currentTimeMillis() - timeLogList.get(timeLogList.size()-1);
        Log.d("MeasureLog", "Measured time: " + totalTime + "ms" + " :: " + logMessage);

        timeLogList.remove(timeLogList.size()-1);
    }
}
