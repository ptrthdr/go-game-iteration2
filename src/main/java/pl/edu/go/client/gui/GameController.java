/**
 * {@code GameController} obsługuje akcje użytkownika w GUI i wysyła komendy do serwera.
 *
 * <p><b>MVC:</b> pełni rolę <b>Controller</b>. Mapuje kliknięcia i przyciski na protokół:
 * {@code MOVE}, {@code PASS}, {@code RESIGN}, {@code AGREE}, {@code RESUME}.
 *
 * <p>Kontroler nie implementuje reguł Go — walidacja należy do serwera ({@code Game/Board}).
 */

package pl.edu.go.client.gui;

import pl.edu.go.client.net.NetworkClient;
import pl.edu.go.game.GamePhase;

import java.io.IOException;

public final class GameController {

    private final NetworkClient net;
    private final GameModel model;

    public GameController(NetworkClient net, GameModel model) {
        this.net = net;
        this.model = model;
    }

    public void onIntersectionClicked(int x, int y) {
        if (!net.isConnected()) return;
        if (model.isFinished()) return;
        if (model.getPhase() != GamePhase.PLAYING) return;
        if (!model.canPlayNow()) return;

        try {
            net.sendLine("MOVE " + x + " " + y);
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    public void sendPass() {
        if (!net.isConnected()) return;
        if (!model.canPlayNow()) return;

        try {
            net.sendLine("PASS");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    public void sendResign() {
        if (!net.isConnected()) return;

        try {
            net.sendLine("RESIGN");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    public void sendAgree() {
        if (!net.isConnected()) return;
        if (!model.inReview()) return;

        try {
            net.sendLine("AGREE");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    public void sendResume() {
        if (!net.isConnected()) return;
        if (!model.inReview()) return;

        try {
            net.sendLine("RESUME");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }
}
