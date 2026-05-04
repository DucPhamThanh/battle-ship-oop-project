package com.battleship.view;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import java.util.List;
import java.util.function.BiConsumer;

public class BoardRenderer {
    private final GridPane gridUI;
    private final boolean showShips;
    private BiConsumer<Integer, Integer> onCellClick;
    private BiConsumer<Integer, Integer> onCellHover;
    private Runnable onMouseExited;

    public BoardRenderer(GridPane gridUI, boolean showShips) {
        this.gridUI = gridUI;
        this.showShips = showShips;
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

    public void render(Board board) {
        gridUI.getChildren().clear();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                StackPane cell = new StackPane();
                cell.getStyleClass().add("cell");
                updateCellVisual(cell, board.getCellState(x, y), board, x, y);

                final int fx = x, fy = y;
                cell.setOnMouseClicked(e -> {
                    if (onCellClick != null) onCellClick.accept(fx, fy);
                });
                cell.setOnMouseEntered(e -> {
                    if (onCellHover != null) onCellHover.accept(fx, fy);
                });
                cell.setOnMouseExited(e -> {
                    if (onMouseExited != null) onMouseExited.run();
                });
                
                gridUI.add(cell, fx, fy);
            }
        }
    }

    public void refresh(Board board) {
        refresh(board, null);
    }

    public void refresh(Board board, List<int[]> highlights) {
        gridUI.getChildren().forEach(node -> {
            Integer nx = GridPane.getColumnIndex(node), ny = GridPane.getRowIndex(node);
            if (nx != null && ny != null) {
                updateCellVisual((StackPane) node, board.getCellState(nx, ny), board, nx, ny);
                if (highlights != null && highlights.stream().anyMatch(p -> p[0] == nx && p[1] == ny)) {
                    node.getStyleClass().add("cell-placement-selected");
                }
            }
        });
    }

    private void updateCellVisual(StackPane cell, CellState state, Board board, int x, int y) {
        // 1. Clear previous classes and reset rotation
        cell.getStyleClass().removeAll("cell-water", "cell-ship", "cell-hit", "cell-miss", "cell-sunk",
                "ship-battleship", "ship-destroyer", "ship-patrolboat", "ship-rescueship", "ship-plane",
                "ship-horizontal", "ship-vertical", "cell-placement-selected");
        cell.getStyleClass().removeIf(s -> s.startsWith("ship-part") || s.startsWith("ship-size"));
        cell.setRotate(0);

        // 2. Render Ship Layer (if visible or already hit/sunk)
        Ship ship = board.getShipAt(x, y);
        boolean shipVisible = ship != null && (showShips || state == CellState.HIT || state == CellState.SUNK);

        if (shipVisible) {
            cell.getStyleClass().add("ship-" + ship.getName().toLowerCase());
            cell.setRotate(ship.isVertical() ? 0 : -90);
            
            List<int[]> positions = ship.getPositions();
            for (int i = 0; i < positions.size(); i++) {
                if (positions.get(i)[0] == x && positions.get(i)[1] == y) {
                    cell.getStyleClass().add("ship-part-" + i);
                    cell.getStyleClass().add("ship-size-" + ship.getSize());
                    break;
                }
            }
        } else {
            cell.getStyleClass().add("cell-water");
        }

        // 3. Render Status Indicators (Hit/Miss/Sunk)
        switch (state) {
            case HIT: cell.getStyleClass().add("cell-hit"); break;
            case MISS: cell.getStyleClass().add("cell-miss"); break;
            case SUNK: cell.getStyleClass().add("cell-sunk"); break;
            default: break;
        }
    }
}
