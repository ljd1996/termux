package com.termux.app;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.UserManager;
import android.system.Os;
import android.util.Log;
import android.util.Pair;

import com.termux.Termux;
import com.termux.TermuxListener;
import com.termux.terminal.TermuxDebug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Install the TermuxHelper bootstrap packages if necessary by following the below steps:
 * <p/>
 * (1) If $PREFIX already exist, assume that it is correct and be done. Note that this relies on that we do not create a
 * broken $PREFIX folder below.
 * <p/>
 * (2) A progress dialog is shown with "Installing..." message and a spinner.
 * <p/>
 * (3) A staging folder, $STAGING_PREFIX, is {@link #deleteFolder(File)} if left over from broken installation below.
 * <p/>
 * (4) The architecture is determined and an appropriate bootstrap zip url is determined in {@link #determineZipUrl()}.
 * <p/>
 * (5) The zip, containing entries relative to the $PREFIX, is is downloaded and extracted by a zip input stream
 * continuously encountering zip file entries:
 * <p/>
 * (5.1) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (5.2) For every other zip entry, extract it into $STAGING_PREFIX and set execute permissions if necessary.
 */
public final class TermuxInstaller {

    private File mUsrFile;
    private TermuxListener mListener;

    public TermuxInstaller() {
        mUsrFile = new File(Termux.PREFIX_PATH);
    }

    public void setListener(TermuxListener listener) {
        this.mListener = listener;
    }

    public boolean isSetup() {
        return mUsrFile.exists() && mUsrFile.isDirectory();
    }

    public void setupIfNeeded(Context context) {
        if (context == null) {
            return;
        }
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (um == null) {
            if (mListener != null) {
                mListener.execute(null, false);
            }
            return;
        }
        boolean isPrimaryUser = um.getSerialNumberForUser(android.os.Process.myUserHandle()) == 0;
        if (!isPrimaryUser) {
            if (mListener != null) {
                mListener.execute(null, false);
            }
            return;
        }

        if (mUsrFile.isDirectory()) {
            if (mListener != null) {
                mListener.execute(null, true);
            }
            return;
        }

        if (mUsrFile.exists()) {
            mUsrFile.delete();
        }

        setupEnv();
    }

    private void setupEnv() {
        // download bootstraps.zip
        boolean success = true;
        if (success) {
            // download success
            File file = new File("/sdcard/bootstrap-aarch64.zip");
            try {
                downloadSuccess(file);
            } catch (Exception e) {
                e.printStackTrace();
                if (mListener != null) {
                    mListener.execute(null, false);
                }
            }
            if (mListener != null) {
                mListener.execute(null, true);
            }
        } else {
            // download fail
            if (mListener != null) {
                mListener.execute(null, false);
            }
        }
    }

    private void downloadSuccess(File file) throws Exception {
        final byte[] buffer = new byte[8096];
        final List<Pair<String, String>> symlinks = new ArrayList<>(50);
        final File usrStaging = new File(Termux.STAGING_PREFIX_PATH);

        if (usrStaging.exists()) {
            deleteFolder(usrStaging);
        }

        ZipInputStream zipInput = new ZipInputStream(new FileInputStream(file));
        ZipEntry zipEntry;
        while ((zipEntry = zipInput.getNextEntry()) != null) {
            if (zipEntry.getName().equals("SYMLINKS.txt")) {
                BufferedReader symlinksReader = new BufferedReader(new InputStreamReader(zipInput));
                String line;
                while ((line = symlinksReader.readLine()) != null) {
                    String[] parts = line.split("←");
                    if (parts.length != 2) {
                        throw new RuntimeException("Malformed symlink line: " + line);
                    }
                    String oldPath = parts[0];
                    String newPath = Termux.STAGING_PREFIX_PATH + "/" + parts[1];
                    symlinks.add(Pair.create(oldPath, newPath));

                    ensureDirectoryExists(new File(newPath).getParentFile());
                }
            } else {
                String zipEntryName = zipEntry.getName();
                File targetFile = new File(Termux.STAGING_PREFIX_PATH, zipEntryName);
                boolean isDirectory = zipEntry.isDirectory();

                ensureDirectoryExists(isDirectory ? targetFile : targetFile.getParentFile());

                if (!isDirectory) {
                    try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                        int readBytes;
                        while ((readBytes = zipInput.read(buffer)) != -1)
                            outStream.write(buffer, 0, readBytes);
                    }
                    if (zipEntryName.startsWith("bin/") || zipEntryName.startsWith("libexec") || zipEntryName.startsWith("lib/apt/methods")) {
                        //noinspection OctalInteger
                        Os.chmod(targetFile.getAbsolutePath(), 0700);
                    }
                }
            }
        }

        if (symlinks.isEmpty()) {
            throw new RuntimeException("No SYMLINKS.txt encountered");
        }
        for (Pair<String, String> symlink : symlinks) {
            Os.symlink(symlink.first, symlink.second);
        }

        if (!usrStaging.renameTo(mUsrFile)) {
            throw new RuntimeException("Unable to rename staging folder");
        }
    }

    private void ensureDirectoryExists(File directory) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new RuntimeException("Unable to create directory: " + directory.getAbsolutePath());
        }
    }

    /**
     * Get bootstrap zip url for this systems cpu architecture.
     */
    private String determineZipUrl() {
        String archName = determineTermuxArchName();
        Log.d(TermuxDebug.TAG, "archName = " + archName);
        Log.d(TermuxDebug.TAG, "sdk version = " + Build.VERSION.SDK_INT);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? "https://test-vb-apt.s3.ap-south-1.amazonaws.com/bootstraps/android-7/bootstrap-" + archName + ".zip"
                : "https://test-vb-apt.s3.ap-south-1.amazonaws.com/bootstraps/android-5/bootstrap-" + archName + ".zip";
    }

    private String determineTermuxArchName() {
        // Note that we cannot use System.getProperty("os.arch") since that may give e.g. "aarch64"
        // while a 64-bit runtime may not be installed (like on the Samsung Galaxy S5 Neo).
        // Instead we search through the supported abi:s on the device, see:
        // http://developer.android.com/ndk/guides/abis.html
        // Note that we search for abi:s in preferred order (the ordering of the
        // Build.SUPPORTED_ABIS list) to avoid e.g. installing arm on an x86 system where arm
        // emulation is available.
        for (String androidArch : Build.SUPPORTED_ABIS) {
            switch (androidArch) {
                case "arm64-v8a":
                    return "aarch64";
                case "armeabi-v7a":
                    return "arm";
                case "x86_64":
                    return "x86_64";
                case "x86":
                    return "i686";
            }
        }
        throw new RuntimeException("Unable to determine arch from Build.SUPPORTED_ABIS =  " +
                Arrays.toString(Build.SUPPORTED_ABIS));
    }

    /**
     * Delete a folder and all its content or throw. Don't follow symlinks.
     */
    private void deleteFolder(File fileOrDirectory) throws IOException {
        if (fileOrDirectory.getCanonicalPath().equals(fileOrDirectory.getAbsolutePath()) && fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteFolder(child);
                }
            }
        }

        if (!fileOrDirectory.delete()) {
            throw new RuntimeException("Unable to delete " + (fileOrDirectory.isDirectory() ? "directory " : "file ") + fileOrDirectory.getAbsolutePath());
        }
    }
}
