package com.termux.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.termux.app.TermuxInstaller;
import com.termux.util.FileUtil;
import com.termux.util.Termux;


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
    private static final String CMD_PARSE_YOUTUBE = "curl --connect-timeout 5 -m 10 http://0.0.0.0:9191/api/info?url=";
    private static final String CMD_CHECK_YOUTUBE_DL = "pip2 list --outdated>" + Termux.TMP_FILE + " 2>&1&&grep 'youtube-dl' " + Termux.TMP_FILE + "|cat >" + Termux.TMP_FILE1;
    private static final String CMD_YOUTUBE_DL_VERSION = "youtube-dl --version > " + Termux.TMP_FILE;
    private static final String CMD_KILL_YOUTUBE_DL = "ps -ef>" + Termux.TMP_FILE1 + " 2>&1&&grep 'youtube-dl' " + Termux.TMP_FILE1 + "|cat >" + Termux.TMP_FILE1;
    private static final String CMD_KILL_PROCESS = "kill -9 ";

    private volatile boolean sIsDlUpdating = false;
    private volatile boolean sFirstCheckDlUpdate = true;
    private volatile boolean sIsInitting = false;
    private volatile static boolean sFirstStartServer = true;

    private TermuxHelper() {
    }

    private static class SingleTon {
        private static TermuxHelper sInstance = new TermuxHelper();
    }

    public static TermuxHelper getInstance() {
        return SingleTon.sInstance;
    }

    private SharedPreferences getSPref(Context context) {
        return context.getSharedPreferences(TermuxHelper.SP_TERMUX_NAME, Context.MODE_PRIVATE);
    }

    private boolean hasInit(Context context) {
        return getSPref(context).getBoolean(TermuxHelper.SP_TERMUX_IS_INITED, false) && FileUtil.usrExist();
    }

    private void setInit(Context context) {
        SharedPreferences preferences = getSPref(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(TermuxHelper.SP_TERMUX_IS_INITED, true);
        editor.apply();
    }

    public void initTermux(Context context) {
        if (context == null) {
            return;
        }
        synchronized (this) {
            if (!hasInit(context)) {
                if (sIsInitting) {
                    return;
                }
                Log.d(Termux.TAG, "begin to init");
                sIsInitting = true;

                new Thread(() -> initOnThread(context, (code, responseText) -> {
                    Log.d(Termux.TAG, "init termux code = " + code);
                    if (code == 0) {
                        setInit(context);
                    }
                })).start();
            } else if (sFirstStartServer) {
                SessionPoolHelper.getInstance().execute(context, CMD_START_YOUTUBE_SERVER, (cmd1, isSuccess1) -> Log.d(Termux.TAG, cmd1 + ": " + isSuccess1));
                sFirstStartServer = false;
            }
        }
    }

    private void initOnThread(Context context, OnExecuteListener listener) {

        TermuxInstaller installer = new TermuxInstaller();
        installer.setListener((cmd, isSuccess) -> {
            if (isSuccess) {
                SessionPoolHelper.getInstance().execute(context, CMD_INSTALL_YOUTUBE_DL, (cmd1, isSuccess1) -> {
                    Log.d(Termux.TAG, cmd1 + ": " + isSuccess1);

                    if (isSuccess1) {
                        SessionPoolHelper.getInstance().execute(context, CMD_START_YOUTUBE_SERVER, (cmd11, isSuccess11) -> {
                            Log.d(Termux.TAG, cmd11 + ": " + isSuccess11);

                            getDlVersion(context, (code, responseText) -> {
                                sFirstCheckDlUpdate = false;
                                listener.onResult(0, "");
                                sIsInitting = false;
                                sFirstStartServer = false;
                            });
                        });
                    } else {
                        listener.onResult(-1, null);
                        sIsInitting = false;
                    }
                });
            } else {
                listener.onResult(-1, null);
                sIsInitting = false;
            }
        });
        installer.setupIfNeeded(context);
    }

    public void parseYoutube(Context context, String url, OnExecuteListener listener) {
        if (context == null || TextUtils.isEmpty(url) || listener == null) {
            if (listener != null) {
                listener.onResult(-1, null);
            }
            return;
        }
        new Thread(() -> parseOnThread(context, url, listener)).start();
    }

    private void parseOnThread(Context context, String url, OnExecuteListener listener) {
        if (sFirstCheckDlUpdate) {
            sFirstCheckDlUpdate = false;
            sIsDlUpdating = true;
            getDlVersion(context, (code, responseText) -> SessionPoolHelper.getInstance().execute(context, CMD_CHECK_YOUTUBE_DL, (cmd, isSuccess) -> {
                Log.d(Termux.TAG, cmd + ": " + isSuccess);

                if (isSuccess) {
                    String versionInfo = FileUtil.readFile(Termux.TMP_FILE1);
                    Log.d(Termux.TAG, "versionInfo = " + versionInfo);

                    if (!TextUtils.isEmpty(versionInfo) && versionInfo.trim().startsWith(YOUTUBE_DL_PKG_NAME)) {
                        listener.onResult(-1, null);
                        String newVersion = null;
                        try {
                            newVersion = versionInfo.split(" ")[2];
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        SessionPoolHelper.getInstance().execute(context, CMD_UPDATE_YOUTUBE_DL, (cmd1, isSuccess1) -> {
                            Log.d(Termux.TAG, cmd1 + ": " + isSuccess1);
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
            }));
        } else if (sIsDlUpdating) {
            listener.onResult(-1, null);
        } else {
            doParse(context, url, listener);
        }
    }

    private static void getDlVersion(Context context, OnExecuteListener listener) {
        SessionPoolHelper.getInstance().execute(context, CMD_YOUTUBE_DL_VERSION, ((cmd, isSuccess) -> {
            Log.d(Termux.TAG, cmd + ": " + isSuccess);

            if (listener == null) {
                return;
            }
            if (isSuccess) {
                listener.onResult(0, FileUtil.readFile(Termux.TMP_FILE));
            } else {
                listener.onResult(-1, null);
            }
        }));
    }

    private static void doParse(Context context, String url, OnExecuteListener listener) {
        Log.d(Termux.TAG, "begin to parse...");

        String path = FileUtil.getDlFileName(url);
        SessionPoolHelper.getInstance().execute(context, CMD_PARSE_YOUTUBE + url +
                ">" + path, (cmd, isSuccess) -> {
            Log.d(Termux.TAG, cmd + ": " + isSuccess);
            if (listener == null) {
                return;
            }
            if (!isSuccess) {
                listener.onResult(-1, null);
            } else {
                listener.onResult(0, FileUtil.readFile(path));
            }
        });
    }

    public void execute(Context context, String cmd, String resultFile, OnExecuteListener listener) {
        if (context == null || TextUtils.isEmpty(cmd) || listener == null) {
            if (listener != null) {
                listener.onResult(-1, null);
            }
            return;
        }
        new Thread(() -> SessionPoolHelper.getInstance().execute(context, cmd, (cmd1, isSuccess) -> {
            Log.d(Termux.TAG, cmd1 + ": " + isSuccess);
            if (isSuccess) {
                listener.onResult(0, FileUtil.readFile(resultFile));
            } else {
                listener.onResult(-1, null);
            }
        })).start();
    }

    public void killParseTask(Context context) {
        SessionPoolHelper.getInstance().execute(context, CMD_KILL_YOUTUBE_DL, (cmd, isSuccess) -> {
            Log.d(Termux.TAG, cmd + ": " + isSuccess);
            if (isSuccess) {
                String result = FileUtil.readFile(Termux.TMP_FILE1);
                Log.d(Termux.TAG, "result = " + result);
                if (!TextUtils.isEmpty(result)) {
                    try {
                        String pid = result.split(" ")[1].trim();
                        SessionPoolHelper.getInstance().execute(context, CMD_KILL_PROCESS + pid, (cmd1, isSuccess1) -> Log.d(Termux.TAG, cmd1 + ": " + isSuccess1));
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    public interface OnExecuteListener {
        void onResult(int code, String responseText);
    }

}
