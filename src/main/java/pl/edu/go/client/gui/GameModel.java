/**
 * {@code GameModel} przechowuje stan gry po stronie klienta GUI.
 *
 * <p><b>MVC:</b> pełni rolę <b>Modelu</b>. Jest aktualizowany wyłącznie komunikatami protokołu z serwera
 * (BOARD, TURN, PHASE, SCORE, TERRITORY, DEADSTONES, END).
 *
 * <p>Model nie implementuje reguł gry i nie liczy wyniku — Single Source of Truth pozostaje po stronie serwera.
 */

package pl.edu.go.client.gui;

import pl.edu.go.game.GamePhase;
import pl.edu.go.game.PlayerColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GameModel {

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private PlayerColor myColor;
    private PlayerColor currentTurn;

    private GamePhase phase = GamePhase.PLAYING;

    private int boardSize = 9;
    private int[][] board = new int[boardSize][boardSize];

    private boolean finished;
    private String endMessage = "";

    private Integer scoreBlack = null;
    private Integer scoreWhite = null;

    // TERRITORY overlay (w review / po territory-end)
    private char[][] territoryMap = null;

    // DEAD stones mask (w review / po territory-end)
    private boolean[][] deadMask = null;

    // parsing BOARD
    private int pendingBoardSize = -1;
    private final List<String> pendingRows = new ArrayList<>();

    // parsing TERRITORY
    private int pendingTerritorySize = -1;
    private final List<String> pendingTerritoryRows = new ArrayList<>();

    // parsing DEADSTONES
    private int pendingDeadSize = -1;
    private final List<String> pendingDeadRows = new ArrayList<>();

    private final List<Runnable> listeners = new ArrayList<>();

    public void addListener(Runnable r) {
        listeners.add(Objects.requireNonNull(r));
    }

    private void notifyListeners() {
        for (Runnable r : listeners) r.run();
    }

    public synchronized void acceptServerLine(String line) {
        if (line == null) return;

        if (line.startsWith("WELCOME ")) {
            String c = line.substring("WELCOME ".length()).trim();
            myColor = PlayerColor.valueOf(c);
            notifyListeners();
            return;
        }

        if (line.startsWith("PHASE ")) {
            String p = line.substring("PHASE ".length()).trim();
            phase = GamePhase.valueOf(p);

            // po RESUME (PLAYING) czyścimy overlay i score
            if (phase == GamePhase.PLAYING) {
                territoryMap = null;
                deadMask = null;
                scoreBlack = null;
                scoreWhite = null;
            }

            notifyListeners();
            return;
        }

        if (line.startsWith("TURN ")) {
            String c = line.substring("TURN ".length()).trim();
            currentTurn = PlayerColor.valueOf(c);
            notifyListeners();
            return;
        }

        if (line.startsWith("SCORE ")) {
            String[] parts = line.split("\\s+");
            if (parts.length == 3) {
                try {
                    scoreBlack = Integer.parseInt(parts[1]);
                    scoreWhite = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignored) {}
            }
            notifyListeners();
            return;
        }

        // INFO/ERROR: bez loga w GUI – wypisujemy do terminala klienta
        if (line.startsWith("INFO ")) {
            System.out.println("[SERVER] " + line);
            return;
        }
        if (line.startsWith("ERROR ")) {
            System.err.println("[SERVER] " + line);
            return;
        }

        // --- BOARD ---
        if (line.startsWith("BOARD ")) {
            pendingBoardSize = Integer.parseInt(line.substring("BOARD ".length()).trim());
            pendingRows.clear();
            return;
        }
        if (line.startsWith("ROW ")) {
            pendingRows.add(line.substring("ROW ".length()));
            return;
        }
        if (line.equals("END_BOARD")) {
            if (pendingBoardSize > 0 && pendingRows.size() == pendingBoardSize) {
                boardSize = pendingBoardSize;
                board = new int[boardSize][boardSize];

                for (int y = 0; y < boardSize; y++) {
                    String row = pendingRows.get(y);
                    for (int x = 0; x < boardSize; x++) {
                        char ch = row.charAt(x);
                        board[x][y] = switch (ch) {
                            case 'X', 'B' -> BLACK;
                            case 'O', 'W' -> WHITE;
                            default -> EMPTY;
                        };
                    }
                }
            }
            pendingBoardSize = -1;
            pendingRows.clear();
            notifyListeners();
            return;
        }

        // --- TERRITORY ---
        if (line.startsWith("TERRITORY ")) {
            pendingTerritorySize = Integer.parseInt(line.substring("TERRITORY ".length()).trim());
            pendingTerritoryRows.clear();
            return;
        }
        if (line.startsWith("TROW ")) {
            pendingTerritoryRows.add(line.substring("TROW ".length()));
            return;
        }
        if (line.equals("END_TERRITORY")) {
            if (pendingTerritorySize > 0 && pendingTerritoryRows.size() == pendingTerritorySize) {
                int n = pendingTerritorySize;
                char[][] map = new char[n][n];

                for (int y = 0; y < n; y++) {
                    String row = pendingTerritoryRows.get(y);
                    for (int x = 0; x < n; x++) {
                        map[x][y] = row.charAt(x);
                    }
                }
                territoryMap = map;
            }
            pendingTerritorySize = -1;
            pendingTerritoryRows.clear();
            notifyListeners();
            return;
        }

        // --- DEADSTONES ---
        if (line.startsWith("DEADSTONES ")) {
            pendingDeadSize = Integer.parseInt(line.substring("DEADSTONES ".length()).trim());
            pendingDeadRows.clear();
            return;
        }
        if (line.startsWith("DROW ")) {
            pendingDeadRows.add(line.substring("DROW ".length()));
            return;
        }
        if (line.equals("END_DEADSTONES")) {
            if (pendingDeadSize > 0 && pendingDeadRows.size() == pendingDeadSize) {
                int n = pendingDeadSize;
                boolean[][] dm = new boolean[n][n];

                for (int y = 0; y < n; y++) {
                    String row = pendingDeadRows.get(y);
                    for (int x = 0; x < n; x++) {
                        dm[x][y] = (row.charAt(x) == '1');
                    }
                }
                deadMask = dm;
            }
            pendingDeadSize = -1;
            pendingDeadRows.clear();
            notifyListeners();
            return;
        }

        if (line.startsWith("END ")) {
            finished = true;
            endMessage = line;

            // jeśli resign – czyścimy overlay/score, bo to był tylko podgląd w review
            String lower = line.toLowerCase();
            if (lower.contains(" resign")) {
                territoryMap = null;
                deadMask = null;
                scoreBlack = null;
                scoreWhite = null;
            }

            notifyListeners();
            return;
        }

        // reszta: do terminala klienta
        System.out.println("[SERVER] " + line);
    }

    public PlayerColor getMyColor() { return myColor; }
    public PlayerColor getCurrentTurn() { return currentTurn; }
    public GamePhase getPhase() { return phase; }

    public Integer getScoreBlack() { return scoreBlack; }
    public Integer getScoreWhite() { return scoreWhite; }

    public char[][] getTerritoryMap() { return territoryMap; }
    public boolean[][] getDeadMask() { return deadMask; }

    public boolean isFinished() { return finished; }
    public String getEndMessage() { return endMessage; }

    public int getBoardSize() { return boardSize; }
    public int[][] getBoard() { return board; }

    public boolean canPlayNow() {
        return !finished
                && phase == GamePhase.PLAYING
                && myColor != null
                && currentTurn != null
                && myColor == currentTurn;
    }

    public boolean inReview() {
        return !finished && phase == GamePhase.SCORING_REVIEW;
    }

    public boolean finishedByTerritory() {
        if (!finished || endMessage == null) return false;
        return endMessage.toLowerCase().contains(" territory");
    }

    public boolean showScoringOverlays() {
        return inReview() || finishedByTerritory();
    }
}
