import mpi.*;

public class FirstProgram {
    private static final int NUMBER_ROWS_A = 10;
    private static final int NUMBER_COLUMNS_A = 10;
    private static final int NUMBER_COLUMNS_B = 10;
    private static final int MASTER = 0;
    private static final int FROM_MASTER = 1;
    private static final int TO_MASTER = 2;
    public static void main(String[] args)
    {
        double[][] matrix_a = new double[NUMBER_ROWS_A][NUMBER_COLUMNS_A];
        double[][] matrix_b = new double[NUMBER_COLUMNS_A][NUMBER_COLUMNS_B];
        double[][] result_matrix = new double[NUMBER_ROWS_A][NUMBER_COLUMNS_B];

        MPI.Init(args);
        int[] arr = new int[10];
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (size < 2)
        {
            System.out.println("Two MPI tasks are minimum. Exiting...\n");
            MPI.COMM_WORLD.Abort(1);
        }

        int[] offset = { 0 };
        int[] rows = { 0 };
        if(rank == MASTER)
        {
            for (int i = 0; i < NUMBER_ROWS_A; i++)
            {
                for (int j = 0; j < NUMBER_COLUMNS_A; j++)
                {
                    matrix_a[i][j] = 1;
                }
            }

            for (int i = 0; i < NUMBER_COLUMNS_A; i++)
            {
                for (int j = 0; j < NUMBER_COLUMNS_B; j++)
                {
                    matrix_b[i][j] = 1;
                }
            }

            int amount_for_process = NUMBER_ROWS_A / (size - 1);
            int extra = NUMBER_ROWS_A % (size - 1);
            for (int destination = 1; destination < size; ++destination)
            {
                rows[0] = destination <= extra ? amount_for_process + 1 : amount_for_process;

                MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, destination, FROM_MASTER);
                MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, destination, FROM_MASTER);
                MPI.COMM_WORLD.Send(matrix_a, offset[0], rows[0], MPI.OBJECT, destination, FROM_MASTER);
                MPI.COMM_WORLD.Send(matrix_b, 0, NUMBER_COLUMNS_A, MPI.OBJECT, destination, FROM_MASTER);

                offset[0] = offset[0] + rows[0];
            }

            for (int source = 1; source < size; ++source)
            {
                MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, TO_MASTER);
                MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, TO_MASTER);
                MPI.COMM_WORLD.Recv(result_matrix, offset[0], rows[0], MPI.OBJECT, source, TO_MASTER);
            }

            for(int i = 0; i < NUMBER_ROWS_A; i++)
            {
                for (int j = 0; j < NUMBER_COLUMNS_B; j++)
                {
                    System.out.print(result_matrix[i][j] + " ");
                }
                System.out.print('\n');
            }
        }
        else
        {
            MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, MASTER, FROM_MASTER);
            MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, MASTER, FROM_MASTER);
            MPI.COMM_WORLD.Recv(matrix_a, 0, rows[0], MPI.OBJECT, MASTER, FROM_MASTER);
            MPI.COMM_WORLD.Recv(matrix_b, 0, NUMBER_COLUMNS_A, MPI.OBJECT, MASTER, FROM_MASTER);

            for (int k = 0; k < NUMBER_COLUMNS_B; k++)
            {
                for (int i = 0; i < rows[0]; i++)
                {
                    for (int j = 0; j < NUMBER_COLUMNS_A; j++)
                    {
                        result_matrix[i][k] += matrix_a[i][j] * matrix_b[j][k];
                    }
                }
            }

            MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, MASTER, TO_MASTER);
            MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, MASTER, TO_MASTER);
            MPI.COMM_WORLD.Send(result_matrix, 0, rows[0], MPI.OBJECT, MASTER, TO_MASTER);
        }

        MPI.Finalize();
    }
}