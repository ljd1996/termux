package com.termux.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.termux.terminal.EmulatorDebug;
import com.termux.terminal.TerminalSession;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * running so that it is not terminated. The user interacts with the session through activity, but this
 * service may outlive the activity when the user or the system disposes of the activity. In that case the user may
 * restart activity later to yet again access the sessions.
 * <p/>
 * In order to keep both terminal sessions and spawned processes (who may outlive the terminal sessions) alive as long
 * as wanted by the user this service is a foreground service, {@link Service#startForeground(int, Notification)}.
 * <p/>
 * Optionally may hold a wake and a wifi lock, in which case that is shown in the notification - see
 */
public final class TermuxService extends Service {

    /** Note that this is a symlink on the Android M preview. */
    @SuppressLint("SdCardPath")
    public static final String FILES_PATH = "/data/data/com.termux/files";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";

    public static final String EXTRA_ARGUMENTS = "com.termux.execute.arguments";

    public static final String EXTRA_CURRENT_WORKING_DIRECTORY = "com.termux.execute.cwd";

    public static final String ACTION_EXECUTE = "com.termux.service_execute";

    private static final String EXTRA_EXECUTE_IN_BACKGROUND = "com.termux.execute.background";

    /** This service is only bound from inside the same process and never uses IPC. */
    public class LocalBinder extends Binder {
        public final TermuxService service = TermuxService.this;
    }

    private final IBinder mBinder = new LocalBinder();

    private final Handler mHandler = new Handler();

    final List<BackgroundJob> mBackgroundTasks = new ArrayList<>();

    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        if (ACTION_EXECUTE.equals(intent.getAction())) {
//            Uri executableUri = intent.getData();
//            String executablePath = (executableUri == null ? null : executableUri.getPath());
//
//            String[] arguments = (executableUri == null ? null : intent.getStringArrayExtra(EXTRA_ARGUMENTS));
//            String cwd = intent.getStringExtra(EXTRA_CURRENT_WORKING_DIRECTORY);
//
//            if (intent.getBooleanExtra(EXTRA_EXECUTE_IN_BACKGROUND, false)) {
//                BackgroundJob task = new BackgroundJob(cwd, executablePath, arguments, this);
//                mBackgroundTasks.add(task);
//            } else {
//                boolean failsafe = intent.getBooleanExtra(TermuxActivity.TERMUX_FAILSAFE_SESSION_ACTION, false);
//                TerminalSession newSession = createTermSession(executablePath, arguments, cwd, failsafe);
//
//                // Make the newly created session the current one to be displayed:
//                TermuxPreferences.storeCurrentSession(this, newSession);
//
//                // Launch the main Termux app, which will now show the current session:
//                startActivity(new Intent(this, TermuxActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//            }
//        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        File termuxTmpDir = new File(TermuxService.PREFIX_PATH + "/tmp");

        if (termuxTmpDir.exists()) {
            try {
                TermuxInstaller.deleteFolder(termuxTmpDir.getCanonicalFile());
            } catch (Exception e) {
                Log.e(EmulatorDebug.LOG_TAG, "Error while removing file at " + termuxTmpDir.getAbsolutePath(), e);
            }

            termuxTmpDir.mkdirs();
        }
    }


    public TerminalSession createTermSession(String executablePath, String[] arguments, String cwd, boolean failSafe) {
        new File(HOME_PATH).mkdirs();

        if (cwd == null) {
            cwd = HOME_PATH;
        }

        String[] env = BackgroundJob.buildEnvironment(failSafe, cwd);
        boolean isLoginShell = false;

        if (executablePath == null) {
            if (!failSafe) {
                for (String shellBinary : new String[]{"login", "bash", "zsh"}) {
                    File shellFile = new File(PREFIX_PATH + "/bin/" + shellBinary);
                    if (shellFile.canExecute()) {
                        executablePath = shellFile.getAbsolutePath();
                        break;
                    }
                }
            }

            if (executablePath == null) {
                // Fall back to system shell as last resort:
                executablePath = "/system/bin/sh";
            }
            isLoginShell = true;
        }

        String[] processArgs = BackgroundJob.setupProcessArgs(executablePath, arguments);
        executablePath = processArgs[0];
        int lastSlashIndex = executablePath.lastIndexOf('/');
        String processName = (isLoginShell ? "-" : "") +
            (lastSlashIndex == -1 ? executablePath : executablePath.substring(lastSlashIndex + 1));

        String[] args = new String[processArgs.length];
        args[0] = processName;
        if (processArgs.length > 1) System.arraycopy(processArgs, 1, args, 1, processArgs.length - 1);

        return new TerminalSession(executablePath, cwd, args, env);
    }

    public void onBackgroundJobExited(final BackgroundJob task) {
        mHandler.post(() -> {
            mBackgroundTasks.remove(task);
        });
    }

}
