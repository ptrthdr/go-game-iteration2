/**
 * {@code Game} implementuje centralną logikę rozgrywki Go na poziomie „sesji gry”
 * (warstwa aplikacyjna nad {@code Board}).
 *
 * <p><b>Single Source of Truth:</b>
 * <ul>
 *   <li>{@code Game} pilnuje: fazy ({@link pl.edu.go.game.GamePhase}), tury gracza, stanu zakończenia,
 *       obsługi PASS/RESIGN/AGREE/RESUME oraz wyzwalania punktacji.</li>
 *   <li>{@code Board} pozostaje źródłem prawdy dla reguł planszy (legalność ruchu, bicie, KO itd.).</li>
 * </ul>
 *
 * <p><b>Wzorzec projektowy:</b>
 * <ul>
 *   <li><b>Observer</b> — {@code Game} publikuje zdarzenia do {@link pl.edu.go.game.GameObserver}
 *       (zmiana planszy, tury, fazy, zakończenie gry).</li>
 * </ul>
 *
 * <p><b>Zasada 8 (minimal review):</b>
 * <ul>
 *   <li>2×PASS → {@code SCORING_REVIEW} (gra nie kończy się automatycznie),</li>
 *   <li>w {@code SCORING_REVIEW}: tylko {@code AGREE}/{@code RESUME},</li>
 *   <li>po {@code RESUME}: reset PASS i <b>ruch ma przeciwnik wznawiającego</b>,</li>
 *   <li>po {@code AGREE}+{@code AGREE}: punktacja (zad. 9) i zakończenie gry.</li>
 * </ul>
 *
 * <p><b>Zadanie 10:</b> gracz może zakończyć grę w dowolnym momencie przez {@code RESIGN}.
 */

package pl.edu.go.game;

import pl.edu.go.board.Board;
import pl.edu.go.move.Move;
import pl.edu.go.analysis.ScoreCalculator;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private final Board board;

    private PlayerColor currentPlayer = PlayerColor.BLACK; // zaczyna BLACK
    private boolean finished = false;
    private GameResult result;

    private int consecutivePasses = 0;

    // ZASADA 8
    private GamePhase phase = GamePhase.PLAYING;
    private boolean agreedBlack = false;
    private boolean agreedWhite = false;

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

    public GamePhase getPhase() {
        return phase;
    }

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

    private void notifyPhaseChanged() {
        for (GameObserver o : observers) {
            o.onPhaseChanged(phase);
        }
    }

    // ===== MOVE =====

    public void playMove(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move is null");
        }
        PlayerColor player = PlayerColor.fromBoardColor(move.getColor());
        playMove(player, move.getX(), move.getY());
    }

    public void playMove(PlayerColor player, int x, int y) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.PLAYING) {
            throw new IllegalStateException("Not in PLAYING phase");
        }
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn: " + player.name());
        }

        boolean ok = board.playMove(player.toBoardColor(), x, y);
        if (!ok) {
            throw new IllegalArgumentException("Illegal move");
        }

        consecutivePasses = 0;

        currentPlayer = currentPlayer.opposite();
        notifyBoardChanged();
        notifyPlayerToMoveChanged();
    }

    // ===== PASS =====

    /**
     * Wykonuje PASS w fazie {@code PLAYING}.
     *
     * <p>Po dwóch kolejnych PASS gra przechodzi do fazy {@code SCORING_REVIEW}
     * (gra nie kończy się automatycznie).</p>
     *
     * @param player gracz wykonujący PASS
     * @throws IllegalStateException jeśli gra zakończona lub nie w fazie PLAYING albo nie tura gracza
     */
    public void pass(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.PLAYING) {
            throw new IllegalStateException("PASS allowed only in PLAYING phase");
        }
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn: " + player.name());
        }

        consecutivePasses++;

        if (consecutivePasses >= 2) {
            phase = GamePhase.SCORING_REVIEW;
            agreedBlack = false;
            agreedWhite = false;
            notifyPhaseChanged();
            return;
        }

        currentPlayer = currentPlayer.opposite();
        notifyPlayerToMoveChanged();
    }

    // ===== ZASADA 8: REVIEW =====

    /**
     * AGREE — gracz akceptuje automatycznie policzony wynik/terytorium.
     * Gdy obaj gracze AGREE -> uruchamiamy ZASADĘ 9 i kończymy grę (END).
     */
    public void agree(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.SCORING_REVIEW) {
            throw new IllegalStateException("AGREE allowed only in SCORING_REVIEW");
        }

        if (player == PlayerColor.BLACK) {
            agreedBlack = true;
        } else {
            agreedWhite = true;
        }

        if (agreedBlack && agreedWhite) {
            endByTerritory();
        }
    }

    /**
     * Wznawia grę z fazy {@code SCORING_REVIEW} do {@code PLAYING}.
     *
     * <p>Zgodnie z wymaganiem zadania: gracz wznawiający oddaje prawo następnego ruchu
     * przeciwnikowi.</p>
     *
     * @param player gracz żądający wznowienia
     * @throws IllegalStateException jeśli nie w fazie SCORING_REVIEW albo gra zakończona
     */
    public void resume(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.SCORING_REVIEW) {
            throw new IllegalStateException("RESUME allowed only in SCORING_REVIEW");
        }

        phase = GamePhase.PLAYING;
        consecutivePasses = 0;
        agreedBlack = false;
        agreedWhite = false;

        // klucz: wznawiający oddaje ruch przeciwnikowi
        currentPlayer = player.opposite();

        notifyPhaseChanged();
        notifyPlayerToMoveChanged();
    }

    // ===== RESIGN =====

    /**
     * RESIGN — gra kończy się od razu, wygrywa przeciwnik.
     */
    public void resign(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }

        finished = true;
        phase = GamePhase.FINISHED;
        notifyPhaseChanged();

        result = new GameResult(player.opposite(), "resign");
        notifyGameEnded();
    }

    // ===== koniec przez terytorium (zasada 9) =====

    private void endByTerritory() {
        finished = true;
        phase = GamePhase.FINISHED;
        notifyPhaseChanged();

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
    }
}
