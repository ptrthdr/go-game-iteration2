/**
 * Klasa BoardView — widok planszy dla klienta GUI.
 *
 * Wzorzec:
 * - MVC (View):
 *   - Odpowiada za graficzną reprezentację planszy i kamieni.
 *
 * Rola klasy:
 * - rysowanie siatki i kamieni na ekranie,
 * - przekazywanie informacji o kliknięciach (x, y) do kontrolera.
 */

package pl.edu.go.client.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * BoardView — Canvas rysujący planszę i kamienie.
 *
 * Kliknięcie wylicza współrzędne (x,y) i przekazuje do handlera.
 */
public final class BoardView extends Canvas {

    public interface IntersectionClickHandler {
        void onClick(int x, int y);
    }

    private final GameModel model;
    private IntersectionClickHandler clickHandler = (x, y) -> {};

    public BoardView(GameModel model, double width, double height) {
        super(width, height);
        this.model = model;

        setOnMouseClicked(this::handleClick);
        redraw();
    }

    public void setClickHandler(IntersectionClickHandler clickHandler) {
        this.clickHandler = clickHandler != null ? clickHandler : (x, y) -> {};
    }

    public void redraw() {
        GraphicsContext g = getGraphicsContext2D();

        int size = model.getBoardSize();
        if (size <= 1) return;

        double w = getWidth();
        double h = getHeight();

        g.setFill(Color.BEIGE);
        g.fillRect(0, 0, w, h);

        double pad = 20.0;
        double gridW = w - 2 * pad;
        double gridH = h - 2 * pad;

        double stepX = gridW / (size - 1);
        double stepY = gridH / (size - 1);

        // grid
        g.setStroke(Color.BLACK);
        for (int i = 0; i < size; i++) {
            double x = pad + i * stepX;
            double y = pad + i * stepY;

            g.strokeLine(pad, y, pad + gridW, y);
            g.strokeLine(x, pad, x, pad + gridH);
        }

        // stones
        int[][] board = model.getBoard();
        double stoneR = Math.min(stepX, stepY) * 0.2;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int cell = board[x][y];
                if (cell == GameModel.EMPTY) continue;

                double cx = pad + x * stepX;
                double cy = pad + y * stepY;

                if (cell == GameModel.BLACK) {
                    g.setFill(Color.BLACK);
                    g.fillOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                    g.setStroke(Color.BLACK);
                    g.strokeOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                } else if (cell == GameModel.WHITE) {
                    g.setFill(Color.WHITE);
                    g.fillOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                    g.setStroke(Color.BLACK);
                    g.strokeOval(cx - stoneR, cy - stoneR, 2 * stoneR, 2 * stoneR);
                }
            }
        }
    }

    private void handleClick(MouseEvent e) {
        int size = model.getBoardSize();
        if (size <= 1) return;

        double w = getWidth();
        double h = getHeight();

        double pad = 20.0;
        double gridW = w - 2 * pad;
        double gridH = h - 2 * pad;

        double stepX = gridW / (size - 1);
        double stepY = gridH / (size - 1);

        double mx = e.getX();
        double my = e.getY();

        int x = (int) Math.round((mx - pad) / stepX);
        int y = (int) Math.round((my - pad) / stepY);

        if (x < 0 || y < 0 || x >= size || y >= size) return;

        clickHandler.onClick(x, y);
    }
}
