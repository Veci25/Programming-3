import java.util.Random;
import mpi.*;

public class Distributive {
    public static void main(String[] args) {
        MPI.Init(args); // Initialize the MPI environment
        int p_rank = MPI.COMM_WORLD.Rank(); // Get the rank of the process


        int[] n = new int[1]; // Array to store the matrix size
        if (args.length < 1) { // Check if matrix size is provided as argument
            if (p_rank == 0) { // Only process 0 prints the message
                System.out.println("p_rank is 0");
            }
            MPI.Finalize(); // Finalize MPI environment
            return;
        }

        if (p_rank == 0) { // Process 0 reads the matrix size from arguments
            try {
                n[0] = Integer.parseInt(args[3]); // Parse the matrix size
                System.out.println("Matrix size: " + n[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid matrix size argument: " + args[0]);
                MPI.Finalize(); // Finalize MPI environment
                return;
            }
        }

        MPI.COMM_WORLD.Barrier(); // Synchronize all processes
        MPI.COMM_WORLD.Bcast(n, 0, 1, MPI.INT, 0); // Broadcast matrix size to all processes

        int[][] A = allocateMatrix(n[0]); // Allocate memory for matrix A
        int[][] B = allocateMatrix(n[0]); // Allocate memory for matrix B

        if (p_rank == 0) { // Process 0 creates and prints matrices
            A = createMatrix(n[0]);
            System.out.println("Matrix A: ");
            printMatrix(A, A.length, A.length);
            B = createMatrix(n[0]);
            System.out.println("Matrix B: ");
            printMatrix(B, B.length, B.length);
        }

        /* The Aprim and Bprim arrays are allocated with a size of n[0] * n[0], where n[0] is the dimension of the NxN matrix.
        This ensures that the one-dimensional arrays have enough space to hold all elements of the two-dimensional matrices.*/
        int[] Aprim = new int[n[0] * n[0]]; // Flattened matrix A
        int[] Bprim = new int[n[0] * n[0]]; // Flattened matrix B
        if (p_rank == 0) { // Process 0 flattens the matrices
            Aprim = Distributive.StraightenMatrix(A);
            Bprim = Distributive.StraightenMatrix(B);
        }

        /* Broadcast the data in chunks(chunks are segments of the flattened matrix that are broadcasted individually
         to manage memory usage and network bandwidth effectively)*/
        int chunkSize = 1000000; // Chunk size for broadcasting
        for (int i = 0; i < n[0] * n[0]; i += chunkSize) {
            int size = Math.min(chunkSize, n[0] * n[0] - i);
            MPI.COMM_WORLD.Bcast(Aprim, i, size, MPI.INT, 0);
            MPI.COMM_WORLD.Bcast(Bprim, i, size, MPI.INT, 0);
        }

        // Convert flattened matrices back to 2D matrices
        A = Distributive.StraightMatrixTo2D(Aprim, n[0]);
        B = Distributive.StraightMatrixTo2D(Bprim, n[0]);

        double startTime = MPI.Wtime();

        multiply(A, B, p_rank, n[0]);

        double endTime = MPI.Wtime();

        double total_time = (endTime - startTime) * 1000;

        if (p_rank == 0) { // Process 0 prints the runtime
            System.out.println("Distributive runtime: " + total_time + "ms");
        }

        MPI.Finalize();
    }

    // Allocate memory for a square matrix of size n
    public static int[][] allocateMatrix(int n) {
        return new int[n][n];
    }

    // Flatten a 2D matrix into a 1D array
    public static int[] StraightenMatrix(int[][] matrix) {
        int[] straightMatrix = new int[matrix.length * matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                straightMatrix[i * matrix.length + j] = matrix[i][j];
            }
        }
        return straightMatrix;
    }

    // Convert a 1D array back to a 2D matrix
    public static int[][] StraightMatrixTo2D(int[] matrix, int n) {
        int[][] twoDMatrix = new int[n][n];
        for (int i = 0; i < matrix.length; i++) {
            twoDMatrix[i / n][i % n] = matrix[i];
        }
        return twoDMatrix;
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

    public static int[][] seqMM(int[][] a, int[][] b) {
        int n = a.length;
        int[][] res = allocateMatrix(n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                res[i][j] = 0;
                for (int k = 0; k < n; k++) {
                    res[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return res;
    }

    public static void split(int[][] P, int[][] C, int iB, int jB) {
        int n = C.length;
        for (int i = iB; i < n + iB; i++) {
            for (int j = jB; j < n + jB; j++) {
                C[i - iB][j - jB] = P[i][j];
            }
        }
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

  public static void join(int[][] C, int[][] P, int iB, int jB) {
        for (int i = iB; i < C.length + iB; i++) {
            for (int j = jB; j < C.length + jB; j++) {
                P[i][j] = C[i - iB][j - jB];
            }
        }
    }

    public static int[][] subtract(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = allocateMatrix(n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] - B[i][j];
            }
        }
        return C;
    }

    public static int[][] add(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = allocateMatrix(n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }

    // Sequential matrix multiplication
    public static int[][] multiply(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        if (n <= 32) {
            return seqMM(A, B);
        } else {
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

            join(C11, C, 0, 0);
            join(C12, C, 0, n / 2);
            join(C21, C, n / 2, 0);
            join(C22, C, n / 2, n / 2);
        }
        return C;
    }

    // Parallel matrix multiplication using MPI
    public static void multiply(int[][] A, int[][] B, int rank, int m1) {
        int n = A.length;
        int m = m1 / 2;

        int[][] A11 = new int[m][m];
        int[][] A12 = new int[m][m];
        int[][] A21 = new int[m][m];
        int[][] A22 = new int[m][m];
        int[][] B11 = new int[m][m];
        int[][] B12 = new int[m][m];
        int[][] B21 = new int[m][m];
        int[][] B22 = new int[m][m];

        split(A, A11, 0, 0);
        split(A, A12, 0, m);
        split(A, A21, m, 0);
        split(A, A22, m, m);
        split(B, B11, 0, 0);
        split(B, B12, 0, m);
        split(B, B21, m, 0);
        split(B, B22, m, m);

        // Allocate memory for the results of the asynchronous computations
        int[][] s1 = allocateMatrix(m);
        int[][] s2 = allocateMatrix(m);
        int[][] s3 = allocateMatrix(m);
        int[][] s4 = allocateMatrix(m);
        int[][] s5 = allocateMatrix(m);
        int[][] s6 = allocateMatrix(m);
        int[][] s7 = allocateMatrix(m);

        // Process 0 receives results from other processes
        if (rank == 0) {
            // Flattened matrices to store the results from other processes
            int[] a = new int[m * m];
            int[] a2 = new int[m * m];
            int[] a3 = new int[m * m];
            int[] a4 = new int[m * m];
            int[] a5 = new int[m * m];
            int[] a6 = new int[m * m];
            int[] a7 = new int[m * m];
            // Receive the results from other processes
            MPI.COMM_WORLD.Recv(a, 0, m * m, MPI.INT, 1, 0);
            MPI.COMM_WORLD.Recv(a2, 0, m * m, MPI.INT, 2, 0);
            MPI.COMM_WORLD.Recv(a3, 0, m * m, MPI.INT, 3, 0);
            MPI.COMM_WORLD.Recv(a4, 0, m * m, MPI.INT, 4, 0);
            MPI.COMM_WORLD.Recv(a5, 0, m * m, MPI.INT, 5, 0);
            MPI.COMM_WORLD.Recv(a6, 0, m * m, MPI.INT, 6, 0);
            MPI.COMM_WORLD.Recv(a7, 0, m * m, MPI.INT, 7, 0);
            // Convert the flattened matrices back to 2D matrices
            s1 = Distributive.StraightMatrixTo2D(a, m);
            s2 = Distributive.StraightMatrixTo2D(a2, m);
            s3 = Distributive.StraightMatrixTo2D(a3, m);
            s4 = Distributive.StraightMatrixTo2D(a4, m);
            s5 = Distributive.StraightMatrixTo2D(a5, m);
            s6 = Distributive.StraightMatrixTo2D(a6, m);
            s7 = Distributive.StraightMatrixTo2D(a7, m);
            // Other processes perform their part of the computation
        } else if (rank == 1) {
            // Process 1 computes the first part of the result
            int[][] M1 = multiply(add(A11, A22), add(B11, B22));
            s1 = M1;
            // Flatten the matrix to send it to process 0
            int[] copyOfMatrix = StraightenMatrix(s1);
            MPI.COMM_WORLD.Send(copyOfMatrix, 0, m * m, MPI.INT, 0, 0);
        } else if (rank == 2) {
            // Process 2 computes the second part of the result
            int[][] M2 = multiply(add(A21, A22), B11);
            s2 = M2;
            int[] copyOfMatrix = StraightenMatrix(s2);
            MPI.COMM_WORLD.Send(copyOfMatrix, 0, m * m, MPI.INT, 0, 0);
        } else if (rank == 3) {
            // Process 3 computes the third part of the result
            int[][] M3 = multiply(A11, subtract(B12, B22));
            s3 = M3;
            int[] copyOfMatrix = StraightenMatrix(s3);
            MPI.COMM_WORLD.Send(copyOfMatrix, 0, m * m, MPI.INT, 0, 0);
        } else if (rank == 4) {
            // Process 4 computes the fourth part of the result
            int[][] M4 = multiply(A22, subtract(B21, B11));
            s4 = M4;
            int[] copyOfMatrix = StraightenMatrix(s4);
            MPI.COMM_WORLD.Send(copyOfMatrix, 0, m * m, MPI.INT, 0, 0);
        } else if (rank == 5) {
            // Process 5 computes the fifth part of the result
            int[][] M5 = multiply(add(A11, A12), B22);
            s5 = M5;
            int[] copyOfMatrix = StraightenMatrix(s5);
            MPI.COMM_WORLD.Send(copyOfMatrix, 0, m * m, MPI.INT, 0, 0);
        } else if (rank == 6) {
            // Process 6 computes the sixth part of the result
            int[][] M6 = multiply(subtract(A21, A11), add(B11, B12));
            s6 = M6;
            int[] copyOfMatrix = StraightenMatrix(s6);
            MPI.COMM_WORLD.Send(copyOfMatrix, 0, m * m, MPI.INT, 0, 0);
        } else if (rank == 7) {
            // Process 7 computes the last part of the result
            int[][] M7 = multiply(subtract(A12, A22), add(B21, B22));
            s7 = M7;
            int[] copyOfMatrix = StraightenMatrix(s7);
            MPI.COMM_WORLD.Send(copyOfMatrix, 0, m * m, MPI.INT, 0, 0);
        }

        // Synchronize all processes
        MPI.COMM_WORLD.Barrier();

        // Process 0 combines results from other processes
        if (rank == 0) {
            // Combine the results of the asynchronous computations
            int[][] C = allocateMatrix(n);
            int[][] C11 = add(subtract(add(s1, s4), s5), s7);
            int[][] C12 = add(s3, s5);
            int[][] C21 = add(s2, s4);
            int[][] C22 = add(subtract(add(s1, s3), s2), s6);
            // Join the submatrices into the final result matrix
            join(C11, C, 0, 0);
            join(C12, C, 0, n / 2);
            join(C21, C, n / 2, 0);
            join(C22, C, n / 2, n / 2);
            System.out.println("Result Matrix: ");
            printMatrix(C, C.length, C.length);
        }
    }
}
