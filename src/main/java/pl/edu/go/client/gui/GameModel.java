package pl.edu.go.client.gui;

import pl.edu.go.game.PlayerColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GameModel — stan klienta GUI.
 *
 * Parsuje komunikaty serwera:
 * - WELCOME <BLACK|WHITE>
 * - INFO <...>, ERROR <...>
 * - TURN <BLACK|WHITE>
 * - BOARD <size>, ROW <...>, END_BOARD
 * - END <WINNER> <reason>
 *
 * UWAGA: serwer koduje kamienie jako X (BLACK) i O (WHITE).
 */
public final class GameModel {

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private PlayerColor myColor;
    private PlayerColor currentTurn;

    private int boardSize = 9;
    private int[][] board = new int[boardSize][boardSize];

    private boolean finished;
    private String endMessage = "";

    // parsing BOARD
    private int pendingBoardSize = -1;
    private final List<String> pendingRows = new ArrayList<>();

    private final List<String> logLines = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public void addListener(Runnable r) {
        listeners.add(Objects.requireNonNull(r));
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            r.run();
        }
    }

    /**
     * Dodaje linię do logu i odświeża UI.
     */
    public synchronized void addLog(String s) {
        logLines.add(s);
        if (logLines.size() > 300) {
            logLines.remove(0);
        }
        notifyListeners();
    }

    public synchronized void acceptServerLine(String line) {
        if (line == null) return;

        if (line.startsWith("WELCOME ")) {
            String c = line.substring("WELCOME ".length()).trim();
            myColor = PlayerColor.valueOf(c);
            addLog("You are " + myColor);
            return;
        }

        if (line.startsWith("TURN ")) {
            String c = line.substring("TURN ".length()).trim();
            currentTurn = PlayerColor.valueOf(c);
            notifyListeners();
            return;
        }

        if (line.startsWith("INFO ")) {
            addLog(line.substring("INFO ".length()));
            return;
        }

        if (line.startsWith("ERROR ")) {
            addLog("[ERROR] " + line.substring("ERROR ".length()));
            return;
        }

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

                        // Serwer: X=BLACK, O=WHITE, .=EMPTY
                        // (dla kompatybilności wspieramy też B/W)
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

        if (line.startsWith("END ")) {
            finished = true;
            endMessage = line;
            addLog("[END] " + line.substring("END ".length()));
            return;
        }

        // fallback
        addLog("[SERVER] " + line);
    }

    public String getLogText() {
        StringBuilder sb = new StringBuilder();
        for (String l : logLines) {
            sb.append(l).append('\n');
        }
        return sb.toString();
    }

    public PlayerColor getMyColor() {
        return myColor;
    }

    public PlayerColor getCurrentTurn() {
        return currentTurn;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getEndMessage() {
        return endMessage;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public int[][] getBoard() {
        return board;
    }

    public boolean canPlayNow() {
        return !finished && myColor != null && currentTurn != null && myColor == currentTurn;
    }
}
