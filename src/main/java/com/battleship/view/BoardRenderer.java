package com.battleship.view;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class BoardRenderer {
    private final GridPane gridUI;
    private final boolean showShips;
    private boolean revealShips;
    private BiConsumer<Integer, Integer> onCellClick;
    private BiConsumer<Integer, Integer> onCellHover;
    private Runnable onMouseExited;
    private final List<int[]> blockedCells = new ArrayList<>();

    public BoardRenderer(GridPane gridUI, boolean showShips) {
        this.gridUI = gridUI;
        this.showShips = showShips;
    }

    public void setRevealShips(boolean revealShips) {
        this.revealShips = revealShips;
    }

    public void setOnCellClick(BiConsumer<Integer, Integer> onCellClick) {
        this.onCellClick = onCellClick;
    }

    public void setOnCellHover(BiConsumer<Integer, Integer> onCellHover) {
        this.onCellHover = onCellHover;
    }

    public void setOnMouseExited(Runnable onMouseExited) {
        this.onMouseExited = onMouseExited;
    }

    public void setBlockedCells(List<int[]> cells) {
        blockedCells.clear();
        if (cells == null) {
            return;
        }

        for (int[] cell : cells) {
            blockedCells.add(new int[] { cell[0], cell[1] });
        }
    }

    public void render(Board board) {
        gridUI.getChildren().clear();
        double cellSize = getCellSize(board.getSize());
        gridUI.add(createAxisLabel("", cellSize), 0, 0);

        for (int x = 0; x < board.getSize(); x++) {
            gridUI.add(createAxisLabel(String.valueOf((char) ('A' + x)), cellSize), x + 1, 0);
        }

        for (int y = 0; y < board.getSize(); y++) {
            gridUI.add(createAxisLabel(String.valueOf(y + 1), cellSize), 0, y + 1);
        }

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                StackPane cell = new StackPane();
                applyCellSize(cell, board.getSize());
                cell.getStyleClass().add("cell");
                updateCellVisual(cell, board.getCellState(x, y), board, x, y);

                final int fx = x, fy = y;
                cell.setOnMouseClicked(e -> {
                    if (onCellClick != null)
                        onCellClick.accept(fx, fy);
                });
                cell.setOnMouseEntered(e -> {
                    playHover(cell, true);
                    if (onCellHover != null)
                        onCellHover.accept(fx, fy);
                });
                cell.setOnMouseExited(e -> {
                    playHover(cell, false);
                    if (onMouseExited != null)
                        onMouseExited.run();
                });

                cell.setUserData(new int[] { fx, fy });
                gridUI.add(cell, fx + 1, fy + 1);
            }
        }
    }

    private void applyCellSize(StackPane cell, int boardSize) {
        double cellSize = getCellSize(boardSize);
        cell.setMinSize(cellSize, cellSize);
        cell.setPrefSize(cellSize, cellSize);
        cell.setMaxSize(cellSize, cellSize);
        cell.setStyle("-fx-min-width: " + cellSize + "px; -fx-min-height: " + cellSize
                + "px; -fx-max-width: " + cellSize + "px; -fx-max-height: " + cellSize + "px;");
    }

    private double getCellSize(int boardSize) {
        if (boardSize >= 12) {
            return 24;
        }
        return boardSize <= 8 ? 32 : 28;
    }

    private Label createAxisLabel(String text, double cellSize) {
        Label label = new Label(text);
        label.getStyleClass().add("axis-label");
        label.setMinSize(cellSize, cellSize);
        label.setPrefSize(cellSize, cellSize);
        label.setMaxSize(cellSize, cellSize);
        return label;
    }

    public void refresh(Board board) {
        refresh(board, null);
    }

    public void refresh(Board board, List<int[]> highlights) {
        refresh(board, highlights, null, true);
    }

    public void refresh(Board board, List<int[]> selectedCells, List<int[]> previewCells, boolean previewValid) {
        gridUI.getChildren().forEach(node -> {
            if (!(node instanceof StackPane) || !(node.getUserData() instanceof int[])) {
                return;
            }

            int[] position = (int[]) node.getUserData();
            int nx = position[0];
            int ny = position[1];
            if (nx >= 0 && nx < board.getSize() && ny >= 0 && ny < board.getSize()) {
                updateCellVisual((StackPane) node, board.getCellState(nx, ny), board, nx, ny);
                if (selectedCells != null && selectedCells.stream().anyMatch(p -> p[0] == nx && p[1] == ny)) {
                    node.getStyleClass().add("cell-placement-selected");
                }
                if (previewCells != null && previewCells.stream().anyMatch(p -> p[0] == nx && p[1] == ny)) {
                    node.getStyleClass().add(previewValid ? "cell-placement-valid" : "cell-placement-invalid");
                }
            }
        });
    }

    public void animateShot(int x, int y, boolean hit) {
        StackPane cell = getCell(x, y);
        if (cell == null) {
            return;
        }

        if (hit) {
            playBlast(cell);
            playSparks(cell);
            playFloatingText(cell, "HIT", "floating-hit");
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(110), cell);
            scaleUp.setToX(1.22);
            scaleUp.setToY(1.22);
            scaleUp.setCycleCount(2);
            scaleUp.setAutoReverse(true);
            scaleUp.play();
        } else {
            playRipple(cell);
            playDroplets(cell);
            playFloatingText(cell, "MISS", "floating-miss");
            FadeTransition fade = new FadeTransition(Duration.millis(130), cell);
            fade.setFromValue(0.45);
            fade.setToValue(1.0);
            fade.setCycleCount(2);
            fade.setAutoReverse(true);
            fade.play();
        }
    }

    public void animateShipSunk(Ship ship) {
        for (int[] pos : ship.getPositions()) {
            StackPane cell = getCell(pos[0], pos[1]);
            if (cell == null) {
                continue;
            }

            playBlast(cell);
            playSparks(cell);
            playFloatingText(cell, "SUNK", "floating-sunk");
            ScaleTransition pulse = new ScaleTransition(Duration.millis(180), cell);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.16);
            pulse.setToY(1.16);
            pulse.setCycleCount(4);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    private void playBlast(StackPane cell) {
        Circle blast = new Circle(5);
        blast.setMouseTransparent(true);
        blast.setFill(Color.rgb(248, 113, 113, 0.78));
        blast.setStroke(Color.rgb(254, 202, 202, 0.95));
        blast.setStrokeWidth(1.4);
        cell.getChildren().add(blast);

        ScaleTransition scale = new ScaleTransition(Duration.millis(360), blast);
        scale.setFromX(0.35);
        scale.setFromY(0.35);
        scale.setToX(3.5);
        scale.setToY(3.5);

        FadeTransition fade = new FadeTransition(Duration.millis(360), blast);
        fade.setFromValue(0.95);
        fade.setToValue(0);

        ParallelTransition animation = new ParallelTransition(scale, fade);
        animation.setOnFinished(e -> cell.getChildren().remove(blast));
        animation.play();
    }

    private void playSparks(StackPane cell) {
        for (int i = 0; i < 8; i++) {
            Circle spark = new Circle(i % 2 == 0 ? 2.2 : 1.6);
            spark.setMouseTransparent(true);
            spark.setFill(i % 2 == 0 ? Color.rgb(254, 240, 138, 0.95) : Color.rgb(248, 113, 113, 0.88));
            cell.getChildren().add(spark);

            double angle = (Math.PI * 2 * i) / 8.0;
            double distance = 16 + (i % 3) * 4;
            TranslateTransition move = new TranslateTransition(Duration.millis(420), spark);
            move.setByX(Math.cos(angle) * distance);
            move.setByY(Math.sin(angle) * distance);

            FadeTransition fade = new FadeTransition(Duration.millis(420), spark);
            fade.setFromValue(1);
            fade.setToValue(0);

            ParallelTransition animation = new ParallelTransition(move, fade);
            animation.setOnFinished(e -> cell.getChildren().remove(spark));
            animation.play();
        }
    }

    private void playRipple(StackPane cell) {
        Circle ripple = new Circle(6);
        ripple.setMouseTransparent(true);
        ripple.setFill(Color.TRANSPARENT);
        ripple.setStroke(Color.rgb(125, 211, 252, 0.95));
        ripple.setStrokeWidth(2);
        cell.getChildren().add(ripple);

        ScaleTransition scale = new ScaleTransition(Duration.millis(320), ripple);
        scale.setFromX(0.45);
        scale.setFromY(0.45);
        scale.setToX(2.6);
        scale.setToY(2.6);

        FadeTransition fade = new FadeTransition(Duration.millis(320), ripple);
        fade.setFromValue(0.9);
        fade.setToValue(0);

        ParallelTransition animation = new ParallelTransition(scale, fade);
        animation.setOnFinished(e -> cell.getChildren().remove(ripple));
        animation.play();

        Circle secondRipple = new Circle(4);
        secondRipple.setMouseTransparent(true);
        secondRipple.setFill(Color.TRANSPARENT);
        secondRipple.setStroke(Color.rgb(186, 230, 253, 0.7));
        secondRipple.setStrokeWidth(1.4);
        cell.getChildren().add(secondRipple);

        ScaleTransition scale2 = new ScaleTransition(Duration.millis(450), secondRipple);
        scale2.setFromX(0.3);
        scale2.setFromY(0.3);
        scale2.setToX(3.2);
        scale2.setToY(3.2);

        FadeTransition fade2 = new FadeTransition(Duration.millis(450), secondRipple);
        fade2.setFromValue(0.7);
        fade2.setToValue(0);

        ParallelTransition animation2 = new ParallelTransition(scale2, fade2);
        animation2.setOnFinished(e -> cell.getChildren().remove(secondRipple));
        animation2.play();
    }

    private void playDroplets(StackPane cell) {
        for (int i = 0; i < 5; i++) {
            Circle droplet = new Circle(1.5);
            droplet.setMouseTransparent(true);
            droplet.setFill(Color.rgb(186, 230, 253, 0.85));
            cell.getChildren().add(droplet);

            double angle = (-Math.PI / 2) + (i - 2) * 0.45;
            TranslateTransition move = new TranslateTransition(Duration.millis(360), droplet);
            move.setByX(Math.cos(angle) * (10 + i * 2));
            move.setByY(Math.sin(angle) * (12 + i * 2));

            FadeTransition fade = new FadeTransition(Duration.millis(360), droplet);
            fade.setFromValue(0.9);
            fade.setToValue(0);

            ParallelTransition animation = new ParallelTransition(move, fade);
            animation.setOnFinished(e -> cell.getChildren().remove(droplet));
            animation.play();
        }
    }

    private void playFloatingText(StackPane cell, String text, String styleClass) {
        Label label = new Label(text);
        label.setMouseTransparent(true);
        label.getStyleClass().addAll("floating-shot-text", styleClass);
        cell.getChildren().add(label);

        TranslateTransition move = new TranslateTransition(Duration.millis(520), label);
        move.setFromY(-6);
        move.setToY(-28);

        FadeTransition fade = new FadeTransition(Duration.millis(520), label);
        fade.setFromValue(1);
        fade.setToValue(0);

        ParallelTransition animation = new ParallelTransition(move, fade);
        animation.setOnFinished(e -> cell.getChildren().remove(label));
        animation.play();
    }

    private void playHover(StackPane cell, boolean entered) {
        ScaleTransition hover = new ScaleTransition(Duration.millis(90), cell);
        hover.setToX(entered ? 1.08 : 1.0);
        hover.setToY(entered ? 1.08 : 1.0);
        hover.play();
    }

    private StackPane getCell(int x, int y) {
        for (javafx.scene.Node node : gridUI.getChildren()) {
            if (!(node instanceof StackPane) || !(node.getUserData() instanceof int[])) {
                continue;
            }

            int[] position = (int[]) node.getUserData();
            if (position[0] == x && position[1] == y) {
                return (StackPane) node;
            }
        }
        return null;
    }

    private void updateCellVisual(StackPane cell, CellState state, Board board, int x, int y) {
        cell.getChildren().clear();
        cell.getStyleClass().removeIf(s -> s.startsWith("ship-") || s.startsWith("cell-"));
        cell.setRotate(0);
        cell.setScaleX(1.0);
        cell.setScaleY(1.0);
        cell.setOpacity(1.0);

        Ship ship = board.getShipAt(x, y);
        boolean shipVisible = ship != null && (showShips || revealShips || state == CellState.HIT || state == CellState.SUNK);

        if (shipVisible) {
            cell.getStyleClass().add("ship-size-" + ship.getSize());
            String shipClass = ship.getName().toLowerCase().replace("tàu_", "").replace("_", "-");
            cell.getStyleClass().add("ship-" + shipClass);
            cell.getStyleClass().add(ship.isVertical() ? "ship-vertical" : "ship-horizontal");
        } else {
            cell.getStyleClass().add("cell-water");
        }

        if (isBlocked(x, y)) {
            cell.getStyleClass().add("cell-shielded");
        }

        // 3. Render Status Indicators (Hit/Miss/Sunk)
        switch (state) {
            case HIT:
                cell.getStyleClass().add("cell-hit");
                addMarker(cell, "!", "hit-marker");
                break;
            case MISS:
                cell.getStyleClass().add("cell-miss");
                addMarker(cell, "x", "miss-marker");
                break;
            case SUNK:
                cell.getStyleClass().add("cell-sunk");
                addMarker(cell, "X", "sunk-marker");
                break;
            default:
                break;
        }
    }

    private void addMarker(StackPane cell, String text, String styleClass) {
        Label marker = new Label(text);
        marker.setMouseTransparent(true);
        marker.getStyleClass().addAll("shot-marker", styleClass);
        cell.getChildren().add(marker);
    }

    private boolean isBlocked(int x, int y) {
        return blockedCells.stream().anyMatch(cell -> cell[0] == x && cell[1] == y);
    }
}
