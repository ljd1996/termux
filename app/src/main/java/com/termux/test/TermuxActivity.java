package com.termux.test;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.TermuxHelper;
import com.termux.Termux;
import com.termux.terminal.TermuxDebug;


/**
 * @author liujiadong
 * @since 2019/8/1
 */
public class TermuxActivity extends Activity {

    private EditText editText;
    private TextView textView;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.drawer_layout);

        textView = findViewById(R.id.text);

        if (hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            TermuxHelper.initTermux(this);
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
                TermuxHelper.initTermux(this);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
            }
        }
    }

    public void btn1(View view) {
        editText = findViewById(R.id.edit);
        String cmd = editText.getText().toString().trim();
        if (!cmd.startsWith("youtube-dl --skip-download --print-json")) {
            Termux.getInstance().execute(this, cmd, (cmd1, isSuccess) -> {
            });
        } else {
            TermuxHelper.parseYoutube(this, "https://www.youtube.com/watch?v=QnjtfMZZnOw",
                    (code, responseText) -> Log.d(TermuxDebug.TAG, "1 parse code = " + code));
            TermuxHelper.parseYoutube(this, "https://www.youtube.com/watch?v=QnjtfMZZnOw",
                    (code, responseText) -> Log.d(TermuxDebug.TAG, "2 parse code = " + code));
        }
    }

    public void btn2(View view) {
        TermuxHelper.parseYoutube(this, "https://www.youtube.com/watch?v=QnjtfMZZnOw",
                (code, responseText) -> Log.d(TermuxDebug.TAG, "parse code = " + code));
    }

    public void btn3(View view) {
        editText = findViewById(R.id.edit);
        editText.setText(R.string.cmd_parse_youtube_prefix);
    }

    public void btn4(View view) {
        textView.setText("");
    }
}
