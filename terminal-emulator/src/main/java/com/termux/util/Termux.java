package com.termux.util;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.FileDescriptor;
import java.lang.reflect.Field;

public final class Termux {

    @SuppressLint("SdCardPath")
    public static final String FILES_PATH = "/data/data/com.vid007.videobuddy/files";
    @SuppressLint("SdCardPath")
    public static final String TMP_FILE = "/data/data/com.vid007.videobuddy/files/home/tmp.txt";
    @SuppressLint("SdCardPath")
    public static final String TMP_FILE1 = "/data/data/com.vid007.videobuddy/files/home/tmp1.txt";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";
    public static final String STAGING_PREFIX_PATH = Termux.FILES_PATH + "/usr-staging";
    public static final String TAG = "termux";

    public static FileDescriptor wrapFileDescriptor(int fileDescriptor) {
        FileDescriptor result = new FileDescriptor();
        try {
            Field descriptorField;
            try {
                descriptorField = FileDescriptor.class.getDeclaredField("descriptor");
            } catch (NoSuchFieldException e) {
                // For desktop java:
                descriptorField = FileDescriptor.class.getDeclaredField("fd");
            }
            descriptorField.setAccessible(true);
            descriptorField.set(result, fileDescriptor);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            Log.wtf(Termux.TAG, "Error accessing FileDescriptor#descriptor private field", e);
            System.exit(1);
        }
        return result;
    }
}
