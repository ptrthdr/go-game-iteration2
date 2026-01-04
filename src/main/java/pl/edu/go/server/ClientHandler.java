package pl.edu.go.server;

import pl.edu.go.game.PlayerColor;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Klasa ClientHandler — obsługa pojedynczego klienta po stronie serwera.
 *
 * Rola klasy:
 * - opakowuje gniazdo sieciowe (Socket) jednego gracza,
 * - w osobnym wątku:
 *   * czyta linie tekstu od klienta,
 *   * przekazuje je do GameSession.handleClientMessage(...),
 * - udostępnia metodę sendLine(...) do wysyłania komunikatów do klienta.
 */
public final class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameSession session;
    private final PlayerColor color;

    // Ustawiane dopiero po starcie run()
    private volatile PrintWriter out;

    // Sygnał „gotowości” (czy out jest ustawione i można wysyłać)
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    public ClientHandler(Socket socket, GameSession session, PlayerColor color) {
        this.socket = socket;
        this.session = session;
        this.color = color;
    }

    public PlayerColor getColor() {
        return color;
    }

    /**
     * Czeka aż handler przygotuje strumień wyjściowy (out).
     * Dzięki temu GameServer może bezpiecznie wysłać WELCOME / INFO zaraz po połączeniu.
     */
    public boolean awaitReady(long timeoutMs) {
        try {
            return readyLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Wysyła jedną linię tekstu do klienta.
     * Jeśli out jeszcze nie jest gotowe, to po prostu nic nie wyśle (ale dzięki awaitReady
     * w GameServer do tego nie powinno dochodzić).
     */
    void sendLine(String line) {
        PrintWriter w = out;
        if (w != null) {
            w.println(line);
            w.flush();
        }
    }

    /**
     * Główna pętla wątku klienta:
     * - tworzy strumienie wejścia/wyjścia,
     * - ustawia out i sygnalizuje gotowość,
     * - wysyła INFO Connected as <color>,
     * - czyta linie od klienta i przekazuje je do GameSession.
     */
    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            // ustawiamy strumień wyjściowy, którego używa sendLine(...)
            this.out = writer;
            // sygnał gotowości do wysyłania
            readyLatch.countDown();

            // prosty komunikat informacyjny po połączeniu
            sendLine("INFO Connected as " + color.name());

            String line;
            while ((line = in.readLine()) != null) {
                session.handleClientMessage(this, line);
            }

            System.out.println("Client " + color + " disconnected (EOF).");
        } catch (IOException e) {
            System.out.println("Client " + color + " disconnected: " + e.getMessage());
        } finally {
            // na wypadek gdyby wyjątek był przed ustawieniem out
            readyLatch.countDown();
        }
    }
}
