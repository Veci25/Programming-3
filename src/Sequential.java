import java.util.Random;
import java.util.Scanner;

public class Sequential{

    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the size of the matrix NxN: ");
        int n = sc.nextInt();
        int[][] matrix_A = createMatrix(n);

        System.out.println("Matrix A: ");
        printMatrix(matrix_A, n, n);

        int[][] matrix_B = createMatrix(n);
        System.out.println("Matrix B: ");
        printMatrix(matrix_B, n, n);

        int[][] result_matrix =  multiply(matrix_A, matrix_B);

        System.out.println("Result Matrix: ");
        printMatrix(result_matrix, n, n);



    }

    /** Method to create a matrix with random integers **/
    public static int[][] createMatrix(int n){
        int[][] res = new int[n][n];
        Random rand = new Random();
        for (int i=0; i<n; i++){
            for (int j=0; j<n; j++){
                res[i][j] = rand.nextInt(10);
            }
        }
        return res;
    }

    public static int[][] seqMM(int[][] a, int[][] b){
        int n = a.length;
        int[][] res = new int[n][n];
        for (int i = 0; i<n; i++){
            for (int j=0; j<n; j++){
                res[i][j] = 0;
                for (int k=0; k<n; k++){
                    res[i][j] += a[i][k]*b[k][j];
                }
            }
        }
        return res;
    }

    /** Method to print the matrix **/
    public static void printMatrix(int[][] A, int row, int col){
        for (int i = 0; i < row; i++){
            for (int j = 0; j < col; j++){
                System.out.print(" " + A[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public static int[][] multiply(int[][] A, int[][] B){
        int n = A.length;
        int[][] C = new int[n][n];

        /** If the matrix is 1x1 just directly multiply it with the other one **/
        if (n <= 32){
            return seqMM(A, B);
        }else{

            /** Createing subamtrcies to get 2x2 matrices because we use divide and conquer **/
            int[][] A11 = new int[n/2][n/2];
            int[][] A12 = new int[n/2][n/2];
            int[][] A21 = new int[n/2][n/2];
            int[][] A22 = new int[n/2][n/2];
            int[][] B11 = new int[n/2][n/2];
            int[][] B12 = new int[n/2][n/2];
            int[][] B21 = new int[n/2][n/2];
            int[][] B22 = new int[n/2][n/2];

            /** Split the matrix **/
            split(A, A11, 0, 0);
            split(A, A12, 0, n/2);
            split(A, A21, n/2, 0);
            split(A, A22, n/2, n/2);

            split(B, B11, 0, 0);
            split(B, B12, 0, n/2);
            split(B, B21, n/2, 0);
            split(B, B22, n/2, n/2);

            /** Strassen's algorithm formulas for matrix multiplication **/
            int[][] M1 = multiply(add(A11, A22), add(B11, B22));
            int[][] M2 = multiply(add(A21, A22), B11);
            int[][] M3 = multiply(A11, subtract(B12, B22));
            int[][] M4 = multiply(A22, subtract(B21, B11));
            int[][] M5 = multiply(add(A11, A12), B22);
            int[][] M6 = multiply(subtract(A21, A11), add(B11, B12));
            int[][] M7 = multiply(subtract(A12, A22), add(B21, B22));

            int[][] C11 = add(subtract(add(M1, M4), M5), M7);
            int[][] C12 = add(M3, M5);
            int[][] C21 = add(M2, M4);
            int[][] C22 = add(subtract(add(M1, M3), M2), M6);

            /** Joining back the submatrices into one matrix **/
            join(C11, C, 0, 0);
            join(C12, C, 0, n / 2);
            join(C21, C, n / 2, 0);
            join(C22, C, n / 2, n / 2);
        }
        return C;
    }

    /** Function to split parent matrix into child matrices, because we use the divide-and-conquer approach **/
    public static void split(int[][] P, int[][] C, int iB, int jB) {
        for (int i = iB; i < C.length + iB; i++)
            for (int j = jB; j < C.length + jB; j++)
                C[i - iB][j - jB] = P[i][j];
    }


    /** Function to join child matrices into parent matrix after the splitting **/
    public static void join(int[][] C, int[][] P, int iB, int jB) {
        for (int i = iB; i < C.length + iB; i++) {
            for (int j = jB; j < C.length + jB; j++) {
                P[i][j] = C[i - iB][j - jB];
            }
        }
    }


    /** Method for subtracting two matrices **/
    public static int[][] subtract(int[][] A, int [][]B){
        int n = A.length;
        int[][] C = new int[n][n];

        for (int i = 0; i < n; i++){
            for (int j = 0; j < n; j++){
                C[i][j] = A[i][j] - B[i][j];
            }
        }
        return C;
    }

    /** Method for adding two matrices **/
    public static int[][] add(int[][] A, int[][] B){
        int n = A.length;
        int[][] C = new int[n][n];

        for (int i = 0; i < n; i++){
            for (int j = 0; j < n; j++){
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }
}