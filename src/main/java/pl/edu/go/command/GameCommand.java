package pl.edu.go.command;

/**
 * {@code GameCommand} reprezentuje komendę wykonywaną na obiekcie {@link pl.edu.go.game.Game}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Poszczególne implementacje enkapsulują wywołanie (MOVE/PASS/RESIGN/AGREE/RESUME)
 * i delegują wykonanie do {@code Game}.
 */


import pl.edu.go.game.Game;

public interface GameCommand {
    void execute(Game game) throws Exception;
}