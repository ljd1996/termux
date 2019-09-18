package com.termux.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.termux.R;
import com.termux.TermuxHelper;
import com.termux.Termux;
import com.termux.TermuxListener;


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

        TermuxHelper helper = new TermuxHelper();
        helper.initTermux(this);
    }

    public void btn1(View view) {
        editText = findViewById(R.id.edit);
        String cmd = editText.getText().toString().trim();
        if (!cmd.startsWith("youtube-dl --skip-download --print-json")) {
            Termux.getInstance().execute(this, cmd, new TermuxListener() {
                @Override
                public void init(boolean isSuccess) {

                }

                @Override
                public void execute(String cmd, boolean isSuccess) {

                }
            });
        } else {
            TermuxHelper helper = new TermuxHelper();
            helper.parseYoutube(this, "https://www.youtube.com/watch?v=QnjtfMZZnOw",
                    "/sdcard/result.json", (code, responseText) -> {

                    });
        }
    }

    public void btn2(View view) {
        TermuxHelper helper = new TermuxHelper();
        helper.parseYoutube(this, "https://www.youtube.com/watch?v=QnjtfMZZnOw",
                "/sdcard/result.json", (code, responseText) -> {

                });
    }

    public void btn3(View view) {
        editText = findViewById(R.id.edit);
        editText.setText(R.string.cmd_parse_youtube_prefix);
    }

    public void btn4(View view) {
        textView.setText("");
    }
}
