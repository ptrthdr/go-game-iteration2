/**
 * {@code TextCommandFactory} mapuje surową linię tekstu protokołu na obiekt {@link pl.edu.go.command.GameCommand}.
 *
 * <p><b>Wzorce projektowe:</b>
 * <ul>
 *   <li><b>Simple Factory / Factory Method</b> — centralizuje tworzenie obiektów komend na podstawie tokenów.</li>
 *   <li><b>Command</b> — zwracane obiekty są implementacjami {@link pl.edu.go.command.GameCommand}.</li>
 * </ul>
 *
 * <p>Parser nie wykonuje logiki gry (np. walidacji reguł), a jedynie buduje komendę i przekazuje do wykonania.
 */
package pl.edu.go.command;

import pl.edu.go.game.PlayerColor;
import pl.edu.go.move.Move;
import pl.edu.go.move.MoveFactory;

public class TextCommandFactory {

    public GameCommand fromNetworkMessage(String message, PlayerColor player) {
        if (message == null) {
            throw new IllegalArgumentException("Empty command");
        }

        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }

        String[] parts = trimmed.split("\\s+");
        String keyword = parts[0].toUpperCase();

        return switch (keyword) {
            case "MOVE" -> {
                if (parts.length != 3) {
                    throw new IllegalArgumentException("MOVE format: MOVE x y");
                }
                int x;
                int y;
                try {
                    x = Integer.parseInt(parts[1]);
                    y = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("MOVE coordinates must be integers: MOVE x y");
                }

                // U Ciebie MoveFactory oczekuje int koloru (Board.BLACK/Board.WHITE)
                Move move = MoveFactory.createMove(player.toBoardColor(), x, y);
                yield new PlaceStoneCommand(move);
            }

            case "PASS" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("PASS takes no arguments");
                }
                yield new PassCommand(player);
            }

            case "RESIGN" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("RESIGN takes no arguments");
                }
                yield new ResignCommand(player);
            }

            case "AGREE" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("AGREE takes no arguments");
                }
                yield new AgreeCommand(player);
            }

            case "RESUME" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("RESUME takes no arguments");
                }
                yield new ResumeCommand(player);
            }

            default -> throw new IllegalArgumentException("Unknown command: " + keyword);
        };
    }
}
