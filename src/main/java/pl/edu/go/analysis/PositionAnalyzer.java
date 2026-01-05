package pl.edu.go.analysis;

import pl.edu.go.board.Board;
import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;

import java.util.*;

/**
 * {@code PositionAnalyzer} wykonuje analizę pozycji: wykrywa grupy kamieni oraz (w kontekście projektu)
 * identyfikuje grupy uznane za martwe.
 *
 * <p>Wynik {@code getDeadGroups()} jest wykorzystywany w punktacji jako „jeńcy” oraz przesyłany do GUI jako
 * {@code DEADSTONES}, aby wyjaśniać rozbieżność między terytorium a wynikiem.
 */

public class PositionAnalyzer {

    private final Board board;
    private final int size;

    public PositionAnalyzer(Board board) {
        this.board = board;
        this.size = board.getState().length;
    }

    public List<StoneGroup> getDeadGroups() {
        List<StoneGroup> dead = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board.getState()[x][y] != Board.EMPTY) {
                    String key = x + "," + y;
                    if (!visited.contains(key)) {
                        StoneGroup g = board.getGroup(x, y);
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
            for (int[] nb : board.neighbors(s.getX(), s.getY())) {
                if (board.getState()[nb[0]][nb[1]] == Board.EMPTY) {
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

                        for (int[] nnb : board.neighbors(p[0], p[1])) {
                            if (board.getState()[nnb[0]][nnb[1]] == Board.EMPTY)
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
        int opp = (color == Board.BLACK ? Board.WHITE : Board.BLACK);

        for (String p : area) {
            String[] parts = p.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);

            for (int[] nb : board.neighbors(x, y)) {
                if (board.getState()[nb[0]][nb[1]] == opp)
                    return false;
            }
        }
        return true;
    }
}
