package com.termux.util;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author liujiadong
 * @since 2019/10/16
 */
public class FileUtil {

    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getDlFileName(String url) {
        return Termux.HOME_PATH + File.separator + FileUtil.md5(url + System.currentTimeMillis());
    }

    public static String readFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            return "";
        }
        FileInputStream is;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            if (file.length() != 0) {
                is = new FileInputStream(file);
                InputStreamReader streamReader = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(streamReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return String.valueOf(stringBuilder);
    }

    public static boolean usrExist() {
        File usrFile = new File(Termux.PREFIX_PATH);
        return usrFile.exists() && usrFile.isDirectory();
    }
}
