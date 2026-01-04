package pl.edu.go.analysis;

import pl.edu.go.board.Board;
import pl.edu.go.board.Territory;
import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;

public class TerritoryAnalyzer {

    private final Board board;
    private final int size;

    public TerritoryAnalyzer(Board board) {
        this.board = board;
        this.size = board.getState().length;
    }

    public Territory[][] computeTerritory() {
        Territory[][] raw = computeRawTerritory();
        Territory[][] out = new Territory[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                if (board.getState()[x][y] == Board.EMPTY) {
                    out[x][y] = raw[x][y];
                    continue;
                }

                StoneGroup g = board.getGroup(x, y);

                if (board.countLiberties(g) >= 2 && groupTouchesNeutral(g, raw))
                    out[x][y] = Territory.SEKI;
                else
                    out[x][y] = Territory.NEUTRAL;
            }
        }
        return out;
    }

    private Territory[][] computeRawTerritory() {
        Territory[][] out = new Territory[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                if (board.getState()[x][y] != Board.EMPTY) {
                    out[x][y] = Territory.NEUTRAL;
                    continue;
                }

                boolean b = false, w = false;
                for (int[] nb : board.neighbors(x, y)) {
                    if (board.getState()[nb[0]][nb[1]] == Board.BLACK)
                        b = true;
                    if (board.getState()[nb[0]][nb[1]] == Board.WHITE)
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
            for (int[] nb : board.neighbors(s.getX(), s.getY())) {
                if (board.getState()[nb[0]][nb[1]] == Board.EMPTY &&
                        raw[nb[0]][nb[1]] == Territory.NEUTRAL)
                    return true;
            }
        }
        return false;
    }
}
