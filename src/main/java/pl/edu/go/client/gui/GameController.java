/**
 * Klasa GameController — kontroler logiki w kliencie GUI.
 *
 * Wzorzec:
 * - MVC / MVP:
 *   - Łączy widok (BoardView) i model (GameModel).
 *
 * Rola klasy:
 * - reaguje na akcje użytkownika (kliknięcia w planszę, przyciski),
 * - wysyła odpowiednie komendy tekstowe do serwera (MOVE, PASS, RESIGN),
 * - w razie potrzeby aktualizuje model i odświeża widok.
 */

package pl.edu.go.client.gui;

import pl.edu.go.client.net.NetworkClient;

import java.io.IOException;

/**
 * GameController — logika sterowania GUI.
 *
 * Wysyła komendy do serwera:
 * - MOVE x y
 * - PASS
 * - RESIGN
 */
public final class GameController {

    private final NetworkClient net;
    private final GameModel model;

    public GameController(NetworkClient net, GameModel model) {
        this.net = net;
        this.model = model;
    }

    public void onIntersectionClicked(int x, int y) {
        if (!net.isConnected()) return;
        if (!model.canPlayNow()) return;

        try {
            net.sendLine("MOVE " + x + " " + y);
        } catch (IOException e) {
            model.addLog("[ERROR] " + e.getMessage());
        }
    }

    public void sendPass() {
        if (!net.isConnected()) return;
        try {
            net.sendLine("PASS");
        } catch (IOException e) {
            model.addLog("[ERROR] " + e.getMessage());
        }
    }

    public void sendResign() {
        if (!net.isConnected()) return;
        try {
            net.sendLine("RESIGN");
        } catch (IOException e) {
            model.addLog("[ERROR] " + e.getMessage());
        }
    }
}
