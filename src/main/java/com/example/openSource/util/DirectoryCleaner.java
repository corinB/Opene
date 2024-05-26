package com.example.openSource.util;

import java.io.File;

public class DirectoryCleaner {

    public static void cleanDirectory(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
    }
}

