package com.battleship;

import com.battleship.logic.BotAI;
import com.battleship.logic.GameEngine;
import com.battleship.logic.GameEventListener;
import com.battleship.logic.PlacementManager;
import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import com.battleship.view.BoardRenderer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

public class GameController implements GameEventListener {

    @FXML
    private GridPane playerBoardUI;
    @FXML
    private GridPane enemyBoardUI;
    @FXML
    private Label statusLabel;
    @FXML
    private Label playerCountLabel;
    @FXML
    private Label enemyCountLabel;

    private final GameEngine engine = new GameEngine();
    private final BotAI botAI = new BotAI();
    private final PlacementManager placementManager = new PlacementManager();

    private BoardRenderer playerRenderer;
    private BoardRenderer enemyRenderer;

    private boolean placementMode = true;
    private static final double BOT_DELAY_SECONDS = 2.0;

    @FXML
    public void initialize() {
        engine.addListener(this);

        playerRenderer = new BoardRenderer(playerBoardUI, true);
        enemyRenderer = new BoardRenderer(enemyBoardUI, false);

        playerRenderer.setOnCellClick(this::handlePlacementClick);
        enemyRenderer.setOnCellClick(this::handlePlayerShot);

        // Setup hover for player board during placement (optional, keeping it clean)
        playerRenderer.setOnMouseExited(() -> {
            if (placementMode)
                playerRenderer.refresh(engine.getPlayerBoard(), placementManager.getCurrentSelection());
        });

        resetGame();
    }

    private void resetGame() {
        engine.reset();
        botAI.reset();
        placementManager.reset();
        placementMode = true;

        playerRenderer.render(engine.getPlayerBoard());
        enemyRenderer.render(engine.getEnemyBoard());

        updateShipCounters();
        updateStatus("Đặt tàu " + placementManager.getCurrentShipName() + " (Size "
                + placementManager.getCurrentShipSize() + "): Chọn ô đầu tiên.");
    }

    private void updateShipCounters() {
        long playerShips = engine.getPlayerBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        long enemyShips = engine.getEnemyBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        int totalShips = placementManager.getTotalShips();
        playerCountLabel.setText("Tàu sống: " + playerShips + " / " + totalShips);
        enemyCountLabel.setText("Tàu sống: " + enemyShips + " / " + totalShips);
    }

    private void handlePlacementClick(int x, int y) {
        if (!placementMode)
            return;

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
                    updateStatus("Đã xong! Lượt của bạn.");
                } else {
                    updateStatus("Đặt tàu " + placementManager.getCurrentShipName() + " (Size "
                            + placementManager.getCurrentShipSize() + "): Chọn ô đầu tiên.");
                }
            } else {
                updateStatus("Đặt tàu " + placementManager.getCurrentShipName() + ": Còn "
                        + placementManager.getRemainingCells() + " ô.");
            }
            playerRenderer.refresh(engine.getPlayerBoard(), placementManager.getCurrentSelection());
            updateShipCounters();
        } else {
            updateStatus("Ô chọn không hợp lệ!");
        }
    }

    private void handlePlayerShot(int x, int y) {
        if (engine.isGameOver() || !engine.isPlayerTurn() || placementMode)
            return;

        CellState state = engine.getEnemyBoard().getCellState(x, y);
        if (state == CellState.HIT || state == CellState.MISS || state == CellState.SUNK)
            return;

        boolean hit = engine.processShot(engine.getEnemyBoard(), x, y);
        if (!hit) {
            updateStatus("Trượt rồi. Lượt của Bot...");
            engine.setPlayerTurn(false);
            scheduleBotMove();
        } else {
            if (engine.getEnemyBoard().isAllSunk()) {
                engine.endGame("BẠN ĐÃ THẮNG!");
            }
        }
    }

    private void scheduleBotMove() {
        PauseTransition pause = new PauseTransition(Duration.seconds(BOT_DELAY_SECONDS));
        pause.setOnFinished(e -> botMove());
        pause.play();
    }

    private void botMove() {
        if (engine.isGameOver())
            return;

        int[] move = botAI.calculateNextMove();
        int x = move[0], y = move[1];
        boolean hit = engine.processShot(engine.getPlayerBoard(), x, y);

        if (hit) {
            botAI.recordHit(x, y);
            Ship ship = engine.getPlayerBoard().getShipAt(x, y);
            if (ship != null && ship.isSunk()) {
                botAI.notifyShipSunk();
            }

            if (engine.getPlayerBoard().isAllSunk()) {
                engine.endGame("BOT ĐÃ THẮNG!");
            } else {
                scheduleBotMove();
            }
        } else {
            engine.setPlayerTurn(true);
            updateStatus("Lượt của bạn!");
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    // GameEventListener Implementation
    @Override
    public void onShotProcessed(Board board, int x, int y, boolean hit) {
        if (board == engine.getPlayerBoard()) {
            playerRenderer.refresh(board);
        } else {
            enemyRenderer.refresh(board);
        }

        if (hit) {
            String shooter = (board == engine.getEnemyBoard()) ? "Bạn" : "Bot";
            updateStatus(shooter + " đã bắn trúng!");
        }
    }

    @Override
    public void onShipSunk(Board board, Ship ship) {
        updateShipCounters();
        String victim = (board == engine.getEnemyBoard()) ? "Bot" : "bạn";
        updateStatus("BẠN ĐÃ ĐÁNH CHÌM TÀU " + ship.getName().toUpperCase() + " CỦA " + victim.toUpperCase() + "!");
    }

    @Override
    public void onTurnChanged(boolean isPlayerTurn) {
        // Handled via onStatusChanged mostly
    }

    @Override
    public void onGameOver(String message) {
        updateStatus("GAME OVER: " + message);
    }

    @Override
    public void onStatusChanged(String message) {
        updateStatus(message);
    }

    @FXML
    private void onRestartClick() {
        resetGame();
    }

    public void handleKeyPressed(javafx.scene.input.KeyEvent event) {
    }
}
