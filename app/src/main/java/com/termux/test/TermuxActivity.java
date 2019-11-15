package com.termux.test;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.helper.TermuxHelper;
import com.termux.util.Termux;


/**
 * @author liujiadong
 * @since 2019/8/1
 */
public class TermuxActivity extends Activity {

    private EditText mEtCmd;
    private EditText mEtPath;
    private TextView mTvResult;
    private Handler mHandle = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.drawer_layout);
        mEtCmd = findViewById(R.id.et_cmd);
        mEtPath = findViewById(R.id.et_path);
        mTvResult = findViewById(R.id.tv_result);

        if (hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            TermuxHelper.getInstance().initTermux(this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        }
    }

    public static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requestCode == 200) {
            if (hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                TermuxHelper.getInstance().initTermux(this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
            }
        }
    }

    public void btn1(View view) {
        String cmd = mEtCmd.getText().toString();
        String path = mEtPath.getText().toString();
        TermuxHelper.getInstance().execute(this, cmd, path, (code, responseText) -> {
            Log.d(Termux.TAG, "code = " + code);
            Log.d(Termux.TAG, "responseText = " + responseText);
            mHandle.post(() -> mTvResult.setText("code: " + code + "\nresponseText: " + responseText));
        });
    }

    public void btn2(View view) {
        mEtCmd.setText(R.string.cmd_parse_youtube_with_path);
        mEtPath.setText(R.string.result_path);
    }

    public void btn3(View view) {
        mEtCmd.setText("");
        mEtPath.setText("");
        mTvResult.setText("");
    }

    public void test(View view) {
        TermuxHelper.getInstance().execute(this, "youtube-dl --skip-download --print-json https://www.youtube.com/watch?v=QnjtfMZZnOw",
                null, (code, responseText) -> Log.d(Termux.TAG, "parse code = " + code));
        TermuxHelper.getInstance().execute(this, "youtube-dl --skip-download --print-json https://www.youtube.com/watch?v=QnjtfMZZnOw",
                null, (code, responseText) -> Log.d(Termux.TAG, "parse code = " + code));
        TermuxHelper.getInstance().execute(this, "youtube-dl --skip-download --print-json https://www.youtube.com/watch?v=QnjtfMZZnOw",
                null, (code, responseText) -> Log.d(Termux.TAG, "parse code = " + code));
        new Thread(() -> TermuxHelper.getInstance().execute(this, "youtube-dl --skip-download --print-json https://www.youtube.com/watch?v=QnjtfMZZnOw",
                null, (code, responseText) -> Log.d(Termux.TAG, "parse code = " + code))).start();
        new Thread(() -> TermuxHelper.getInstance().execute(this, "youtube-dl --skip-download --print-json https://www.youtube.com/watch?v=QnjtfMZZnOw",
                null, (code, responseText) -> Log.d(Termux.TAG, "parse code = " + code))).start();
        new Thread(() -> TermuxHelper.getInstance().execute(this, "youtube-dl --skip-download --print-json https://www.youtube.com/watch?v=QnjtfMZZnOw",
                null, (code, responseText) -> Log.d(Termux.TAG, "parse code = " + code))).start();
    }
}
