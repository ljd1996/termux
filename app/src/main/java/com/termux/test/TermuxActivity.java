package com.termux.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.termux.R;
import com.termux.Termux;
import com.termux.app.TermuxInstaller;


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

    private static final int MSG_SUCCESS = 0;
    private static final int MSG_INIT_FAIL = 1;
    private static final int MSG_INSTALL_FAIL = 2;
    private static final int MSG_PARSE_RST = 3;

    private static final int REQUESTCODE_PERMISSION_STORAGE = 1234;

    private EditText editText;
    private TextView textView;

    private Termux termux;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS:
                    Toast.makeText(TermuxActivity.this, "install successfully!", Toast.LENGTH_LONG).show();
                    break;
                case MSG_INIT_FAIL:
                    Toast.makeText(TermuxActivity.this, "initFail!", Toast.LENGTH_LONG).show();
                    break;
                case MSG_INSTALL_FAIL:
                    Toast.makeText(TermuxActivity.this, "installFail!", Toast.LENGTH_LONG).show();
                    break;
                case MSG_PARSE_RST:
                    String result = msg.obj.toString();
//                    if (result.startsWith(Termux.PARSE_YOUTUBE)) {
//                        textView.setText("");
//                    }
                    textView.append(result);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.drawer_layout);

        textView = findViewById(R.id.text);

        termux = Termux.mInstance.init(this, new Termux.TermuxHandle() {
            @Override
            public void success() {
                Log.d("LLL", "success");
                handler.sendEmptyMessage(MSG_SUCCESS);
            }

            @Override
            public void initFail() {
                Log.d("LLL", "initFail");
                handler.sendEmptyMessage(MSG_INIT_FAIL);
            }

            @Override
            public void installFail() {
                Log.d("LLL", "installFail");
                handler.sendEmptyMessage(MSG_INSTALL_FAIL);
            }

            @Override
            public void parse(String result) {
                Message msg = Message.obtain();
                msg.obj = result;
                msg.what = MSG_PARSE_RST;
                handler.sendMessage(msg);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUESTCODE_PERMISSION_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            TermuxInstaller.setupStorageSymlinks(this);
        }
    }

    public void btn1(View view) {
        editText = findViewById(R.id.edit);
        String cmd = editText.getText().toString().trim();
        termux.getSession().write(cmd + "\n");
    }

    public void btn2(View view) {
        if (!termux.isInstalled()) {
            termux.install();
        }
    }

    public void btn3(View view) {
        editText = findViewById(R.id.edit);
        String url = editText.getText().toString().trim();
        termux.getSession().write(Termux.PARSE_YOUTUBE + url + "\n");
    }

    public void btn4(View view) {
        textView.setText("");
    }
}
