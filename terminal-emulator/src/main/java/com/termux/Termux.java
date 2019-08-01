package com.termux;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.annotation.NonNull;

import com.termux.app.BackgroundJob;
import com.termux.app.TermuxInstaller;
import com.termux.terminal.TerminalSession;

import java.io.File;


/**
 * @author liujiadong
 * @since 2019/8/1
 */
public enum Termux {
    mInstance;

    @SuppressLint("SdCardPath")
    public static final String FILES_PATH = "/data/data/com.termux/files";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";

    public static final String CMD_GET_YOUTUBE_DL = "apt update&&apt -y install python&&pip install --upgrade pip&&pip install --upgrade youtube-dl\n";
    public static final String CMD_IS_INSTALLED = "youtube-dl\n";
    public static final String INSTALL_SUCCESS = "Successfully installed youtube-dl";
    public static final String HAS_INSTALL = "Requirement already up-to-date: youtube-dl";


    private Activity mActivity;
    private TermuxHandle mTermuxHandle;

    private TerminalSession mSession;
    private boolean mIsInstalled = false;

    public Termux init(Activity activity, TermuxHandle handle) {
        mActivity = activity;
        mTermuxHandle = handle;

        TermuxInstaller.setupIfNeeded(mActivity, () -> {
            try {
                mSession = createSession();
            } catch (Exception e) {
                if (mTermuxHandle != null) {
                    mTermuxHandle.initFail();
                }
            }
        });
        return this;
    }

    public void setInstalled(boolean mIsInstalled) {
        this.mIsInstalled = mIsInstalled;
    }

    public boolean isInstalled() {
        return mIsInstalled;
    }

    public TerminalSession getSession() {
        return mSession;
    }

    public TermuxHandle getTermuxHandle() {
        return mTermuxHandle;
    }

    public void install() {
        if (mSession == null) {
            TermuxInstaller.setupIfNeeded(mActivity, () -> {
                try {
                    mSession = createSession();
                } catch (Exception e) {
                    if (mTermuxHandle != null) {
                        mTermuxHandle.initFail();
                    }
                }
                mSession.write(CMD_GET_YOUTUBE_DL);
            });
        } else {
            mSession.write(CMD_GET_YOUTUBE_DL);
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

        TerminalSession session = new TerminalSession(executablePath, HOME_PATH, args, env);
        session.initializeEmulator(500, 50);

        return session;
    }


    public interface TermuxHandle {
        void success();

        void initFail();

        void installFail();
    }
}
