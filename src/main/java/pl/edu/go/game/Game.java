package pl.edu.go.game;

import pl.edu.go.board.Board;
import pl.edu.go.move.Move;
import pl.edu.go.analysis.ScoreCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Game — „silnik wysokiego poziomu” nad Board.
 *
 * Ściąga (Single Source of Truth):
 * - Game pilnuje: finished, tura (currentPlayer), PASS/RESIGN, wynik,
 * powiadomienia Observer.
 * - Board pilnuje: reguły planszy (zajęte pole, bicie, oddechy, samobójstwo,
 * bounds).
 * - Komendy (Command) NIE walidują tury/finished — tylko delegują do Game.
 *
 * Observer:
 * - notifyBoardChanged() po udanym MOVE,
 * - notifyPlayerToMoveChanged() po zmianie tury (MOVE/PASS),
 * - notifyGameEnded() po końcu gry (2xPASS lub RESIGN).
 */
public class Game {

    private final Board board;

    // Ściąga: stan gry
    private PlayerColor currentPlayer = PlayerColor.BLACK; // zaczyna BLACK
    private boolean finished = false;
    private GameResult result;

    private int consecutivePasses = 0; // 2 passy => koniec gry
    private final List<GameObserver> observers = new ArrayList<>();

    public Game(Board board) {
        this.board = board;
    }

    public Board getBoard() {
        return board;
    }

    public PlayerColor getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isFinished() {
        return finished;
    }

    public GameResult getResult() {
        return result;
    }

    // ------- OBSERVER API -------

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    private void notifyBoardChanged() {
        for (GameObserver o : observers) {
            o.onBoardChanged(board);
        }
    }

    private void notifyPlayerToMoveChanged() {
        for (GameObserver o : observers) {
            o.onPlayerToMoveChanged(currentPlayer);
        }
    }

    private void notifyGameEnded() {
        for (GameObserver o : observers) {
            o.onGameEnded(result);
        }
    }

    // ------- LOGIKA WYSOKIEGO POZIOMU -------

    /**
     * Wykonanie ruchu przez obiekt Move.
     * Move to dane (kolor + x/y), walidacja zawsze tu w Game + Board.
     */
    public void playMove(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move is null");
        }

        PlayerColor player = PlayerColor.fromBoardColor(move.getColor());
        playMove(player, move.getX(), move.getY());
    }

    /**
     * Wykonanie ruchu na (x,y) przez konkretnego gracza.
     */
    public void playMove(PlayerColor player, int x, int y) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn: " + player.name());
        }

        boolean ok = board.playMove(player.toBoardColor(), x, y);
        if (!ok) {
            throw new IllegalStateException(
                    "Illegal move at (" + (char) ('A' + x) + ", " + (y + 1) + ")");
        }

        consecutivePasses = 0;
        currentPlayer = currentPlayer.opposite();

        notifyBoardChanged();
        notifyPlayerToMoveChanged();
    }

    /**
     * PASS.
     * 2 kolejne passy => koniec gry + ZASADA 9 (punktacja terytorium).
     */
    public void pass(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn: " + player.name());
        }

        consecutivePasses++;

        if (consecutivePasses >= 2) {
            finished = true;

            // --- ZASADA 9 ---
            int[] score = ScoreCalculator.computeScore(board);

            PlayerColor winner;
            if (score[0] > score[1]) {
                winner = PlayerColor.BLACK;
            } else if (score[1] > score[0]) {
                winner = PlayerColor.WHITE;
            } else {
                winner = null; // remis
            }

            result = new GameResult(winner, "territory");
            notifyGameEnded();
            return;
        }

        currentPlayer = currentPlayer.opposite();
        notifyPlayerToMoveChanged();
    }

    /**
     * RESIGN.
     * wygrywa przeciwnik, gra kończy się od razu.
     */
    public void resign(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }

        finished = true;
        result = new GameResult(player.opposite(), "resign");
        notifyGameEnded();
    }
}
