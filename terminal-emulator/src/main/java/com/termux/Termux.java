package com.termux;


import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.termux.app.BackgroundJob;
import com.termux.app.TermuxInstaller;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TermuxDebug;

import java.io.File;

/**
 * @author liujiadong
 * @since 2019/9/6
 */
public class Termux {

    @SuppressLint("SdCardPath")
    public static final String FILES_PATH = "/data/data/com.vid007.videobuddy/files";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";

    private TerminalSession mSession;


    private Termux() {
    }

    private static class LazyHolder {
        private static final Termux sInstance = new Termux();
    }

    public static Termux getInstance() {
        return LazyHolder.sInstance;
    }

    public void execute(Context context, String cmd, TermuxListener listener) {
        if (listener == null) {
            Log.e(TermuxDebug.LOG_TAG, "the listener cannot be null");
            return;
        }
        synchronized (this) {
            if (mSession == null) {
                if (context == null) {
                    listener.init(false);
                    return;
                }
                TermuxInstaller.setupIfNeeded(context, listener, () -> {
                    try {
                        mSession = createSession(listener);
                    } catch (Exception e) {
                        listener.init(false);
                    }
                    if (mSession != null) {
                        if (!TextUtils.isEmpty(cmd)) {
                            mSession.write(cmd);
                        }
                    } else {
                        listener.init(false);
                    }
                });
            } else {
                mSession.setmListener(listener);
                mSession.write(cmd);
            }
        }
    }

    private TerminalSession createSession(TermuxListener listener) {
        new File(HOME_PATH).mkdirs();

        String[] env = BackgroundJob.buildEnvironment(false, HOME_PATH);
        String executablePath = null;
        for (String shellBinary : new String[]{"login", "bash", "zsh"}) {
            File shellFile = new File(PREFIX_PATH + "/bin/" + shellBinary);
            if (shellFile.canExecute()) {
                executablePath = shellFile.getAbsolutePath();
                break;
            }
        }

        if (executablePath == null) {
            // Fall back to system shell as last resort:
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

        TerminalSession session = new TerminalSession(executablePath, HOME_PATH, args, env, listener);
        session.initializeEmulator(Integer.MAX_VALUE, 120);

        return session;
    }
}
