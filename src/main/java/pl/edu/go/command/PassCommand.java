/**
 * {@code PassCommand} enkapsuluje komendę {@code PASS}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Komenda wywołuje {@code Game.pass(...)} i może spowodować przejście do {@code SCORING_REVIEW} (zad. 8).
 */

package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;


public class PassCommand implements GameCommand {

    private final PlayerColor player;

    public PassCommand(PlayerColor player) {
        this.player = player;
    }

    @Override
    public void execute(Game game) {
        // bez checka tury tutaj
        game.pass(player);
    }
}
