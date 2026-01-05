package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

/**
 * {@code AgreeCommand} enkapsuluje komendę {@code AGREE} w fazie {@code SCORING_REVIEW}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Po zgodzie obu graczy uruchamiana jest punktacja (zad. 9) i gra się kończy.
 */

public class AgreeCommand implements GameCommand {

    private final PlayerColor player;

    public AgreeCommand(PlayerColor player) {
        this.player = player;
    }

    @Override
    public void execute(Game game) {
        game.agree(player);
    }
}
