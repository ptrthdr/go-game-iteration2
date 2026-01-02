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

    // -------------------------------------------------
    // PODSTAWY
    // -------------------------------------------------

    private boolean inside(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    private List<int[]> neighbors(int x, int y) {
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

    // -------------------------------------------------
    // GRUPY I ODDECHY (ZASADY 1–6)
    // -------------------------------------------------

    private StoneGroup getGroup(int x, int y) {
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

    // WYMAGANE przez testy (refleksja!)
    private int countLiberties(StoneGroup g) {
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

    // -------------------------------------------------
    // ZASADA 7 – ŻYCIE STRATEGICZNE (DWA OCZY)
    // -------------------------------------------------

    private boolean isStrategicallyAlive(StoneGroup g) {
        return hasTwoEyes(g);
    }

    private boolean hasTwoEyes(StoneGroup g) {
        List<Set<String>> areas = collectAdjacentEmptyAreas(g);
        int eyes = 0;

        for (Set<String> area : areas) {
            if (isEye(area, g.getColor())) {
                eyes++;
                if (eyes >= 2)
                    return true;
            }
        }
        return false;
    }

    private List<Set<String>> collectAdjacentEmptyAreas(StoneGroup g) {
        Set<String> seen = new HashSet<>();
        List<Set<String>> areas = new ArrayList<>();

        for (Stone s : g.getStones()) {
            for (int[] nb : neighbors(s.getX(), s.getY())) {
                if (board[nb[0]][nb[1]] == EMPTY) {
                    String start = nb[0] + "," + nb[1];
                    if (seen.contains(start))
                        continue;

                    Set<String> area = new HashSet<>();
                    Stack<int[]> stack = new Stack<>();
                    stack.push(new int[] { nb[0], nb[1] });

                    while (!stack.isEmpty()) {
                        int[] p = stack.pop();
                        String key = p[0] + "," + p[1];
                        if (seen.contains(key))
                            continue;

                        seen.add(key);
                        area.add(key);

                        for (int[] nnb : neighbors(p[0], p[1])) {
                            if (board[nnb[0]][nnb[1]] == EMPTY)
                                stack.push(nnb);
                        }
                    }
                    areas.add(area);
                }
            }
        }
        return areas;
    }

    private boolean isEye(Set<String> area, int color) {
        int opp = (color == BLACK ? WHITE : BLACK);

        for (String p : area) {
            String[] parts = p.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);

            for (int[] nb : neighbors(x, y)) {
                if (board[nb[0]][nb[1]] == opp)
                    return false;
            }
        }
        return true;
    }

    // -------------------------------------------------
    // PUNKTY NICZYJE, SEKI, TERYTORIUM
    // -------------------------------------------------

    private Territory[][] computeRawTerritory() {
        Territory[][] out = new Territory[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                if (board[x][y] != EMPTY) {
                    out[x][y] = Territory.NEUTRAL;
                    continue;
                }

                boolean b = false, w = false;
                for (int[] nb : neighbors(x, y)) {
                    if (board[nb[0]][nb[1]] == BLACK)
                        b = true;
                    if (board[nb[0]][nb[1]] == WHITE)
                        w = true;
                }

                if (b && w)
                    out[x][y] = Territory.NEUTRAL;
                else if (b)
                    out[x][y] = Territory.BLACK;
                else if (w)
                    out[x][y] = Territory.WHITE;
                else
                    out[x][y] = Territory.NEUTRAL;
            }
        }
        return out;
    }

    private boolean groupTouchesNeutral(StoneGroup g, Territory[][] raw) {
        for (Stone s : g.getStones()) {
            for (int[] nb : neighbors(s.getX(), s.getY())) {
                if (board[nb[0]][nb[1]] == EMPTY &&
                        raw[nb[0]][nb[1]] == Territory.NEUTRAL)
                    return true;
            }
        }
        return false;
    }

    public List<StoneGroup> getDeadGroups() {
        List<StoneGroup> dead = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board[x][y] != EMPTY) {
                    String key = x + "," + y;
                    if (!visited.contains(key)) {
                        StoneGroup g = getGroup(x, y);
                        for (Stone s : g.getStones())
                            visited.add(s.getX() + "," + s.getY());

                        if (!isStrategicallyAlive(g))
                            dead.add(g);
                    }
                }
            }
        }
        return dead;
    }

    public Territory[][] computeTerritory() {
        Territory[][] raw = computeRawTerritory();
        Territory[][] out = new Territory[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                if (board[x][y] == EMPTY) {
                    out[x][y] = raw[x][y];
                    continue;
                }

                StoneGroup g = getGroup(x, y);

                if (countLiberties(g) >= 2 && groupTouchesNeutral(g, raw))
                    out[x][y] = Territory.SEKI;

                else
                    out[x][y] = Territory.NEUTRAL;
            }
        }
        return out;
    }

    // -------------------------------------------------
    // LOGIKA RUCHU (ZASADY 1–6)
    // -------------------------------------------------

    public boolean playMove(int color, int x, int y) {

        if (!inside(x, y) || board[x][y] != EMPTY)
            return false;

        int[][] before = deepCopy(board);
        board[x][y] = color;
        int opp = (color == BLACK ? WHITE : BLACK);
        boolean captured = false;

        for (int[] nb : neighbors(x, y)) {
            int nx = nb[0], ny = nb[1];
            if (board[nx][ny] == opp) {
                StoneGroup g = getGroup(nx, ny);
                if (countLiberties(g) == 0) {
                    removeGroup(g);
                    captured = true;
                }
            }
        }

        StoneGroup my = getGroup(x, y);
        if (countLiberties(my) == 0 && !captured) {
            board[x][y] = EMPTY;
            return false;
        }

        if (captured && previousBoard != null && boardsEqual(board, previousBoard)) {
            board[x][y] = EMPTY;
            return false;
        }

        previousBoard = before;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String[] symbols = { ".", "○", "●" };
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                sb.append(symbols[board[x][y]]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public int[][] getState() {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++)
            System.arraycopy(board[i], 0, copy[i], 0, size);
        return copy;
    }
}
