package com.termux;


import android.content.Context;
import android.util.Log;

import com.termux.terminal.TermuxDebug;

import java.io.File;

/**
 * @author liujiadong
 * @since 2019/8/1
 */
public class TermuxHelper {


    public static final String CMD_INSTALL_YOUTUBE_DL = "apt update&&apt -y install python2&&pip2 install --upgrade pip&&pip install --upgrade youtube-dl";
    public static final String CMD_PARSE_YOUTUBE = "youtube-dl --skip-download --print-json ";
    public static final String CMD_CHECK_YOUTUBE_DL = "youtube-dl --version";


    public void parseYoutube(Context context, String url) {
        Termux.getInstance().execute(context, CMD_CHECK_YOUTUBE_DL, new TermuxListener() {
            @Override
            public void init(boolean isSuccess) {

            }

            @Override
            public void execute(String cmd, boolean isSuccess) {
                Log.d(TermuxDebug.LOG_TAG, cmd + ": " + isSuccess);
                if (!isSuccess) {
                    Termux.getInstance().execute(context, CMD_INSTALL_YOUTUBE_DL, new TermuxListener() {
                        @Override
                        public void init(boolean isSuccess) {

                        }

                        @Override
                        public void execute(String cmd, boolean isSuccess) {
                            Log.d(TermuxDebug.LOG_TAG, cmd + ": " + isSuccess);
                            if (isSuccess) {
                                doParse(context, url);
                            }
                        }
                    });
                } else {
                    doParse(context, url);
                }
            }
        });
    }

    private void doParse(Context context, String url) {
        Termux.getInstance().execute(context, CMD_PARSE_YOUTUBE + url +
                ">" + context.getExternalCacheDir() + File.separator + "result.json", new TermuxListener() {
            @Override
            public void init(boolean isSuccess) {
                Termux.getInstance().closeSession();
            }

            @Override
            public void execute(String cmd, boolean isSuccess) {
                Log.d(TermuxDebug.LOG_TAG, cmd + ": " + isSuccess);
                Termux.getInstance().closeSession();
            }
        });
    }

}
