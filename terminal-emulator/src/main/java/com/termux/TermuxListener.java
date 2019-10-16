package com.termux;


/**
 * @author liujiadong
 * @since 2019/8/29
 */
public interface TermuxListener {

    void execute(String cmd, boolean isSuccess);
}
