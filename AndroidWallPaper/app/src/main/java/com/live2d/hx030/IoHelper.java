package com.live2d.hx030;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IoHelper {
    public static void unzip(String zipPath, String outputPath) throws IOException {
        File zip = new File(zipPath);
        if (!zip.exists()) {
            throw new IOException("File not found");
        }
        unzip(new ZipInputStream(new FileInputStream(zip)), outputPath);
    }
    public static String unzip(ZipInputStream zis, String outputPath) throws IOException {
        String ret = "undefined";
        byte[] buffer = new byte[1024];
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            // HACK: workaround for model filename
            String currentFilename = zipEntry.getName();
            if (currentFilename.endsWith(".model3.json")) {
//                currentFilename = "custom_model.model3.json";
                ret = currentFilename.replace(".model3.json", "");
            }

            File currentOutput = new File(outputPath, currentFilename);
            if (zipEntry.isDirectory()) {
                if (!currentOutput.isDirectory() && !currentOutput.mkdirs()) {
                    throw new IOException("Failed to create directory " + currentOutput);
                }
            } else {
                File parent = currentOutput.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                FileOutputStream fos = new FileOutputStream(currentOutput);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        return ret;
    }

    public static boolean delete(File target) {
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children == null) return false;
            for (File child : children)
                delete(child);
        }

        return target.delete();
    }
}
