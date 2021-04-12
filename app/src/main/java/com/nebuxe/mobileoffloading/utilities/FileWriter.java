package com.nebuxe.mobileoffloading.utilities;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class FileWriter {

    public static void writeText(Context context, String filename, boolean append, String text) {
        File path = context.getFilesDir();
        File file = new File(path, filename);

        try {
            text += "\n";
            java.io.FileWriter fileWriter = new java.io.FileWriter(file, append);
            fileWriter.write(text);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeMatrix(Context context, String filename, int[][] matrix) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                stringBuilder.append(matrix[i][j] + "\t");
            }
            stringBuilder.append("\n");
        }

        FileWriter.writeText(context, filename, false, stringBuilder.toString());
    }
}
