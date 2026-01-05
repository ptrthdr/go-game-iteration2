package pl.edu.go.analysis;

import pl.edu.go.board.Board;
import pl.edu.go.board.Territory;
import pl.edu.go.model.StoneGroup;

/**
 * {@code ScoreCalculator} wylicza wynik końcowy gry w wariancie „territory scoring”.
 *
 * <p>Wynik jest sumą:
 * <ul>
 *   <li><b>terytorium</b> — z {@link pl.edu.go.analysis.TerritoryAnalyzer},</li>
 *   <li><b>jeńców (dead stones)</b> — z {@link pl.edu.go.analysis.PositionAnalyzer#getDeadGroups()}
 *       (kamienie uznane za martwe dodawane jako punkty dla przeciwnika).</li>
 * </ul>
 *
 * <p>Klasa pełni rolę serwisu obliczeniowego (Utility/Service). Nie modyfikuje planszy.
 */

public class ScoreCalculator {

    /**
     * Zwraca punkty w formacie:
     * [0] = BLACK, [1] = WHITE
     */
    public static int[] computeScore(Board board) {

        // --- ANALIZATORY ---
        TerritoryAnalyzer territoryAnalyzer = new TerritoryAnalyzer(board);
        PositionAnalyzer positionAnalyzer = new PositionAnalyzer(board);

        Territory[][] t = territoryAnalyzer.computeTerritory();

        int black = 0;
        int white = 0;

        // 1. Liczenie terytorium
        for (int x = 0; x < t.length; x++) {
            for (int y = 0; y < t.length; y++) {
                if (t[x][y] == Territory.BLACK) {
                    black++;
                } else if (t[x][y] == Territory.WHITE) {
                    white++;
                }
            }
        }

        // 2. Liczenie jeńców (martwe grupy)
        for (StoneGroup g : positionAnalyzer.getDeadGroups()) {
            int cnt = g.getStones().size();
            if (g.getColor() == Board.BLACK) {
                white += cnt; // czarne zbite → punkty białych
            } else {
                black += cnt; // białe zbite → punkty czarnych
            }
        }

        return new int[] { black, white };
    }
}
