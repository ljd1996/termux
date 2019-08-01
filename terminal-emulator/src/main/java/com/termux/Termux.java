package com.termux;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.termux.app.TermuxInstaller;
import com.termux.app.TermuxService;
import com.termux.terminal.TerminalSession;

/**
 * @author liujiadong
 * @since 2019/8/1
 */
public class Termux {

    private static final String CMD_GET_YOUTUBE_DL = "apt update&&apt -y install python&&pip install --upgrade pip&&pip install --upgrade youtube-dl\n";

    public static final String INSTALL_SUCCESS = "Successfully installed youtube-dl";
    public static final String HAS_INSTALL = "Usage";

    private Activity mActivity;
    private TermuxService mTermService;
    private TerminalSession mSession;
    private TermuxInitHandle mTermuxInitHandle;

    private boolean mIsInstalled = false;

    public Termux(Activity activity, @NonNull TermuxInitHandle handle) {
        mActivity = activity;
        mTermuxInitHandle = handle;
    }

    public TerminalSession getSession() {
        return mSession;
    }

    public void setIsInstalled(boolean mIsInstalled) {
        this.mIsInstalled = mIsInstalled;
    }

    @UiThread
    public void install() {
        bindService();
    }

    public boolean isInstalled() {
        if (mTermService == null) {
            bindService();
        }
        return true;
    }

    private void bindService() {
        Intent serviceIntent = new Intent(mActivity, TermuxService.class);
        mActivity.startService(serviceIntent);
        if (!mActivity.bindService(serviceIntent, new TermuxConnection(), 0)) {
            throw new RuntimeException("bindService() failed");
        }
    }

    class TermuxConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mTermService = ((TermuxService.LocalBinder) iBinder).service;

            if (mSession == null) {
                TermuxInstaller.setupIfNeeded(mActivity, mTermuxInitHandle, () -> {
                    try {
                        mSession = mTermService.createTermSession(null, null, null, false);
                        mSession.initializeEmulator(500, 50);
                        mSession.setTermux(Termux.this);

                        mSession.write(CMD_GET_YOUTUBE_DL);
                    } catch (Exception e) {
                        mTermuxInitHandle.initFail();
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mTermService = null;
        }
    }

    public interface TermuxInitHandle {
        void success();

        void initFail();

        void youtubeInstallFail();
    }
}
