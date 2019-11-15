package com.termux.helper;

import android.util.Log;

import com.termux.emulator.TerminalSession;
import com.termux.util.Termux;

import java.util.Vector;

/**
 * @author liujiadong
 * @since 2019/11/14
 */
class SessionPool {
    private static final int MAX_ACTIVE = 8;

    private Vector<TerminalSession> mSessions;

    SessionPool() {
        mSessions = new Vector<>();
    }

    synchronized TerminalSession getSession() {
        TerminalSession session = getFreeSession();
        if (session != null) {
            return session;
        }

        Log.d(Termux.TAG, "There is no free session.");

        if (mSessions.size() < MAX_ACTIVE) {
            session = TerminalSession.createSession();
            mSessions.add(session);
            return session;
        }

        Log.d(Termux.TAG, "The number of sessions is up to the maximum");

        // 等待100ms再获取一次session
        wait(100);
        return getFreeSession();
    }

    void closeSession(TerminalSession session) {
        if (session != null) {
            session.finishIfRunning();
        }
    }


    private TerminalSession getFreeSession() {
        for (TerminalSession session : mSessions) {
            if (session.isFree()) {
                return session;
            }
        }
        return null;
    }

    private void wait(int m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
