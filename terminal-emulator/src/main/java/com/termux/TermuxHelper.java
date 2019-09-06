package com.termux;

import android.content.Context;
import android.util.Log;

import com.termux.terminal.TermuxDebug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;


/**
 * @author liujiadong
 * @since 2019/8/1
 */
public class TermuxHelper {

    public static final String CMD_INSTALL_YOUTUBE_DL = "apt update&&apt -y install python2&&pip2 install --upgrade pip&&pip install --upgrade youtube-dl";
    public static final String CMD_PARSE_YOUTUBE = "youtube-dl --skip-download --print-json ";
    public static final String CMD_CHECK_YOUTUBE_DL = "youtube-dl --version";


    public void initTermux(Context context) {
        Termux.getInstance().execute(context, null, null);
    }

    public void parseYoutube(Context context, String url, String resultPath, OnParseYoutubeListener listener) {
        Termux.getInstance().execute(context, CMD_CHECK_YOUTUBE_DL, new TermuxListener() {
            @Override
            public void init(boolean isSuccess) {
                if (!isSuccess && listener != null) {
                    listener.parseResult(-1, null);
                }
            }

            @Override
            public void execute(String cmd, boolean isSuccess) {
                Log.d(TermuxDebug.LOG_TAG, cmd + ": " + isSuccess);
                if (!isSuccess) {
                    Termux.getInstance().execute(context, CMD_INSTALL_YOUTUBE_DL, new TermuxListener() {
                        @Override
                        public void init(boolean isSuccess) {
                            if (!isSuccess && listener != null) {
                                listener.parseResult(-1, null);
                            }
                        }

                        @Override
                        public void execute(String cmd, boolean isSuccess) {
                            Log.d(TermuxDebug.LOG_TAG, cmd + ": " + isSuccess);
                            if (isSuccess) {
                                doParse(context, url, resultPath, listener);
                            } else if (listener != null) {
                                listener.parseResult(-1, null);
                            }
                        }
                    });
                } else {
                    doParse(context, url, resultPath, listener);
                }
            }
        });
    }

    private void doParse(Context context, String url, String resultPath, OnParseYoutubeListener listener) {
        Termux.getInstance().execute(context, CMD_PARSE_YOUTUBE + url +
                ">" + resultPath, new TermuxListener() {
            @Override
            public void init(boolean isSuccess) {
                if (!isSuccess) {
                    Termux.getInstance().closeSession();
                    if (listener != null) {
                        listener.parseResult(-1, null);
                    }
                }
            }

            @Override
            public void execute(String cmd, boolean isSuccess) {
                Log.d(TermuxDebug.LOG_TAG, cmd + ": " + isSuccess);
                Termux.getInstance().closeSession();
                if (listener == null) {
                    return;
                }
                if (!isSuccess) {
                    listener.parseResult(-1, null);
                } else {
                    listener.parseResult(0, readFile(resultPath));
                }
            }
        });
    }

    private String readFile(String path) {
        File file = new File(path);
        FileInputStream is;
        StringBuilder stringBuilder = null;
        try {
            if (file.length() != 0) {
                is = new FileInputStream(file);
                InputStreamReader streamReader = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(streamReader);
                String line;
                stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.valueOf(stringBuilder);
    }

    public interface OnParseYoutubeListener {
        void parseResult(int code, String responseText);
    }

}
