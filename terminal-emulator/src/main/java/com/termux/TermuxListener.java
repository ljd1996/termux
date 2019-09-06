package com.termux;


/**
 * @author liujiadong
 * @since 2019/8/29
 */
public interface TermuxListener {

    void init(boolean isSuccess);

    void execute(String cmd, boolean isSuccess);
}
