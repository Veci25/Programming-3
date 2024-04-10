import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Parallel {
    public static void main(String[] args) {
        // Example matrices
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the size of the matrix NxN: ");
        int n = sc.nextInt();
        int[][] matrix_A = createMatrix(n);

        System.out.println("Matrix A: ");
        printMatrix(matrix_A, n, n);

        int[][] matrix_B = createMatrix(n);
        System.out.println("Matrix B: ");
        printMatrix(matrix_B, n, n);

        // Create a ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool();

        // Create a StrassensParallel task with the matrices A and B
        StrassensParallel task = new StrassensParallel(matrix_A, matrix_B);

        // Submit the task to the ForkJoinPool
        int[][] result = forkJoinPool.invoke(task);

        // Print the result
        System.out.println("Result:");
        printMatrix(result, n, n);
    }

    public static void printMatrix(int[][] A, int row, int col) {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                System.out.print(" " + A[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    /** Method to create a matrix with random integers **/
    public static int[][] createMatrix(int n){
        int[][] res = new int[n][n];
        Random rand = new Random();
        for (int i = 0; i < n; i++){
            for (int j = 0; j < n; j++){
                res[i][j] = rand.nextInt(10);
            }
        }
        return res;
    }


    public static int[][] naiveWay(int[][] matA, int[][] matB) {
        int n = matA.length;
        int[][] prod = new int[n][n];

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                final int row = i;
                final int col = j;
                executor.submit(() -> {
                    int sum = 0;
                    for (int k = 0; k < n; k++) {
                        sum += matA[row][k] * matB[k][col];
                    }
                    prod[row][col] = sum;
                });
            }
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            // Wait for all tasks to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return prod;
    }
}
class StrassensParallel extends RecursiveTask<int[][]> {
        private int[][] A;
        private int[][] B;

        public StrassensParallel(int[][] A, int[][] B){
            this.A = A;
            this.B = B;
        }

        @Override
        protected int[][] compute() {
            return multiply(A,B);
        }

            public static int[][] multiply(int[][] A, int[][] B){
                int n = A.length;
                int[][] C = new int[n][n];

                /** If the matrix is 1x1 just directly multiply it with the other one **/
                if (n <= 32)
                {
                    return Parallel.naiveWay(A, B);
                }

                int[][] A11 = new int[n / 2][n / 2];
                int[][] A12 = new int[n / 2][n / 2];
                int[][] A21 = new int[n / 2][n / 2];
                int[][] A22 = new int[n / 2][n / 2];
                int[][] B11 = new int[n / 2][n / 2];
                int[][] B12 = new int[n / 2][n / 2];
                int[][] B21 = new int[n / 2][n / 2];
                int[][] B22 = new int[n / 2][n / 2];


                split(A, A11, 0, 0);
                split(A, A12, 0, n / 2);
                split(A, A21, n / 2, 0);
                split(A, A22, n / 2, n / 2);

                split(B, B11, 0, 0);
                split(B, B12, 0, n / 2);
                split(B, B21, n / 2, 0);
                split(B, B22, n / 2, n / 2);

                int[][] s1 = multiply(add(A11, A22), add(B11, B22));
                int[][] s2 = multiply(add(A21, A22), B11);
                int[][] s3 = multiply(A11, subtract(B12, B22));
                int[][] s4 = multiply(A22, subtract(B21, B11));
                int[][] s5 = multiply(add(A11, A12), B22);
                int[][] s6 = multiply(subtract(A21, A11), add(B11, B12));
                int[][] s7 = multiply(subtract(A12, A22), add(B21, B22));

                int[][] c11 = add(subtract(add(s1, s4), s5), s7);
                int[][] c12 = add(s3, s5);
                int[][] c21 = add(s2, s4);
                int[][] c22 = add(subtract(add(s1, s3), s2), s6);

                join(C, c11, 0, 0);
                join(C, c12, 0, n / 2);
                join(C, c21, n / 2, 0);
                join(C, c22, n / 2, n / 2);

                return C;
            }




        public static void split(int[][] P, int[][] C, int iB, int jB) {
            for (int i = iB; i < C.length + iB; i++)
                for (int j = jB; j < C.length + jB; j++)
                    C[i - iB][j - jB] = P[i][j];
        }


        /** Function to join child matrices into parent matrix after the splitting **/
        public static void join(int[][] C, int[][] P, int iB, int jB) {
            int n = P.length;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    C[i + iB][j + jB] = P[i][j];
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

