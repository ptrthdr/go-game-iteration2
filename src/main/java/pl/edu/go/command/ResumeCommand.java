package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

/**
 * {@code ResumeCommand} enkapsuluje komendę {@code RESUME} w fazie {@code SCORING_REVIEW}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Wznowienie resetuje licznik PASS oraz przekazuje ruch przeciwnikowi wznawiającego (wymóg zad. 8).
 */

public class ResumeCommand implements GameCommand {

    private final PlayerColor player;

    public ResumeCommand(PlayerColor player) {
        this.player = player;
    }

    @Override
    public void execute(Game game) {
        game.resume(player);
    }
}
