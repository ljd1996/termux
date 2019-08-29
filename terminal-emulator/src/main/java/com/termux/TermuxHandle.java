package com.termux;


/**
 * @author liujiadong
 * @since 2019/8/29
 */
public interface TermuxHandle {

    void init(boolean isSuccess);

    void execute(boolean isSuccess, String output);
}
