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

        initTermux();
    }

    private void initTermux() {
        if (!TermuxHelper.isInited(this)) {
            Log.d(TermuxDebug.TAG, "begin to init");
            TermuxHelper.init(this, (code, responseText) -> {
                Log.d(TermuxDebug.TAG, "init termux code = " + code);
                if (code == 0) {
                    TermuxHelper.setInited(this);
                }
            });
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
