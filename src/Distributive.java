import java.util.Random;
import java.util.Scanner;

import mpi.*;
public class Distributive {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        MPI.Init(args);
        int p_rank = MPI.COMM_WORLD.Rank();
        int num_process = MPI.COMM_WORLD.Size();


        int[] n = new int[1];
        if (p_rank == 0) {
            System.out.println("Enter the dimensions of the matrix: ");
            n[0] = 100;
        }
        MPI.COMM_WORLD.Barrier();
        MPI.COMM_WORLD.Bcast(n, 0, 1, MPI.INT , 0);



        int[][] A = allocateMatrix(n[0]);
        int[][] B = allocateMatrix(n[0]);

        if (p_rank == 0){
            A = createMatrix(n[0]);
            B = createMatrix(n[0]);


        }
        int[] ajde1=new int[n[0]*n[0]];
        int[] ajde2=new int[n[0]*n[0]];
        if(p_rank==0){
            ajde1=Distributive.StraightenMatrix(A);
            ajde2=Distributive.StraightenMatrix(B);
        }
        MPI.COMM_WORLD.Bcast(ajde1,0 ,n[0] * n[0], MPI.INT, 0);
        MPI.COMM_WORLD.Bcast(ajde2, 0, n[0] * n[0], MPI.INT, 0);

        A=Distributive.StraightMatrixTo2D(ajde1,n[0]);
        B=Distributive.StraightMatrixTo2D(ajde2,n[0]);

        double startTime = MPI.Wtime();

        multiply(A, B, p_rank,n[0]);

        double endTime = MPI.Wtime();

        if (p_rank == 0){
            System.out.println("Distributive runtime: " + (endTime - startTime));

        }

        MPI.Finalize();

    }
    public static int[][] allocateMatrix(int n) {
        int[][] matrix = new int[n][n];
        return matrix;
    }
    public static int[] StraightenMatrix(int[][] matrix){
        int[] straightMatrix = new int[matrix.length* matrix.length];
        for(int i = 0; i< matrix.length;i++){
            for (int j = 0; j < matrix.length; j++){
                straightMatrix[i* matrix.length+j] = matrix[i][j];
            }
        }
        return straightMatrix;
    }
    public static int[][] StraightMatrixTo2D(int[] matrix,int n){
        int[][] twoDMatrix = new int[n][n];
        for(int i = 0; i< matrix.length;i++){
            twoDMatrix[i/n][i-(i/n)*n]=matrix[i];
        }
        return twoDMatrix;
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

        int[][] res = allocateMatrix(n);

        for (int i = 0; i < n; i++){
            for (int j = 0; j < n; j++){
                res[i][j] = 0;
                for (int k = 0; k < n; k++){
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
    public static void printMatrix(int[][] A, int row, int col){
        for (int i = 0; i < row; i++){
            for (int j = 0; j < col; j++){
                System.out.print(" " + A[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void join(int[][] C, int[][] P, int iB, int jB) {
        int n = C.length;

        for (int i = iB; i < n + iB; i++) {
            for (int j = jB; j < n + jB; j++) {
                P[i][j]= C[i - iB][j - jB];
            }
        }
    }

    /** Method for subtracting two matrices **/
    public static int[][] subtract(int[][] A, int [][]B){
        int n = A.length;
        int[][] C = allocateMatrix(n);

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
        int[][] C = allocateMatrix(n);

        for (int i = 0; i < n; i++){
            for (int j = 0; j < n; j++){
                C[i][j] = A[i][j] + B[i][j];
            }
        }
        return C;
    }

    public static int[][] multiply(int[][] A, int[][] B){
        int n = A.length;
        int[][] C = new int[n][n];

        /** If the matrix is 1x1 just directly multiply it with the other one **/
        if (n<=32){

            return seqMM(A, B);
        }else{
            System.out.println("DA");
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

    public static void multiply(int[][] A, int[][] B, int rank, int m1){
        int n = A.length;
        int[][] C = allocateMatrix(n);

        int m = m1/2;

        if(n == 1){
            C = allocateMatrix(1);
            C[0][0] = A[0][0] * B[0][0];
        }

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

        int[][] s1 = allocateMatrix(m);
        int[][] s2 = allocateMatrix(m);
        int[][] s3 = allocateMatrix(m);
        int[][] s4 = allocateMatrix(m);
        int[][] s5 = allocateMatrix(m);
        int[][] s6 = allocateMatrix(m);
        int[][] s7 = allocateMatrix(m);

        if (rank == 0) {
            int[] a = new int[m*m];
            int[] a2 = new int[m*m];
            int[] a3 = new int[m*m];
            int[] a4 = new int[m*m];
            int[] a5 = new int[m*m];
            int[] a6 = new int[m*m];
            int[] a7 = new int[m*m];
            MPI.COMM_WORLD.Recv(a, 0, m * m, MPI.INT, 1, 0);
            MPI.COMM_WORLD.Recv(a2, 0, m * m, MPI.INT, 2, 0);
            MPI.COMM_WORLD.Recv(a3, 0, m * m, MPI.INT, 3, 0);
            MPI.COMM_WORLD.Recv(a4, 0, m * m, MPI.INT, 4, 0);
            MPI.COMM_WORLD.Recv(a5, 0, m * m, MPI.INT, 5, 0);
            MPI.COMM_WORLD.Recv(a6, 0, m * m, MPI.INT, 6, 0);
            MPI.COMM_WORLD.Recv(a7, 0, m * m, MPI.INT, 7, 0);
            s1 = Distributive.StraightMatrixTo2D(a,m);
            s2 = Distributive.StraightMatrixTo2D(a2,m);
            s3 = Distributive.StraightMatrixTo2D(a3,m);
            s4 = Distributive.StraightMatrixTo2D(a4,m);
            s5 = Distributive.StraightMatrixTo2D(a5,m);
            s6 = Distributive.StraightMatrixTo2D(a6,m);
            s7 = Distributive.StraightMatrixTo2D(a7,m);

        }
        if (rank == 1){
            int[][] M1 = multiply(add(A11, A22), add(B11, B22));
            s1 = M1;
            int[] kopija = StraightenMatrix(s1);
            MPI.COMM_WORLD.Send(kopija,0, m * m, MPI.INT, 0, 0);
        }
        if (rank == 2){
            int[][] M2 = multiply(add(A21, A22), B11);
            s2 = M2;
            int[] kopija = StraightenMatrix(s2);
            MPI.COMM_WORLD.Send(kopija,0, m * m, MPI.INT, 0, 0);
        }
        if (rank == 3){
            int[][] M3 = multiply(A11, subtract(B12, B22));
            s3 = M3;
            int[] kopija = StraightenMatrix(s3);
            MPI.COMM_WORLD.Send(kopija,0, m * m, MPI.INT, 0, 0);
        }
        if (rank == 4){
            int[][] M4 = multiply(A22, subtract(B21, B11));
            s4 = M4;
            int[] kopija = StraightenMatrix(s4);
            MPI.COMM_WORLD.Send(kopija,0, m * m, MPI.INT, 0, 0);
        }
        if (rank == 5){
            int[][] M5 = multiply(add(A11, A12), B22);
            s5 = M5;
            int[] kopija = StraightenMatrix(s5);
            MPI.COMM_WORLD.Send(kopija,0, m * m, MPI.INT, 0, 0);
        }
        if (rank == 6){
            int[][] M6 = multiply(subtract(A21, A11), add(B11, B12));
            s6 = M6;
            int[] kopija = StraightenMatrix(s6);
            MPI.COMM_WORLD.Send(kopija,0, m * m, MPI.INT, 0, 0);
        }
        if (rank == 7){
            int[][] M7 = multiply(subtract(A12, A22), add(B21, B22));
            s7 = M7;
            int[] kopija = StraightenMatrix(s7);
            MPI.COMM_WORLD.Send(kopija,0, m * m, MPI.INT, 0, 0);
        }
        MPI.COMM_WORLD.Barrier();

        if (rank == 0){

            int[][] C11 = add(subtract(add(s1, s4), s5), s7);
            int[][] C12 = add(s3, s5);
            int[][] C21 = add(s2, s4);
            int[][] C22 = add(subtract(add(s1, s3), s2), s6);

            join(C11, C, 0, 0);
            join(C12, C, 0, n / 2);
            join(C21, C, n / 2, 0);
            join(C22, C, n / 2, n / 2);
            printMatrix(C,C.length,C.length);

        }

    }


}
