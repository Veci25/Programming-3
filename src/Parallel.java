import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Parallel {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the size of the matrix NxN: ");
        int n = sc.nextInt();
        int[][] matrix_A = createMatrix(n);
        System.out.println("Matrix A: ");
        printMatrix(matrix_A, n, n);

        int[][] matrix_B = createMatrix(n);
        System.out.println("Matrix B: ");
        printMatrix(matrix_B, n, n);

        long startTime = System.currentTimeMillis();

        int[][] result = multiply(matrix_A, matrix_B);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Result:");
        printMatrix(result, n, n);
        System.out.println("Time taken to multiply matrices: " + duration + " milliseconds");
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

    public static int[][] createMatrix(int n) {
        int[][] res = new int[n][n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                res[i][j] = rand.nextInt(10);
            }
        }
        return res;
    }

    /* This method is responsible for creating and managing the lifecycle of the ExecutorService.
       This is used to call the multiplyAsync method which performs the matrix multiplication asynchronously,
       it initializes an ExecutorService to manage a pool of threads that will execute the matrix
       multiplication tasks in parallel. The multiplyAsync method uses CompletableFuture to execute
       parts of the matrix multiplication in parallel.
       The multiply method waits for the result of the asynchronous computation using the join() method on the
       CompletableFuture returned by multiplyAsync. */
    public static int[][] multiply(int[][] A, int[][] B) {
        // Create an ExecutorService to manage a pool of threads
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        // Perform matrix multiplication asynchronously using the executor
        CompletableFuture<int[][]> resultFuture = multiplyAsync(A, B, executor);
        // Wait for the result of the asynchronous computation
        int[][] result = resultFuture.join();
        // Shutdown the executor to free up resources
        executor.shutdown();
        // Return the resulting matrix
        return result;
    }

    // If the matrix is small, perform naive multiplication asynchronously
    public static CompletableFuture<int[][]> multiplyAsync(int[][] A, int[][] B, ExecutorService executor) {
        // Get the size of the matrix
        int n = A.length;
        int[][] C = new int[n][n];

        /* If the matrix size is smaller than 32 we use the normal matrix multiplication because the strassen's algorithm is
         * more efficient for larger matrices */
        if (n <= 32) {
            // Perform matrix multiplication using the naive approach
            return CompletableFuture.supplyAsync(() -> naiveWay(A, B), executor);
        }

        // Initialize submatrices for divide and conquer
        int[][] A11 = new int[n / 2][n / 2];
        int[][] A12 = new int[n / 2][n / 2];
        int[][] A21 = new int[n / 2][n / 2];
        int[][] A22 = new int[n / 2][n / 2];
        int[][] B11 = new int[n / 2][n / 2];
        int[][] B12 = new int[n / 2][n / 2];
        int[][] B21 = new int[n / 2][n / 2];
        int[][] B22 = new int[n / 2][n / 2];

        // Split the matrices A and B into 4 submatrices each
        split(A, A11, 0, 0);
        split(A, A12, 0, n / 2);
        split(A, A21, n / 2, 0);
        split(A, A22, n / 2, n / 2);
        split(B, B11, 0, 0);
        split(B, B12, 0, n / 2);
        split(B, B21, n / 2, 0);
        split(B, B22, n / 2, n / 2);

        // Perform matrix multiplications in parallel using CompletableFuture
        CompletableFuture<int[][]> s1 = multiplyAsync(add(A11, A22), add(B11, B22), executor);
        CompletableFuture<int[][]> s2 = multiplyAsync(add(A21, A22), B11, executor);
        CompletableFuture<int[][]> s3 = multiplyAsync(A11, subtract(B12, B22), executor);
        CompletableFuture<int[][]> s4 = multiplyAsync(A22, subtract(B21, B11), executor);
        CompletableFuture<int[][]> s5 = multiplyAsync(add(A11, A12), B22, executor);
        CompletableFuture<int[][]> s6 = multiplyAsync(subtract(A21, A11), add(B11, B12), executor);
        CompletableFuture<int[][]> s7 = multiplyAsync(subtract(A12, A22), add(B21, B22), executor);

        // Combine the results of the asynchronous computations
        return s1.thenCombine(s2, (r1, r2) -> {
            int[][] c11 = add(subtract(add(r1, s4.join()), s5.join()), s7.join());
            int[][] c12 = add(s3.join(), s5.join());
            int[][] c21 = add(s2.join(), s4.join());
            int[][] c22 = add(subtract(add(r1, s3.join()), s2.join()), s6.join());

            // Join the submatrices into the final result matrix
            join(C, c11, 0, 0);
            join(C, c12, 0, n / 2);
            join(C, c21, n / 2, 0);
            join(C, c22, n / 2, n / 2);

            return C;
        });
    }

    public static void split(int[][] P, int[][] C, int iB, int jB) {
        for (int i = iB; i < C.length + iB; i++) {
            for (int j = jB; j < C.length + jB; j++) {
                C[i - iB][j - jB] = P[i][j];
            }
        }
    }

   public static void join(int[][] C, int[][] P, int iB, int jB) {
        for (int i = iB; i < C.length + iB; i++) {
            for (int j = jB; j < C.length + jB; j++) {
                P[i][j] = C[i - iB][j - jB];
            }
        }
    }

    public static int[][] subtract(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
        return C;
    }

    public static int[][] add(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }

    public static int[][] naiveWay(int[][] matA, int[][] matB) {
        int n = matA.length;
        int[][] prod = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += matA[i][k] * matB[k][j];
                }
                prod[i][j] = sum;
            }
        }
        return prod;
    }
}
