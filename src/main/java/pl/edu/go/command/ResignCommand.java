package pl.edu.go.command;

/**
 * {@code ResignCommand} enkapsuluje komendę {@code RESIGN}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Zgodnie z zadaniem 10 może zakończyć grę w dowolnym momencie.
 */


import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

public class ResignCommand implements GameCommand {

    private final PlayerColor player;

    public ResignCommand(PlayerColor player) {
        this.player = player;
    }

    @Override
    public void execute(Game game) {
        game.resign(player);
    }
}
