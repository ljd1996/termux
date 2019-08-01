package com.termux;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

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
public final class TermuxActivity extends Activity {

    private static final int REQUESTCODE_PERMISSION_STORAGE = 1234;

    private EditText editText;

    private Termux termux;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.drawer_layout);

        termux = Termux.mInstance.init(this, new Termux.TermuxHandle() {
            @Override
            public void success() {
                Log.d("LLL", "success");
            }

            @Override
            public void initFail() {
                Log.d("LLL", "initFail");
            }

            @Override
            public void installFail() {
                Log.d("LLL", "installFail");
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
}
