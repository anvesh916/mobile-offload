package com.nebuxe.mobileoffloading.utilities;

public class Matrix {

    public static int[][] buildMatrix(int rows, int cols) {
        int[][] matrix = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
//                matrix[i][j] = (int) (Math.random() * 100);
                matrix[i][j] = (i + j) % 128;
            }
        }

        return matrix;
    }

    public static int[][] transpose(int[][] matrix) {
        int[][] transpose = new int[matrix[0].length][matrix.length];
        for (int row = 0; row < matrix.length; row++) {
            for (int col = 0; col < matrix[0].length; col++) {
                transpose[col][row] = matrix[row][col];
            }
        }

        return transpose;
    }

    public static int dotProduct(int[] vector1, int[] vector2) {
        int res = 0;
        for (int i = 0; i < vector1.length; i++) {
            res += vector1[i] * vector2[i];
        }

        return res;
    }
}
