package com.base.find_phone_clap_detector.utils;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueuedWorkFix {
    private static final String TAG = "QueuedWorkFix";

    public static void clearQueuedWork() {
        try {
            Class<?> queuedWorkClass = Class.forName("android.app.QueuedWork");
            
            boolean cleared = false;
            // Try to clear sWork (Android 8+)
            try {
                Field sWorkField = queuedWorkClass.getDeclaredField("sWork");
                sWorkField.setAccessible(true);
                Object work = sWorkField.get(null);
                if (work instanceof LinkedList) {
                    synchronized (queuedWorkClass) { // sWork is often synchronized on QueuedWork.class
                        ((LinkedList<?>) work).clear();
                    }
                    cleared = true;
                }
            } catch (NoSuchFieldException e) {
                // Ignore
            }

            // Try to clear sPendingWorkFinishers
            try {
                Field finishersField = queuedWorkClass.getDeclaredField("sPendingWorkFinishers");
                finishersField.setAccessible(true);
                Object finishers = finishersField.get(null);
                if (finishers instanceof ConcurrentLinkedQueue) {
                    ((ConcurrentLinkedQueue<?>) finishers).clear();
                    cleared = true;
                } else if (finishers instanceof LinkedList) {
                    ((LinkedList<?>) finishers).clear();
                    cleared = true;
                }
            } catch (NoSuchFieldException e) {
                // Ignore
            }
            
            if (cleared) {
                Log.d(TAG, "QueuedWork cleared successfully.");
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to clear QueuedWork", e);
        }
    }
}
