package com.termux.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.termux.R;
import com.termux.TermuxHelper;
import com.termux.Termux;
import com.termux.TermuxListener;
import com.termux.terminal.TermuxDebug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;


/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public class TermuxActivity extends Activity {

    private EditText editText;
    private TextView textView;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.drawer_layout);

        textView = findViewById(R.id.text);
    }

    public void btn1(View view) {
        editText = findViewById(R.id.edit);
        String cmd = editText.getText().toString().trim();
//        if (cmd.startsWith("youtube-dl --skip-download --print-json")) {
//            TermuxHelper.mInstance.execute(TermuxHelper.CMD_PARSE_YOUTUBE + getExternalCacheDir() + File.separator + "result.json;"
//                    + "if [ $? -ne 0 ]; then echo 1; else echo 0;fi;\n", new TermuxListener() {
//                @Override
//                public void init(boolean isSuccess) {
//
//                }
//
//                @Override
//                public void execute(boolean isSuccess, String cmd) {
//                    if (isSuccess) {
//                        e(TermuxDebug.LOG_TAG, readFile(getExternalCacheDir() + File.separator + "result.json"));
//                    }
//                }
//            });
//        }
    }

    public static void e(String tag, String msg) {
        if (tag == null || tag.length() == 0 || msg == null || msg.length() == 0) return;
        int segmentSize = 3 * 1024;
        long length = msg.length();
        if (length <= segmentSize) {
            // 长度小于等于限制直接打印
            Log.e(tag, msg);
        } else {
            while (msg.length() > segmentSize) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize);
                msg = msg.replace(logContent, "");
                Log.e(tag, logContent);
            }
            Log.e(tag, msg);// 打印剩余日志
        }
    }

    public void btn2(View view) {
        TermuxHelper helper = new TermuxHelper();
        helper.parseYoutube(this, "https://www.youtube.com/watch?v=QnjtfMZZnOw");
    }

    public void btn3(View view) {
        editText = findViewById(R.id.edit);
        editText.setText(R.string.cmd_parse_youtube_prefix);
    }

    public void btn4(View view) {
        textView.setText("");
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

}
