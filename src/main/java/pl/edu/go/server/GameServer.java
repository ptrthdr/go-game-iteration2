package pl.edu.go.server;

import pl.edu.go.board.Board;
import pl.edu.go.board.BoardFactory;
import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Klasa GameServer — punkt startowy serwera gry.
 *
 * Rola klasy:
 * - tworzy obiekt Board i Game dla nowej partii,
 * - tworzy GameSession,
 * - otwiera ServerSocket na porcie 5001,
 * - akceptuje dwóch klientów (BLACK, WHITE),
 * - uruchamia osobne wątki ClientHandler,
 * - WAŻNE: czeka aż oba ClientHandler będą gotowe do wysyłania (out != null),
 * - dopiero potem wywołuje session.startGame().
 */
public final class GameServer {

    public static void main(String[] args) {
        int port = 5001;
        int boardSize = 9; // testowo 9x9

        Board board = BoardFactory.createBoard(boardSize);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            // pierwszy klient = BLACK
            Socket s1 = serverSocket.accept();
            System.out.println("First player connected (BLACK)");
            ClientHandler h1 = new ClientHandler(s1, session, PlayerColor.BLACK);
            session.setPlayer(PlayerColor.BLACK, h1);
            Thread t1 = new Thread(h1, "Client-BLACK");
            t1.start();

            // drugi klient = WHITE
            Socket s2 = serverSocket.accept();
            System.out.println("Second player connected (WHITE)");
            ClientHandler h2 = new ClientHandler(s2, session, PlayerColor.WHITE);
            session.setPlayer(PlayerColor.WHITE, h2);
            Thread t2 = new Thread(h2, "Client-WHITE");
            t2.start();

            // KLUCZOWE: czekamy aż oba handlery będą gotowe wysyłać (żeby nie zgubić WELCOME)
            boolean r1 = h1.awaitReady(2000);
            boolean r2 = h2.awaitReady(2000);
            if (!r1 || !r2) {
                System.out.println("WARNING: Some client handlers not ready in time. Starting game anyway.");
            }

            session.startGame();
            System.out.println("Game started. Waiting for moves...");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
