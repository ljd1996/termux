package com.termux.terminal;

import android.text.TextUtils;
import android.util.Log;

import com.termux.TermuxListener;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public final class TerminalSession {

    private static final String CMD_NO_OUTPUT = ">/dev/null 2>&1;";
    private static final String SUCCESS_CODE = "0";
    public static final String FAIL_CODE = "1";

    private final ByteQueue mTerminalToProcessIOQueue = new ByteQueue(4096);

    private int mShellPid;

    private int mTerminalFileDescriptor;

    private final String mShellPath;
    private final String mCwd;
    private final String[] mArgs;
    private final String[] mEnv;

    private volatile String mCurrentCmd = "";
    private TermuxListener mListener;

    public TerminalSession(String shellPath, String cwd, String[] args, String[] env, TermuxListener listener) {
        this.mShellPath = shellPath;
        this.mCwd = cwd;
        this.mArgs = args;
        this.mEnv = env;
        this.mListener = listener;
    }

    public void setmListener(TermuxListener listener) {
        this.mListener = listener;
    }

    public void initializeEmulator(int columns, int rows) {

        int[] processId = new int[1];
        mTerminalFileDescriptor = JNI.createSubprocess(mShellPath, mCwd, mArgs, mEnv, processId, rows, columns);
        mShellPid = processId[0];

        Log.d(TermuxDebug.LOG_TAG, "mShellPath: " + mShellPath);
        Log.d(TermuxDebug.LOG_TAG, "mCwd: " + mCwd);
        Log.d(TermuxDebug.LOG_TAG, "mArgs: " + Arrays.toString(mArgs));
        Log.d(TermuxDebug.LOG_TAG, "mEnv: " + Arrays.toString(mEnv));
        Log.d(TermuxDebug.LOG_TAG, "mShellPid: " + mShellPid);
        Log.d(TermuxDebug.LOG_TAG, "mTerminalFileDescriptor: " + mTerminalFileDescriptor);

        final FileDescriptor terminalFileDescriptorWrapped = wrapFileDescriptor(mTerminalFileDescriptor);

        new Thread("TermSessionInputReader[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                try (InputStream termIn = new FileInputStream(terminalFileDescriptorWrapped)) {
                    final byte[] buffer = new byte[4096];
                    while (true) {
                        int read = termIn.read(buffer);
                        if (read == -1) return;

                        String result = new String(buffer, 0, read);

                        Log.d(TermuxDebug.LOG_TAG, "result = " + result);

                        if (!TextUtils.isEmpty(result) && result.trim().startsWith(mCurrentCmd) && mListener != null) {
                            mListener.execute(mCurrentCmd, result.trim().replace(mCurrentCmd, "").trim().startsWith(SUCCESS_CODE));
                        }
                    }
                } catch (Exception e) {
                    // Ignore, just shutting down.
                }
            }
        }.start();

        new Thread("TermSessionOutputWriter[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                final byte[] buffer = new byte[4096];
                try (FileOutputStream termOut = new FileOutputStream(terminalFileDescriptorWrapped)) {
                    while (true) {
                        int bytesToWrite = mTerminalToProcessIOQueue.read(buffer, true);
                        if (bytesToWrite == -1) return;

                        mCurrentCmd = new String(buffer, 0, bytesToWrite);
                        Log.d(TermuxDebug.LOG_TAG, "mCurrentCmd = " + mCurrentCmd);

                        byte[] cmd = wrapCmd(buffer, bytesToWrite);
                        termOut.write(cmd, 0, cmd.length);
                    }
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }.start();

        new Thread("TermSessionWaiter[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                int processExitCode = JNI.waitFor(mShellPid);
                cleanupResources();
                Log.d(TermuxDebug.LOG_TAG, "exitCode = " + processExitCode);
            }
        }.start();
    }


    private String wrapCmd(String cmd) {
        return "{ " + cmd + "; }" + CMD_NO_OUTPUT + wrapResult(cmd);
    }

    private String wrapResult(String cmd) {
        return "if [ $? -ne 0 ]; then echo \"" + cmd + " 1\"; else echo \"" + cmd + " 0\";fi;\n";
    }

    private byte[] wrapCmd(byte[] cmd, int bytesToWrite) {
        return wrapCmd(new String(cmd, 0, bytesToWrite)).getBytes(StandardCharsets.UTF_8);
    }

    private static FileDescriptor wrapFileDescriptor(int fileDescriptor) {
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
            Log.wtf(TermuxDebug.LOG_TAG, "Error accessing FileDescriptor#descriptor private field", e);
            System.exit(1);
        }
        return result;
    }

    public void write(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (mShellPid > 0) mTerminalToProcessIOQueue.write(bytes, 0, bytes.length);
    }

    private void cleanupResources() {
        synchronized (this) {
            mShellPid = -1;
        }
        mTerminalToProcessIOQueue.close();
        JNI.close(mTerminalFileDescriptor);
    }
}
