package com.termux.emulator;

/**
 * @author liujiadong
 * @since 2019/10/15
 */
class Command {

    private String mCmd;
    private OnCommandListener mListener;


    Command(String cmd) {
        this.mCmd = cmd;
    }

    String getCmd() {
        return mCmd;
    }

    OnCommandListener getListener() {
        return mListener;
    }

    void setListener(OnCommandListener listener) {
        this.mListener = listener;
    }
}
