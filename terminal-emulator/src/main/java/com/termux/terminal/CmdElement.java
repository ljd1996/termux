package com.termux.terminal;

import com.termux.TermuxListener;

/**
 * @author liujiadong
 * @since 2019/10/15
 */
public class CmdElement {
    private String mCmd;
    private TermuxListener mListener;

    public CmdElement(String cmd, TermuxListener listener) {
        this.mCmd = cmd;
        this.mListener = listener;
    }

    public String getCmd() {
        return mCmd;
    }

    public void setCmd(String cmd) {
        this.mCmd = cmd;
    }

    public TermuxListener getListener() {
        return mListener;
    }

    public void setListener(TermuxListener listener) {
        this.mListener = listener;
    }
}
