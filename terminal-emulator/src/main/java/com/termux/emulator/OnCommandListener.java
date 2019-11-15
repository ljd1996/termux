package com.termux.emulator;


/**
 * @author liujiadong
 * @since 2019/8/29
 */
public interface OnCommandListener {

    void execute(String cmd, boolean isSuccess);
}
