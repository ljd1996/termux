package com.termux.app;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @Date: 2024/3/7
 */
public class AssetCopy {

    private final AssetManager mAssetManager;

    public AssetCopy(Context context) {
        mAssetManager = context.getAssets();
    }

    /**
     * 执行拷贝任务
     *
     * @param asset 需要拷贝的assets文件路径
     * @return 拷贝成功后的目标文件句柄
     */
    public File copy(String asset, String dest) throws IOException {

        InputStream source = mAssetManager.open(asset);
        File destinationFile = new File(dest, asset);

        if (destinationFile.exists()) {
            return destinationFile;
        }

        destinationFile.getParentFile().mkdirs();
        OutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[1024];
        int nread;

        while ((nread = source.read(buffer)) != -1) {
            if (nread == 0) {
                nread = source.read();
                if (nread < 0)
                    break;
                destination.write(nread);
                continue;
            }
            destination.write(buffer, 0, nread);
        }
        destination.close();

        return destinationFile;
    }
}
