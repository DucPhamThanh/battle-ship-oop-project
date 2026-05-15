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

import java.util.List;

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

    public void setDifficulty(BotAI.Difficulty difficulty) {
        botAI.setDifficulty(difficulty);
    }

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
            delayBotMove();
        if (hit) {
            if (engine.getEnemyBoard().isAllSunk()) {
                engine.endGame("BẠN ĐÃ THẮNG!");
            }
        }
    }

    private void delayBotMove() {
        PauseTransition pause = new PauseTransition(Duration.seconds(BOT_DELAY_SECONDS));
        pause.setOnFinished(e -> botMove());
        pause.play();
    }

    private int secondChange = 0;

    private void botMove() {
        if (engine.isGameOver())
            return;

        int[] move = botAI.calculateNextMove();
        int x = move[0];
        int y = move[1];
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
                delayBotMove();
            }
        } else {
            // Logic cho chế độ HARD: Được bắn 2 lượt (cho phép trượt 1 lần)
            if (botAI.getDifficulty() == BotAI.Difficulty.HARD && secondChange < 1) {
                secondChange++;
                updateStatus("Bot trượt! Còn 1 lượt...");
                delayBotMove();
            } else {
                secondChange = 0; // Reset cho lượt sau
                engine.setPlayerTurn(true);
                updateStatus("Lượt của bạn!");
            }
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

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
        if (board == engine.getEnemyBoard()) {
            updateStatus("BẠN ĐÃ ĐÁNH CHÌM TÀU " + ship.getName().toUpperCase() + "! " +
                    "Mời tiếp tục.");
        } else {
            updateStatus("Tàu " + ship.getName().toUpperCase() + " của bạn đã bị đánh chìm!");
        }
    }

    @Override
    public void onTurnChanged(boolean isPlayerTurn) {
        if (isPlayerTurn)
            updateStatus("Lượt của bạn!");
        else
            updateStatus("Lượt của Bot...");
    }

    @Override
    public void onGameOver(String message) {
        updateStatus("GAME OVER: " + message);
    }

    @Override
    public void onStatusChanged(String message) {
        updateStatus(message);
    }

    private void endGame(String message) {
        engine.setGameOver(true);
        statusLabel.setText("GAME OVER: " + message);
    }

    @FXML
    private void onRestartClick() {
        resetGame();
    }

    @FXML
    private void onBackToMenu(javafx.event.ActionEvent event) throws java.io.IOException {
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(App.class.getResource("menu-view.fxml"));
        javafx.scene.Scene scene = new javafx.scene.Scene(fxmlLoader.load(), 1000, 700);
        stage.setScene(scene);
    }

}
