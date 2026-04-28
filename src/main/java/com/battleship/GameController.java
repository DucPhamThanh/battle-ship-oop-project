package com.battleship;

import com.battleship.logic.BotAI;
import com.battleship.logic.GameEngine;
import com.battleship.logic.PlacementManager;
import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class GameController {

    @FXML private GridPane playerBoardUI;
    @FXML private GridPane enemyBoardUI;
    @FXML private Label statusLabel;
    @FXML private Label playerCountLabel;
    @FXML private Label enemyCountLabel;

    private final GameEngine engine = new GameEngine();
    private final BotAI botAI = new BotAI();
    private final PlacementManager placementManager = new PlacementManager();
    
    private boolean placementMode = true;
    private static final double BOT_DELAY_SECONDS = 2.0;

    @FXML
    public void initialize() {
        resetGame();
    }

    private void resetGame() {
        engine.reset();
        botAI.reset();
        placementManager.reset();
        placementMode = true;

        renderBoard(playerBoardUI, engine.getPlayerBoard(), true);
        renderBoard(enemyBoardUI, engine.getEnemyBoard(), false);
        updateShipCounters();
        statusLabel.setText("Đặt tàu " + placementManager.getCurrentShipName() + " (Size " + placementManager.getCurrentShipSize() + "): Chọn ô đầu tiên.");
    }

    private void updateShipCounters() {
        long playerShips = engine.getPlayerBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        long enemyShips = engine.getEnemyBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        int totalShips = placementManager.getTotalShips();
        playerCountLabel.setText("Tàu sống: " + playerShips + " / " + totalShips);
        enemyCountLabel.setText("Tàu sống: " + enemyShips + " / " + totalShips);
    }

    private void renderBoard(GridPane gridUI, Board board, boolean showShips) {
        gridUI.getChildren().clear();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                StackPane cell = new StackPane();
                cell.getStyleClass().add("cell");
                updateCellVisual(cell, board.getCellState(x, y), showShips, board, x, y);

                final int fx = x, fy = y;
                if (!showShips) cell.setOnMouseClicked(e -> handlePlayerShot(fx, fy));
                else {
                    cell.setOnMouseEntered(e -> handlePlacementHover(fx, fy));
                    cell.setOnMouseExited(e -> refreshBoardUI(playerBoardUI, engine.getPlayerBoard(), true));
                    cell.setOnMouseClicked(e -> handlePlacementClick(fx, fy));
                }
                gridUI.add(cell, fx, fy);
            }
        }
    }

    private void handlePlacementHover(int x, int y) {
        // Hover preview removed at user request
    }

    private void handlePlacementClick(int x, int y) {
        if (!placementMode) return;

        if (placementManager.isValidNextCell(x, y, engine.getPlayerBoard())) {
            placementManager.addCell(x, y);
            
            if (placementManager.isShipComplete()) {
                Ship ship = placementManager.commitShip();
                for (int[] pos : ship.getPositions()) {
                    engine.getPlayerBoard().setCellState(pos[0], pos[1], CellState.SHIP);
                }
                engine.getPlayerBoard().getShips().add(ship);
                
                if (placementManager.allShipsPlaced()) {
                    placementMode = false;
                    statusLabel.setText("Đã xong! Lượt của bạn.");
                } else {
                    statusLabel.setText("Đặt tàu " + placementManager.getCurrentShipName() + " (Size " + placementManager.getCurrentShipSize() + "): Chọn ô đầu tiên.");
                }
            } else {
                statusLabel.setText("Đặt tàu " + placementManager.getCurrentShipName() + ": Còn " + placementManager.getRemainingCells() + " ô.");
            }
            refreshBoardUI(playerBoardUI, engine.getPlayerBoard(), true);
            updateShipCounters();
        } else {
            statusLabel.setText("Ô chọn không hợp lệ!");
        }
    }

    private void handlePlayerShot(int x, int y) {
        if (engine.isGameOver() || !engine.isPlayerTurn() || placementMode) return;

        CellState state = engine.getEnemyBoard().getCellState(x, y);
        if (state == CellState.HIT || state == CellState.MISS || state == CellState.SUNK) return;

        boolean hit = engine.processShot(engine.getEnemyBoard(), x, y);
        if (hit) {
            Ship ship = engine.getEnemyBoard().getShipAt(x, y);
            if (ship != null && ship.isSunk()) {
                engine.markShipAsSunk(engine.getEnemyBoard(), ship);
                updateShipCounters();
                statusLabel.setText("BẠN ĐÃ ĐÁNH CHÌM TÀU " + ship.getName().toUpperCase() + "!");
            } else statusLabel.setText("TRÚNG RỒI! Bạn được bắn tiếp.");
            
            refreshBoardUI(enemyBoardUI, engine.getEnemyBoard(), false);
            if (engine.getEnemyBoard().isAllSunk()) endGame("BẠN ĐÃ THẮNG!");
        } else {
            refreshBoardUI(enemyBoardUI, engine.getEnemyBoard(), false);
            statusLabel.setText("Trượt rồi. Lượt của Bot...");
            engine.setPlayerTurn(false);
            scheduleBotMove();
        }
    }

    private void scheduleBotMove() {
        PauseTransition pause = new PauseTransition(Duration.seconds(BOT_DELAY_SECONDS));
        pause.setOnFinished(e -> botMove());
        pause.play();
    }

    private void botMove() {
        if (engine.isGameOver()) return;

        int[] move = botAI.calculateNextMove();
        int x = move[0], y = move[1];
        boolean hit = engine.processShot(engine.getPlayerBoard(), x, y);

        if (hit) {
            botAI.recordHit(x, y);
            Ship ship = engine.getPlayerBoard().getShipAt(x, y);
            if (ship != null && ship.isSunk()) {
                engine.markShipAsSunk(engine.getPlayerBoard(), ship);
                botAI.notifyShipSunk();
                updateShipCounters();
                statusLabel.setText("Bot đã đánh chìm tàu của bạn!");
            }
            refreshBoardUI(playerBoardUI, engine.getPlayerBoard(), true);
            
            if (engine.getPlayerBoard().isAllSunk()) endGame("BOT ĐÃ THẮNG!");
            else scheduleBotMove();
        } else {
            refreshBoardUI(playerBoardUI, engine.getPlayerBoard(), true);
            engine.setPlayerTurn(true);
            statusLabel.setText("Lượt của bạn!");
        }
    }

    private void refreshBoardUI(GridPane gridUI, Board board, boolean showShips) {
        gridUI.getChildren().forEach(node -> {
            Integer nx = GridPane.getColumnIndex(node), ny = GridPane.getRowIndex(node);
            if (nx != null && ny != null) {
                updateCellVisual((StackPane) node, board.getCellState(nx, ny), showShips, board, nx, ny);
                if (showShips && placementManager.isAlreadySelected(nx, ny)) {
                    node.getStyleClass().add("cell-placement-selected");
                }
            }
        });
    }

    private void updateCellVisual(StackPane cell, CellState state, boolean showShips, Board board, int x, int y) {
        cell.getStyleClass().removeAll("cell-water", "cell-ship", "cell-hit", "cell-miss", "cell-sunk",
                "ship-carrier", "ship-battleship", "ship-cruiser", "ship-submarine", "ship-destroyer");

        switch (state) {
            case WATER: cell.getStyleClass().add("cell-water"); break;
            case SHIP:
                if (showShips) {
                    Ship ship = board.getShipAt(x, y);
                    cell.getStyleClass().add(ship != null ? "ship-" + ship.getName().toLowerCase() : "cell-ship");
                } else cell.getStyleClass().add("cell-water");
                break;
            case HIT: cell.getStyleClass().add("cell-hit"); break;
            case SUNK: cell.getStyleClass().add("cell-sunk"); break;
            case MISS: cell.getStyleClass().add("cell-miss"); break;
        }
    }

    private void endGame(String message) {
        engine.setGameOver(true);
        statusLabel.setText("GAME OVER: " + message);
    }

    @FXML private void onRestartClick() { resetGame(); }
    public void handleKeyPressed(javafx.scene.input.KeyEvent event) {}
}
