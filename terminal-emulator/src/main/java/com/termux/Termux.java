package com.termux;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.termux.app.BackgroundJob;
import com.termux.app.TermuxInstaller;
import com.termux.terminal.CmdElement;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TermuxDebug;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author liujiadong
 * @since 2019/9/6
 */
public class Termux {

    @SuppressLint("SdCardPath")
    private static final String FILES_PATH = "/data/data/com.vid007.videobuddy/files";
    public static final String TMP_FILE = FILES_PATH + "/home/tmp.txt";
    public static final String TMP_FILE1 = FILES_PATH + "/home/tmp1.txt";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";
    public static final String STAGING_PREFIX_PATH = Termux.FILES_PATH + "/usr-staging";

    private TerminalSession mSession;
    private ArrayBlockingQueue<CmdElement> mCmdQueue;
    private volatile boolean mIsExed = true;

    private Termux() {
        mCmdQueue = new ArrayBlockingQueue<>(8);
        new Thread(() -> {
            CmdElement element;
            while (true) {
                try {
                    element = mCmdQueue.take();
                    String cmd = element.getCmd();
                    TermuxListener listener = element.getListener();
                    if (listener == null || TextUtils.isEmpty(cmd)) {
                        continue;
                    }
                    mIsExed = false;
                    if (mSession == null) {
                        try {
                            mSession = createSession();
                        } catch (Exception e) {
                            listener.execute(null, false);
                            continue;
                        }
                        if (mSession != null) {
                            mSession.setListener((cmd12, isSuccess) -> {
                                listener.execute(cmd12, isSuccess);
                                mIsExed = true;
                            });
                            mSession.write(cmd);
                        } else {
                            listener.execute(null, false);
                            continue;
                        }
                    } else {
                        mSession.setListener((cmd1, isSuccess) -> {
                            listener.execute(cmd1, isSuccess);
                            mIsExed = true;
                        });
                        mSession.write(cmd);
                    }
                    while (!mIsExed) ;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static class LazyHolder {
        private static final Termux sInstance = new Termux();
    }

    public static Termux getInstance() {
        return LazyHolder.sInstance;
    }

    public void execute(Context context, String cmd, TermuxListener listener) {
        // listener不能为null
        if (context == null || listener == null) {
            return;
        }

        // 初始化
        if (TextUtils.isEmpty(cmd)) {
            TermuxInstaller installer = new TermuxInstaller();
            installer.setListener((cmd1, isSuccess) -> listener.execute(null, isSuccess));
            installer.setupIfNeeded(context);
            return;
        }

        if (!mCmdQueue.offer(new CmdElement(cmd, listener))) {
            listener.execute(cmd, false);
        }
    }

    public void clearQueue() {
        if (mCmdQueue != null) {
            mCmdQueue.clear();
        }
    }

    public void closeSession() {
        clearQueue();
        mIsExed = true;
        if (mSession != null) {
            mSession.finishIfRunning();
            mSession = null;
        }
    }

    private TerminalSession createSession() {
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
            Log.d(TermuxDebug.TAG, "fall back to system shell");
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

        TerminalSession session = new TerminalSession(executablePath, HOME_PATH, args, env);
        session.initializeEmulator(Integer.MAX_VALUE, 120);

        return session;
    }
}
