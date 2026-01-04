package pl.edu.go.client.net;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * NetworkClient — prosta warstwa sieciowa klienta (GUI/CLI).
 *
 * - Utrzymuje połączenie TCP (Socket).
 * - Ma wątek czytający linie (read loop) i przekazuje je do callbacku onLine.
 * - Umożliwia wysyłanie linii sendLine().
 */
public final class NetworkClient {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private Thread readerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Consumer<String> onLine = s -> {};
    private Consumer<Exception> onError = e -> {};

    public void setOnLine(Consumer<String> onLine) {
        this.onLine = Objects.requireNonNull(onLine);
    }

    public void setOnError(Consumer<Exception> onError) {
        this.onError = Objects.requireNonNull(onError);
    }

    public synchronized void connect(String host, int port) throws IOException {
        if (isConnected()) {
            throw new IOException("Already connected");
        }

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        running.set(true);
        readerThread = new Thread(this::readLoop, "NetworkClient-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public synchronized void disconnect() {
        running.set(false);
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
            // ignore
        }
        socket = null;
        in = null;
        out = null;
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized void sendLine(String line) throws IOException {
        if (!isConnected() || out == null) {
            throw new IOException("Not connected");
        }
        out.write(line);
        out.write("\n");
        out.flush();
    }

    private void readLoop() {
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                onLine.accept(line);
            }
        } catch (Exception e) {
            if (running.get()) {
                onError.accept(e);
            }
        } finally {
            disconnect();
        }
    }
}
