package pl.edu.go.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pl.edu.go.client.net.NetworkClient;

/**
 * GuiClientApp — klient okienkowy (JavaFX) do gry Go.
 *
 * Uruchamianie:
 * mvn javafx:run
 */
public final class GuiClientApp extends Application {

    private final GameModel model = new GameModel();
    private final NetworkClient net = new NetworkClient();

    private BoardView boardView;
    private GameController controller;

    private TextArea logArea;
    private Label statusLabel;
    private Label turnLabel;
    private Label colorLabel;

    private Button passBtn;
    private Button resignBtn;

    @Override
    public void start(Stage stage) {
        controller = new GameController(net, model);

        // UI: connect bar
        TextField hostField = new TextField("localhost");
        hostField.setPrefColumnCount(12);

        TextField portField = new TextField("5001");
        portField.setPrefColumnCount(6);

        Button connectBtn = new Button("Connect");
        Button disconnectBtn = new Button("Disconnect");
        disconnectBtn.setDisable(true);

        HBox connectBar = new HBox(8, new Label("Host:"), hostField, new Label("Port:"), portField, connectBtn, disconnectBtn);
        connectBar.setPadding(new Insets(10));

        // labels
        statusLabel = new Label("Disconnected");
        colorLabel = new Label("Color: -");
        turnLabel = new Label("Turn: -");

        VBox infoBox = new VBox(6, statusLabel, colorLabel, turnLabel);
        infoBox.setPadding(new Insets(10));

        // board
        boardView = new BoardView(model, 560, 560);
        boardView.setClickHandler(controller::onIntersectionClicked);

        // buttons
        passBtn = new Button("PASS");
        resignBtn = new Button("RESIGN");

        passBtn.setMaxWidth(Double.MAX_VALUE);
        resignBtn.setMaxWidth(Double.MAX_VALUE);

        passBtn.setOnAction(e -> controller.sendPass());
        resignBtn.setOnAction(e -> controller.sendResign());

        VBox actionsBox = new VBox(8, passBtn, resignBtn);
        actionsBox.setPadding(new Insets(10));
        actionsBox.setFillWidth(true);

        // log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);

        VBox rightPane = new VBox(10, infoBox, actionsBox, new Label("Log:"), logArea);
        rightPane.setPadding(new Insets(10));
        rightPane.setPrefWidth(320);

        BorderPane root = new BorderPane();
        root.setTop(connectBar);
        root.setCenter(boardView);
        root.setRight(rightPane);

        Scene scene = new Scene(root, 920, 650);
        stage.setTitle("Go Game - GUI Client");
        stage.setScene(scene);
        stage.show();

        // model -> UI refresh
        model.addListener(() -> Platform.runLater(this::refreshUI));

        // network callbacks
        net.setOnLine(model::acceptServerLine);
        net.setOnError(ex -> {
            model.addLog("[ERROR] " + ex.getMessage());
            Platform.runLater(() -> {
                statusLabel.setText("Disconnected (error)");
                connectBtn.setDisable(false);
                disconnectBtn.setDisable(true);
            });
        });

        connectBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            int port;

            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException nfe) {
                statusLabel.setText("Invalid port");
                model.addLog("[ERROR] Invalid port: " + portField.getText());
                return;
            }

            try {
                net.connect(host, port);
                statusLabel.setText("Connected: " + host + ":" + port);
                connectBtn.setDisable(true);
                disconnectBtn.setDisable(false);
                model.addLog("Connected to " + host + ":" + port);
                refreshUI();
            } catch (Exception ex) {
                statusLabel.setText("Connect failed: " + ex.getClass().getSimpleName());
                model.addLog("[ERROR] Connect failed: " + ex.getMessage());
                connectBtn.setDisable(false);
                disconnectBtn.setDisable(true);
            }
        });

        disconnectBtn.setOnAction(e -> {
            net.disconnect();
            statusLabel.setText("Disconnected");
            model.addLog("Disconnected.");
            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);
            refreshUI();
        });

        refreshUI();
    }

    private void refreshUI() {
        boardView.redraw();

        if (model.getMyColor() != null) {
            colorLabel.setText("Color: " + model.getMyColor());
        } else {
            colorLabel.setText("Color: -");
        }

        if (model.getCurrentTurn() != null) {
            turnLabel.setText("Turn: " + model.getCurrentTurn());
        } else {
            turnLabel.setText("Turn: -");
        }

        logArea.setText(model.getLogText());
        logArea.positionCaret(logArea.getLength());

        boolean connected = net.isConnected();
        boolean finished = model.isFinished();

        // PASS tylko gdy jesteś połączony, gra trwa i jest Twoja tura
        passBtn.setDisable(!connected || finished || !model.canPlayNow());

        // RESIGN: dozwolone zawsze gdy połączony i gra nie jest zakończona
        resignBtn.setDisable(!connected || finished);

        if (finished) {
            statusLabel.setText("Finished: " + model.getEndMessage());
        }
    }

    @Override
    public void stop() {
        net.disconnect();
    }
}
