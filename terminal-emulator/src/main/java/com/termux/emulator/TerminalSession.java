package com.termux.emulator;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;

import com.termux.app.BackgroundJob;
import com.termux.util.Termux;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;


public final class TerminalSession {

    private static final String CMD_NO_OUTPUT = ">/dev/null 2>&1;";
    private static final String SUCCESS_CODE = "0";

    private volatile ArrayBlockingQueue<Command> mCmdQueue = new ArrayBlockingQueue<>(8);
    private final ByteQueue mTerminalToProcessIOQueue = new ByteQueue(4096);
    private volatile Command mCurrentCommand = null;

    private int mShellPid;
    private int mTerminalFileDescriptor;

    private final String mShellPath;
    private final String mCwd;
    private final String[] mArgs;
    private final String[] mEnv;


    public static TerminalSession createSession() {
        new File(Termux.HOME_PATH).mkdirs();

        String[] env = BackgroundJob.buildEnvironment(false, Termux.HOME_PATH);
        String executablePath = null;
        for (String shellBinary : new String[]{"login", "bash", "zsh"}) {
            File shellFile = new File(Termux.PREFIX_PATH + "/bin/" + shellBinary);
            if (shellFile.canExecute()) {
                executablePath = shellFile.getAbsolutePath();
                break;
            }
        }

        if (executablePath == null) {
            // Fall back to system shell as last resort:
            Log.d(Termux.TAG, "fall back to system shell");
            executablePath = "/system/bin/sh";
        }

        String[] processArgs = BackgroundJob.setupProcessArgs(executablePath, null);
        executablePath = processArgs[0];
        int lastSlashIndex = executablePath.lastIndexOf('/');
        String processName = "-" +
                (lastSlashIndex == -1 ? executablePath : executablePath.substring(lastSlashIndex + 1));

        String[] args = new String[processArgs.length];
        args[0] = processName;
        if (processArgs.length > 1)
            System.arraycopy(processArgs, 1, args, 1, processArgs.length - 1);

        TerminalSession session = new TerminalSession(executablePath, Termux.HOME_PATH, args, env);
        session.initializeEmulator();
        session.init();
        return session;
    }

    private TerminalSession(String shellPath, String cwd, String[] args, String[] env) {
        this.mShellPath = shellPath;
        this.mCwd = cwd;
        this.mArgs = args;
        this.mEnv = env;
    }

    private void init() {
        new Thread(() -> {
            while (true) {
                try {
                    mCurrentCommand = mCmdQueue.take();
                    writeCurrentCmd();
                    synchronized (TerminalSession.this) {
                        wait();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initializeEmulator() {

        int[] processId = new int[1];
        mTerminalFileDescriptor = JNI.createSubprocess(mShellPath, mCwd, mArgs, mEnv, processId, 120, Integer.MAX_VALUE);
        mShellPid = processId[0];

//        Log.d(Termux.TAG, "mShellPath: " + mShellPath);
//        Log.d(Termux.TAG, "mCwd: " + mCwd);
//        Log.d(Termux.TAG, "mArgs: " + Arrays.toString(mArgs));
//        Log.d(Termux.TAG, "mEnv: " + Arrays.toString(mEnv));
        Log.d(Termux.TAG, "mShellPid: " + mShellPid);
//        Log.d(Termux.TAG, "mTerminalFileDescriptor: " + mTerminalFileDescriptor);

        final FileDescriptor terminalFileDescriptorWrapped = Termux.wrapFileDescriptor(mTerminalFileDescriptor);

        new Thread("TermSessionInputReader[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                try (InputStream termIn = new FileInputStream(terminalFileDescriptorWrapped)) {
                    final byte[] buffer = new byte[4096];
                    while (true) {
                        int read = termIn.read(buffer);
                        if (read == -1) return;

                        String result = new String(buffer, 0, read);

//                        Log.d(Termux.TAG, "result = " + result);

                        if (!TextUtils.isEmpty(result) && mCurrentCommand != null) {
                            String currentCmd = mCurrentCommand.getCmd();
                            OnCommandListener listener = mCurrentCommand.getListener();
                            if (result.trim().startsWith(currentCmd)) {
                                if (listener != null) {
                                    listener.execute(currentCmd, result.trim().replace(currentCmd, "").trim().startsWith(SUCCESS_CODE));
                                }
                                mCurrentCommand = null;
                                synchronized (TerminalSession.this) {
                                    TerminalSession.this.notify();
                                }
                            }
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

                        String currentCmd = new String(buffer, 0, bytesToWrite);
                        if (TextUtils.isEmpty(currentCmd.trim())) {
                            continue;
                        }
                        byte[] cmd = wrapCmd(currentCmd);

                        Log.d(Termux.TAG, mShellPid + " currentCmd = " + currentCmd);

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
                Log.d(Termux.TAG, "exitCode = " + processExitCode);
            }
        }.start();
    }

    private byte[] wrapCmd(String cmd) {
        String wrapCmd = "{ " + cmd + "; }" + CMD_NO_OUTPUT + "if [ $? -ne 0 ]; then echo \"" + cmd + " 1\"; else echo \"" + cmd + " 0\";fi;\n";
        return wrapCmd.getBytes(StandardCharsets.UTF_8);
    }

    private void writeCurrentCmd() {
        if (mCurrentCommand != null && !TextUtils.isEmpty(mCurrentCommand.getCmd())) {
            String s = mCurrentCommand.getCmd();
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            if (mShellPid > 0) mTerminalToProcessIOQueue.write(bytes, 0, bytes.length);
        }
    }

    private synchronized boolean isRunning() {
        return mShellPid != -1;
    }

    private void cleanupResources() {
        synchronized (this) {
            mShellPid = -1;
        }
        mTerminalToProcessIOQueue.close();
        JNI.close(mTerminalFileDescriptor);
    }


    public synchronized boolean isFree() {
        return mCmdQueue.size() == 0 && mCurrentCommand == null;
    }

    /**
     * 传入listener为空时使用返回值，否则使用回调
     *
     * @param context  cannot be null
     * @param cmd      cannot be null
     * @param listener
     * @return
     */
    public boolean execute(Context context, String cmd, OnCommandListener listener) {
        if (context == null || TextUtils.isEmpty(cmd)) {
            return false;
        }

        Command command = new Command(cmd);
        command.setListener(listener);
        if (!mCmdQueue.offer(command)) {
            if (listener != null) {
                listener.execute(cmd, false);
            }
            return false;
        }
        return true;
    }

    public void finishIfRunning() {
        if (isRunning()) {
            try {
                Os.kill(mShellPid, OsConstants.SIGKILL);
            } catch (ErrnoException e) {
                Log.w("termux", "Failed sending SIGKILL: " + e.getMessage());
            }
        }
    }
}
