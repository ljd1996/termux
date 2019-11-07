package com.termux;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.termux.terminal.TermuxDebug;
import com.termux.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;


/**
 * @author liujiadong
 * @since 2019/8/1
 */
public class TermuxHelper {

    private static final String SP_TERMUX_NAME = "sp_termux";
    private static final String SP_TERMUX_IS_INITED = "isInited";
    private static final String YOUTUBE_DL_PKG_NAME = "youtube-dl";

    private static final String CMD_INSTALL_YOUTUBE_DL = "python2 -m ensurepip --upgrade --no-default-pip&&pip2 install --upgrade youtube-dl&&pip2 install youtube-dl-server";
    private static final String CMD_UPDATE_YOUTUBE_DL = "pip2 install --upgrade youtube-dl";
    private static final String CMD_START_YOUTUBE_SERVER = "(nohup youtube-dl-server --number-processes 1 --host 0.0.0.0 --port 9191 &)";
    private static final String CMD_PARSE_YOUTUBE = "curl http://0.0.0.0:9191/api/info?url=";
    private static final String CMD_CHECK_YOUTUBE_DL = "pip2 list --outdated>" + Termux.TMP_FILE + " 2>&1&&grep 'youtube-dl' " + Termux.TMP_FILE + "|cat >" + Termux.TMP_FILE;
    private static final String CMD_YOUTUBE_DL_VERSION = "youtube-dl --version > " + Termux.TMP_FILE;

    private static boolean sIsDlUpdating = false;
    private static boolean sFirstCheckDlUpdate = true;
    private static boolean sIsInitting = false;

    public static boolean isInited(Context context) {
        return getSPref(context).getBoolean(TermuxHelper.SP_TERMUX_IS_INITED, false);
    }

    public static void setInited(Context context) {
        SharedPreferences preferences = getSPref(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(TermuxHelper.SP_TERMUX_IS_INITED, true);
        editor.apply();
    }

    private static SharedPreferences getSPref(Context context) {
        return context.getSharedPreferences(TermuxHelper.SP_TERMUX_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context, OnExecuteListener listener) {
        if (sIsInitting || listener == null) {
            if (listener != null) {
                listener.onResult(-1, null);
            }
            return;
        }
        sIsInitting = true;
        new Thread(() -> initOnThread(context, listener)).start();
    }

    private static void initOnThread(Context context, OnExecuteListener listener) {
        if (context == null || listener == null) {
            if (listener != null) {
                listener.onResult(-1, null);
                sIsInitting = false;
            }
            return;
        }
        Termux.getInstance().execute(context, null, (cmd, isSuccess) -> {
            Log.d(TermuxDebug.TAG, cmd + ": " + isSuccess);

            if (isSuccess) {

                Termux.getInstance().execute(context, CMD_INSTALL_YOUTUBE_DL, (cmd11, isSuccess11) -> {
                    Log.d(TermuxDebug.TAG, cmd11 + ": " + isSuccess11);

                    if (isSuccess11) {
                        Termux.getInstance().execute(context, CMD_START_YOUTUBE_SERVER, (cmd1, isSuccess1) -> {
                            Log.d(TermuxDebug.TAG, cmd1 + ": " + isSuccess1);
                            getDlVersion(context, (code, responseText) -> {
                                sFirstCheckDlUpdate = false;
                                listener.onResult(0, "");
                                sIsInitting = false;
                            });
                        });
                    } else {
                        listener.onResult(-1, null);
                        sIsInitting = false;
                    }
                });
            } else {
                sIsInitting = false;
                listener.onResult(-1, null);
            }
        });
    }

    public static void parseYoutube(Context context, String url, OnExecuteListener listener) {
        if (context == null || TextUtils.isEmpty(url) || listener == null) {
            if (listener != null) {
                listener.onResult(-1, null);
            }
            return;
        }
        new Thread(() -> parseOnThread(context, url, listener)).start();
    }

    private static void parseOnThread(Context context, String url, OnExecuteListener listener) {
        if (sFirstCheckDlUpdate) {
            sFirstCheckDlUpdate = false;
            sIsDlUpdating = true;
            getDlVersion(context, (code, responseText) -> {
                Termux.getInstance().execute(context, CMD_CHECK_YOUTUBE_DL, (cmd, isSuccess) -> {
                    Log.d(TermuxDebug.TAG, cmd + ": " + isSuccess);
                    if (isSuccess) {
                        String versionInfo = readFile(Termux.TMP_FILE);
                        Log.d(TermuxDebug.TAG, "versionInfo = " + versionInfo);
                        if (!TextUtils.isEmpty(versionInfo) && versionInfo.trim().startsWith(YOUTUBE_DL_PKG_NAME)) {
                            listener.onResult(-1, null);
                            String newVersion = null;
                            try {
                                newVersion = versionInfo.split(" ")[2];
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Termux.getInstance().execute(context, CMD_UPDATE_YOUTUBE_DL, (cmd1, isSuccess1) -> {
                                Log.d(TermuxDebug.TAG, cmd1 + ": " + isSuccess1);
                                if (isSuccess1) {
                                } else {
                                }
                                sIsDlUpdating = false;
                            });
                        } else {
                            sIsDlUpdating = false;
                            doParse(context, url, listener);
                        }
                    } else {
                        sIsDlUpdating = false;
                        listener.onResult(-1, null);
                    }
                });
            });
        } else if (sIsDlUpdating) {
            listener.onResult(-1, null);
        } else {
            doParse(context, url, listener);
        }
    }

    private static void getDlVersion(Context context, OnExecuteListener listener) {
        Termux.getInstance().execute(context, CMD_YOUTUBE_DL_VERSION, ((cmd, isSuccess) -> {
            Log.d(TermuxDebug.TAG, cmd + ": " + isSuccess);

            if (listener == null) {
                return;
            }
            if (isSuccess) {
                listener.onResult(0, readFile(Termux.TMP_FILE));
            } else {
                listener.onResult(-1, null);
            }
        }));
    }

    private static void doParse(Context context, String url, OnExecuteListener listener) {
        Log.d(TermuxDebug.TAG, "begin to parse...");

        String path = getDlFileName(url);
        Termux.getInstance().execute(context, CMD_PARSE_YOUTUBE + url +
                ">" + path, (cmd, isSuccess) -> {
            Log.d(TermuxDebug.TAG, cmd + ": " + isSuccess);
            if (listener == null) {
                return;
            }
            if (!isSuccess) {
                listener.onResult(-1, null);
            } else {
                listener.onResult(0, readFile(path));
            }
        });
    }

    private static String getDlFileName(String url) {
        return Termux.HOME_PATH + File.separator + FileUtil.md5(url + System.currentTimeMillis());
    }

    private static String readFile(String path) {
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

    public interface OnExecuteListener {
        void onResult(int code, String responseText);
    }

}
