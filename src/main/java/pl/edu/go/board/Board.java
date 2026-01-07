package pl.edu.go.board;

import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;

import java.util.*;

public class Board {

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private final int size;
    private final int[][] board;
    private int[][] previousBoard = null;

    public Board(int size) {
        this.size = size;
        this.board = new int[size][size];
    }

    public boolean inside(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    public List<int[]> neighbors(int x, int y) {
        List<int[]> n = new ArrayList<>();
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (inside(nx, ny))
                n.add(new int[] { nx, ny });
        }
        return n;
    }

    public StoneGroup getGroup(int x, int y) {
        int color = board[x][y];
        StoneGroup g = new StoneGroup(color);

        Set<String> visited = new HashSet<>();
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[] { x, y });

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            String key = p[0] + "," + p[1];
            if (visited.contains(key))
                continue;

            visited.add(key);
            g.addStone(new Stone(p[0], p[1], color));

            for (int[] nb : neighbors(p[0], p[1]))
                if (board[nb[0]][nb[1]] == color)
                    stack.push(nb);
        }
        return g;
    }

    public int countLiberties(StoneGroup g) {
        Set<String> libs = new HashSet<>();
        for (Stone s : g.getStones()) {
            for (int[] nb : neighbors(s.getX(), s.getY())) {
                if (board[nb[0]][nb[1]] == EMPTY)
                    libs.add(nb[0] + "," + nb[1]);
            }
        }
        return libs.size();
    }

    private void removeGroup(StoneGroup g) {
        for (Stone s : g.getStones())
            board[s.getX()][s.getY()] = EMPTY;
    }

    public boolean playMove(int color, int x, int y) {

        if (!inside(x, y) || board[x][y] != EMPTY)
            return false;

        int[][] before = deepCopy(board);
        board[x][y] = color;

        int opp = (color == BLACK ? WHITE : BLACK);
        int capturedStones = 0;

        for (int[] nb : neighbors(x, y)) {
            int nx = nb[0], ny = nb[1];
            if (board[nx][ny] == opp) {
                StoneGroup g = getGroup(nx, ny);
                if (countLiberties(g) == 0) {
                    capturedStones += g.getStones().size();
                    removeGroup(g);
                }
            }
        }

        StoneGroup my = getGroup(x, y);
        if (countLiberties(my) == 0 && capturedStones == 0) {
            board[x][y] = EMPTY;
            return false;
        }

        if (capturedStones == 1 && previousBoard != null &&
                boardsEqual(board, previousBoard)) {

            for (int i = 0; i < size; i++)
                System.arraycopy(before[i], 0, board[i], 0, size);

            return false;
        }

        previousBoard = before;
        return true;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++)
            System.arraycopy(src[i], 0, copy[i], 0, size);
        return copy;
    }

    private boolean boardsEqual(int[][] a, int[][] b) {
        if (a == null || b == null)
            return false;
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if (a[i][j] != b[i][j])
                    return false;
        return true;
    }

    public int[][] getState() {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++)
            System.arraycopy(board[i], 0, copy[i], 0, size);
        return copy;
    }

}
