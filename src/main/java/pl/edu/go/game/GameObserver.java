/**
 * {@code GameObserver} jest interfejsem obserwatora stanu gry.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Observer</b>.
 * Implementacje (np. warstwa serwera) subskrybują zdarzenia z {@link pl.edu.go.game.Game}
 * i reagują na zmiany (np. rozsyłając protokół do klientów).
 */

package pl.edu.go.game;


import pl.edu.go.board.Board;

public interface GameObserver {
    void onBoardChanged(Board board);
    void onGameEnded(GameResult result);
    void onPlayerToMoveChanged(PlayerColor player);
    void onPhaseChanged(GamePhase phase);
}