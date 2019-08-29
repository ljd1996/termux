package com.termux;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;

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
    public static final String FILES_PATH = "/data/data/com.vid007.videobuddy/files";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";

    public static final String CMD_INSTALL_YOUTUBE_DL = "apt update> /dev/null 2>&1&&apt -y install python2> /dev/null 2>&1&&pip2 install --upgrade pip> /dev/null 2>&1&&pip install --upgrade youtube-dl > /dev/null 2>&1;if [ $? -ne 0 ]; then echo -1; else echo 0;fi;\n";
    public static final String PARSE_YOUTUBE = "youtube-dl --skip-download --print-json https://www.youtube.com/watch?v=QnjtfMZZnOw > ";
    public static final String CMD_CHECK_YOUTUBE_DL = "youtube-dl --version>/dev/null 2>&1;if [ $? -ne 0 ]; then echo -1; else echo 0;fi;\n";

    public static final String SUCCESS_CODE = "0";
    public static final String FAIL_CODE = "-1";
    public static final int TASK_TYPE_NO = 0;
    public static final int TASK_TYPE_INSTALL_YOUTUBE = 1;
    public static final int TASK_TYPE_PARSE_YOUTUBE = 2;
    public static final int TASK_TYPE_CHECK_YOUTUBE_DL = 3;
    public static int sTaskType = TASK_TYPE_NO;

    private Activity mActivity;
    private TermuxHandle mInitHandle;
    private TermuxHandle mExecHandle;

    private TerminalSession mSession;

    public void init(Activity activity, @NonNull TermuxHandle handle) {
        mActivity = activity;
        mInitHandle = handle;

        TermuxInstaller.setupIfNeeded(mActivity, () -> {
            try {
                mSession = createSession();
            } catch (Exception e) {
                if (mInitHandle != null) {
                    mInitHandle.init(false);
                }
            }
            if (mInitHandle != null) {
                mInitHandle.init(mSession != null);
            }
        });
    }


    public TerminalSession getSession() {
        return mSession;
    }

    public TermuxHandle getInitHandle() {
        return mInitHandle;
    }

    public TermuxHandle getExecHandle() {
        return mExecHandle;
    }

    public void execute(String cmd, int taskType, TermuxHandle execHandle) {
        if (execHandle == null) {
            Log.e("LLL", "the execHandle cannot be null");
            return;
        }
        this.mExecHandle = execHandle;
        sTaskType = taskType;
        if (mSession == null) {
            if (mActivity == null) {
                mExecHandle.execute(false, null);
                return;
            }
            this.mInitHandle = mExecHandle;
            TermuxInstaller.setupIfNeeded(mActivity, () -> {
                try {
                    mSession = createSession();
                } catch (Exception e) {
                    mExecHandle.execute(false, null);
                }
                mExecHandle.execute(mSession != null, null);
                if (mSession != null) {
                    mSession.write(cmd);
                }
            });
        } else {
            mSession.write(cmd);
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
        session.initializeEmulator(Integer.MAX_VALUE, 50);

        return session;
    }
}
