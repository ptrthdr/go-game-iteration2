/**
 * {@code PlaceStoneCommand} enkapsuluje żądanie wykonania ruchu ({@code MOVE}) w grze.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Komenda deleguje wykonanie do {@link pl.edu.go.game.Game}; walidacja reguł należy do {@code Board/Game}.
 */
package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.move.Move;


public class PlaceStoneCommand implements GameCommand {

    private final Move move;

    public PlaceStoneCommand(Move move) {
        this.move = move;
    }

    @Override
    public void execute(Game game) {
        // bez żadnych checków tury tutaj
        game.playMove(move);
    }
}
