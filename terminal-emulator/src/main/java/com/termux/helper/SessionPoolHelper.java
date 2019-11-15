package com.termux.helper;

import android.content.Context;

import com.termux.emulator.OnCommandListener;
import com.termux.emulator.TerminalSession;

/**
 * @author liujiadong
 * @since 2019/11/15
 */
class SessionPoolHelper {

    private SessionPool mSessionPool;


    private SessionPoolHelper() {
        mSessionPool = new SessionPool();
    }

    private static class SingleTon {
        private static SessionPoolHelper sInstance = new SessionPoolHelper();
    }

    static SessionPoolHelper getInstance() {
        return SingleTon.sInstance;
    }

    void execute(Context context, String cmd, OnCommandListener listener) {
        TerminalSession session = mSessionPool.getSession();
        if (session == null) {
            listener.execute(cmd, false);
        } else {
            session.execute(context, cmd, listener);
        }
    }
}
