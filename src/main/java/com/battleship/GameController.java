package com.battleship;

import com.battleship.logic.BotAI;
import com.battleship.logic.GameEngine;
import com.battleship.logic.GameEventListener;
import com.battleship.logic.GameRules;
import com.battleship.logic.PlacementManager;
import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import com.battleship.view.BoardRenderer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.awt.Toolkit;
import java.util.ArrayList;
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
    @FXML
    private TextArea battleLogArea;
    @FXML
    private Label turnStatsLabel;
    @FXML
    private Label accuracyLabel;
    @FXML
    private Button radarButton;
    @FXML
    private Button missileButton;
    @FXML
    private Button repairButton;
    @FXML
    private Button shieldButton;

    private final GameEngine engine = new GameEngine();
    private final BotAI botAI = new BotAI();
    private final PlacementManager placementManager = new PlacementManager();

    private BoardRenderer playerRenderer;
    private BoardRenderer enemyRenderer;

    private enum ActiveSkill {
        NONE, RADAR, MISSILE, SHIELD
    }

    private boolean placementMode = true;
    private static final double BOT_DELAY_SECONDS = 1.0;
    private int playerShots;
    private int playerHits;
    private int botShots;
    private int botHits;
    private int playerShipsSunk;
    private int botShipsSunk;
    private long gameStartMillis;
    private boolean gameOverDialogShown;
    private boolean radarAvailable;
    private boolean missileAvailable;
    private boolean repairAvailable;
    private boolean shieldAvailable;
    private ActiveSkill activeSkill = ActiveSkill.NONE;
    private int botShotsRemaining;
    private int shieldTurnsRemaining;
    private final List<int[]> shieldCells = new ArrayList<>();
    private final List<int[]> radarCells = new ArrayList<>();

    public void setDifficulty(BotAI.Difficulty difficulty) {
        botAI.setDifficulty(difficulty);
        if (playerRenderer != null && enemyRenderer != null) {
            resetGame();
        }
    }

    @FXML
    public void initialize() {
        engine.addListener(this);

        playerRenderer = new BoardRenderer(playerBoardUI, true);
        enemyRenderer = new BoardRenderer(enemyBoardUI, false);

        playerRenderer.setOnCellClick(this::handlePlayerBoardClick);
        playerRenderer.setOnCellHover(this::handlePlayerBoardHover);
        enemyRenderer.setOnCellClick(this::handlePlayerShot);
        enemyRenderer.setOnCellHover(this::handleEnemyBoardHover);
        enemyRenderer.setOnMouseExited(() -> {
            if (activeSkill == ActiveSkill.RADAR || activeSkill == ActiveSkill.MISSILE) {
                enemyRenderer.refresh(engine.getEnemyBoard(), radarCells);
            }
        });

        playerRenderer.setOnMouseExited(() -> {
            if (placementMode || activeSkill == ActiveSkill.SHIELD)
                playerRenderer.refresh(engine.getPlayerBoard());
        });
        playerBoardUI.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.R) {
                        rotateCurrentShip();
                    }
                });
            }
        });

        resetGame();
    }

    private void resetGame() {
        engine.reset(botAI.getDifficulty());
        botAI.reset();
        placementManager.configureForDifficulty(botAI.getDifficulty());
        placementMode = true;
        activeSkill = ActiveSkill.NONE;
        radarAvailable = true;
        missileAvailable = true;
        repairAvailable = true;
        shieldAvailable = true;
        shieldTurnsRemaining = 0;
        shieldCells.clear();
        radarCells.clear();
        botAI.setBlockedCells(shieldCells);
        botShotsRemaining = 0;
        playerShots = 0;
        playerHits = 0;
        botShots = 0;
        botHits = 0;
        playerShipsSunk = 0;
        botShipsSunk = 0;
        gameStartMillis = System.currentTimeMillis();
        gameOverDialogShown = false;

        enemyRenderer.setRevealShips(false);
        playerRenderer.setBlockedCells(shieldCells);
        playerRenderer.render(engine.getPlayerBoard());
        enemyRenderer.render(engine.getEnemyBoard());

        battleLogArea.clear();
        addLog("Ván mới: " + GameRules.getDisplayName(botAI.getDifficulty()) + " | Map "
                + engine.getPlayerBoard().getSize() + "x" + engine.getPlayerBoard().getSize()
                + " | Bot bắn " + GameRules.getBotShotsPerTurn(botAI.getDifficulty()) + " phát/lượt.");
        updateShipCounters();
        updateStats();
        updatePlacementStatus();
        updateSkillButtons();
    }

    private void updateShipCounters() {
        long playerShips = engine.getPlayerBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        long enemyShips = engine.getEnemyBoard().getShips().stream().filter(s -> !s.isSunk()).count();
        int totalShips = placementManager.getTotalShips();
        playerCountLabel.setText("Tàu sống: " + playerShips + " / " + totalShips);
        enemyCountLabel.setText("Tàu sống: " + enemyShips + " / " + totalShips);
    }

    private void handlePlayerBoardClick(int x, int y) {
        if (placementMode) {
            handlePlacementClick(x, y);
        } else if (activeSkill == ActiveSkill.SHIELD) {
            useShield(x, y);
        }
    }

    private void handlePlayerBoardHover(int x, int y) {
        if (placementMode) {
            handlePlacementHover(x, y);
        } else if (activeSkill == ActiveSkill.SHIELD) {
            handleShieldHover(x, y);
        }
    }

    private void handleEnemyBoardHover(int x, int y) {
        if (engine.isGameOver() || !engine.isPlayerTurn() || placementMode) {
            return;
        }

        if (activeSkill == ActiveSkill.RADAR) {
            enemyRenderer.refresh(engine.getEnemyBoard(), collectRadarCells(x, y));
        } else if (activeSkill == ActiveSkill.MISSILE) {
            enemyRenderer.refresh(engine.getEnemyBoard(), collectMissilePreviewCells(x, y));
        }
    }

    private void handlePlacementClick(int x, int y) {
        if (!placementMode)
            return;

        if (placementManager.allShipsPlaced()) {
            updatePlacementStatus();
            return;
        }

        Ship ship = placementManager.placeCurrentShip(engine.getPlayerBoard(), x, y);
        if (ship == null) {
            updateStatus("Ô chọn không hợp lệ!");
            handlePlacementHover(x, y);
            return;
        }

        playerRenderer.refresh(engine.getPlayerBoard());
        updateShipCounters();
        addLog("Bạn đặt " + ship.getName() + " " + placementManager.getOrientationName() + ".");

        if (placementManager.allShipsPlaced()) {
            updatePlacementStatus();
            addLog("Bạn đã đặt đủ tàu. Bấm XÁC NHẬN để bắt đầu.");
        } else {
            updatePlacementStatus();
        }
    }

    private void handlePlacementHover(int x, int y) {
        if (!placementMode || placementManager.allShipsPlaced()) {
            return;
        }

        List<int[]> previewCells = placementManager.getPreviewCells(x, y);
        boolean previewValid = placementManager.areCellsPlaceable(engine.getPlayerBoard(), previewCells);
        playerRenderer.refresh(engine.getPlayerBoard(), null, previewCells, previewValid);
    }

    private void handleShieldHover(int x, int y) {
        if (!shieldAvailable || engine.isGameOver() || !engine.isPlayerTurn()) {
            return;
        }

        int shieldSize = GameRules.getShieldSize(botAI.getDifficulty());
        List<int[]> previewCells = collectShieldCells(x, y, shieldSize);
        playerRenderer.refresh(engine.getPlayerBoard(), previewCells);
    }

    private void updatePlacementStatus() {
        if (placementManager.allShipsPlaced()) {
            updateStatus("Đã đặt đủ tàu. Có thể AUTO ĐẶT TÀU lại hoặc bấm XÁC NHẬN để bắt đầu.");
            return;
        }

        updateStatus("Đặt tàu " + placementManager.getCurrentShipName() + " (Size "
                + placementManager.getCurrentShipSize() + ", " + placementManager.getOrientationName()
                + "): chọn vị trí.");
    }

    private void handlePlayerShot(int x, int y) {
        if (engine.isGameOver() || !engine.isPlayerTurn() || placementMode)
            return;

        if (activeSkill != ActiveSkill.NONE) {
            handleSpecialTargetClick(x, y);
            return;
        }

        CellState state = engine.getEnemyBoard().getCellState(x, y);
        if (state == CellState.HIT || state == CellState.MISS || state == CellState.SUNK)
            return;

        boolean hit = engine.processShot(engine.getEnemyBoard(), x, y);
        if (!hit) {
            updateStatus("Trượt rồi. Lượt của Bot...");
            engine.setPlayerTurn(false);
            startBotTurn();
        } else {
            if (engine.getEnemyBoard().isAllSunk()) {
                engine.endGame("BẠN ĐÃ THẮNG!");
            }
        }
        updateSkillButtons();
    }

    private void handleSpecialTargetClick(int x, int y) {
        if (activeSkill == ActiveSkill.RADAR) {
            useRadar(x, y);
        } else if (activeSkill == ActiveSkill.MISSILE) {
            useMissile(x, y);
        } else if (activeSkill == ActiveSkill.SHIELD) {
            updateStatus("Chọn vùng chặn trên bản đồ của bạn.");
        }
    }

    private void useRadar(int centerX, int centerY) {
        int signalCount = 0;
        Board enemyBoard = engine.getEnemyBoard();
        radarCells.clear();
        radarCells.addAll(collectRadarCells(centerX, centerY));
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) {
                if (!isInBounds(enemyBoard, x, y)) {
                    continue;
                }
                CellState state = enemyBoard.getCellState(x, y);
                if (state == CellState.SHIP || state == CellState.HIT) {
                    signalCount++;
                }
            }
        }

        radarAvailable = false;
        activeSkill = ActiveSkill.NONE;
        String result = signalCount > 0 ? "có tín hiệu tàu" : "không có tín hiệu";
        enemyRenderer.refresh(enemyBoard, radarCells);
        updateStatus("Radar " + toCoordinate(centerX, centerY) + ": " + result + ".");
        addLog("Radar quét vùng 3x3 tại " + toCoordinate(centerX, centerY) + ": " + result + ".");
        updateSkillButtons();
    }

    private List<int[]> collectRadarCells(int centerX, int centerY) {
        List<int[]> cells = new ArrayList<>();
        Board enemyBoard = engine.getEnemyBoard();
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) {
                if (isInBounds(enemyBoard, x, y)) {
                    cells.add(new int[] { x, y });
                }
            }
        }
        return cells;
    }

    private void useMissile(int startX, int startY) {
        List<int[]> targets = collectMissileTargets(startX, startY);
        if (targets.isEmpty()) {
            updateStatus("Vùng missile đã bị bắn hết. Chọn vùng khác.");
            return;
        }

        missileAvailable = false;
        activeSkill = ActiveSkill.NONE;
        addLog("Bạn dùng Missile 2x2 tại " + toCoordinate(startX, startY) + ".");

        boolean anyHit = false;
        for (int[] target : targets) {
            boolean hit = engine.processShot(engine.getEnemyBoard(), target[0], target[1]);
            anyHit = anyHit || hit;
            if (engine.getEnemyBoard().isAllSunk()) {
                engine.endGame("BẠN ĐÃ THẮNG!");
                updateSkillButtons();
                return;
            }
        }

        if (anyHit) {
            updateStatus("Missile trúng mục tiêu. Bạn được bắn tiếp!");
            updateSkillButtons();
        } else {
            engine.setPlayerTurn(false);
            startBotTurn();
        }
    }

    private List<int[]> collectMissileTargets(int startX, int startY) {
        List<int[]> targets = new ArrayList<>();
        Board enemyBoard = engine.getEnemyBoard();
        for (int x = startX; x <= startX + 1; x++) {
            for (int y = startY; y <= startY + 1; y++) {
                if (isInBounds(enemyBoard, x, y) && !isTargeted(enemyBoard, x, y)) {
                    targets.add(new int[] { x, y });
                }
            }
        }
        return targets;
    }

    private List<int[]> collectMissilePreviewCells(int startX, int startY) {
        List<int[]> cells = new ArrayList<>();
        Board enemyBoard = engine.getEnemyBoard();
        for (int x = startX; x <= startX + 1; x++) {
            for (int y = startY; y <= startY + 1; y++) {
                if (isInBounds(enemyBoard, x, y)) {
                    cells.add(new int[] { x, y });
                }
            }
        }
        return cells;
    }

    private void useRepair() {
        Board board = engine.getPlayerBoard();
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                if (board.getCellState(x, y) != CellState.HIT) {
                    continue;
                }

                Ship ship = board.getShipAt(x, y);
                if (ship == null || ship.isSunk()) {
                    continue;
                }

                ship.repair();
                board.setCellState(x, y, CellState.SHIP);
                botAI.forgetShot(x, y);
                repairAvailable = false;
                playerRenderer.refresh(board);
                updateSkillButtons();
                addLog("Repair: hồi 1 phần tàu tại " + toCoordinate(x, y) + ".");
                updateStatus("Đã repair 1 phần tàu. Bạn vẫn được bắn.");
                return;
            }
        }

        updateStatus("Chưa có phần tàu nào đang bị trúng để repair.");
    }

    private void useShield(int x, int y) {
        if (!shieldAvailable) {
            updateStatus("Vùng chặn đã dùng trong trận này.");
            return;
        }

        int shieldSize = GameRules.getShieldSize(botAI.getDifficulty());
        shieldCells.clear();
        shieldCells.addAll(collectShieldCells(x, y, shieldSize));
        if (shieldCells.isEmpty()) {
            updateStatus("Không thể đặt vùng chặn ở vị trí này.");
            return;
        }

        shieldAvailable = false;
        shieldTurnsRemaining = 2;
        activeSkill = ActiveSkill.NONE;
        botAI.setBlockedCells(shieldCells);
        playerRenderer.setBlockedCells(shieldCells);
        playerRenderer.refresh(engine.getPlayerBoard());
        updateSkillButtons();
        addLog("Chặn vùng " + shieldSize + "x" + shieldSize + " từ " + toCoordinate(shieldCells.get(0)[0], shieldCells.get(0)[1])
                + " trong 2 lượt bot.");
        updateStatus("Đã chặn vùng " + shieldSize + "x" + shieldSize + ". Bot sẽ né vùng này trong 2 lượt.");
    }

    private List<int[]> collectShieldCells(int startX, int startY, int shieldSize) {
        List<int[]> cells = new ArrayList<>();
        Board board = engine.getPlayerBoard();
        int x0 = Math.max(0, Math.min(startX, board.getSize() - shieldSize));
        int y0 = Math.max(0, Math.min(startY, board.getSize() - shieldSize));

        for (int x = x0; x < x0 + shieldSize; x++) {
            for (int y = y0; y < y0 + shieldSize; y++) {
                if (isInBounds(board, x, y)) {
                    cells.add(new int[] { x, y });
                }
            }
        }
        return cells;
    }

    private boolean isTargeted(Board board, int x, int y) {
        CellState state = board.getCellState(x, y);
        return state == CellState.HIT || state == CellState.MISS || state == CellState.SUNK;
    }

    private boolean isInBounds(Board board, int x, int y) {
        return x >= 0 && x < board.getSize() && y >= 0 && y < board.getSize();
    }

    private void delayBotMove() {
        PauseTransition pause = new PauseTransition(Duration.seconds(BOT_DELAY_SECONDS));
        pause.setOnFinished(e -> botMove());
        pause.play();
    }

    private void startBotTurn() {
        activeSkill = ActiveSkill.NONE;
        botShotsRemaining = GameRules.getBotShotsPerTurn(botAI.getDifficulty());
        botAI.setBlockedCells(shieldTurnsRemaining > 0 ? shieldCells : new ArrayList<>());
        updateSkillButtons();
        updateStatus("Bot đang suy nghĩ...");
        delayBotMove();
    }

    private void botMove() {
        if (engine.isGameOver())
            return;

        if (botShotsRemaining <= 0) {
            finishBotTurn();
            return;
        }

        int[] move = botAI.calculateNextMove(engine.getPlayerBoard());
        int x = move[0];
        int y = move[1];
        boolean hit = engine.processShot(engine.getPlayerBoard(), x, y);
        if (!hit) {
            botShotsRemaining--;
        }

        if (hit) {
            botAI.recordHit(x, y);
            Ship ship = engine.getPlayerBoard().getShipAt(x, y);
            if (ship != null && ship.isSunk()) {
                botAI.notifyShipSunk();
            }

            if (engine.getPlayerBoard().isAllSunk()) {
                engine.endGame("BOT ĐÃ THẮNG!");
                return;
            }
        }

        if (botShotsRemaining > 0) {
            updateStatus("Bot còn " + botShotsRemaining + " phát...");
            delayBotMove();
        } else {
            finishBotTurn();
            updateSkillButtons();
            updateStatus("Lượt của bạn!");
        }
    }

    private void finishBotTurn() {
        if (shieldTurnsRemaining > 0) {
            shieldTurnsRemaining--;
            if (shieldTurnsRemaining == 0) {
                shieldCells.clear();
                botAI.setBlockedCells(shieldCells);
                playerRenderer.setBlockedCells(shieldCells);
                playerRenderer.refresh(engine.getPlayerBoard());
                addLog("Vùng chặn đã hết hiệu lực.");
            }
        }

        engine.setPlayerTurn(true);
        updateSkillButtons();
        updateStatus("Lượt của bạn!");
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        pulseNode(statusLabel);
    }

    private void addLog(String message) {
        battleLogArea.appendText(message + System.lineSeparator());
        battleLogArea.positionCaret(battleLogArea.getText().length());
    }

    private void updateStats() {
        turnStatsLabel.setText("Map " + engine.getPlayerBoard().getSize() + "x" + engine.getPlayerBoard().getSize()
                + " | Bắn: Bạn " + playerShots + " | Bot " + botShots);
        accuracyLabel.setText("Độ chính xác: Bạn " + getAccuracy(playerHits, playerShots) + "% | Bot "
                + getAccuracy(botHits, botShots) + "%");
    }

    private int getAccuracy(int hits, int shots) {
        if (shots == 0) {
            return 0;
        }
        return (int) Math.round((hits * 100.0) / shots);
    }

    private String toCoordinate(int x, int y) {
        return String.valueOf((char) ('A' + x)) + (y + 1);
    }

    @Override
    public void onShotProcessed(Board board, int x, int y, boolean hit) {
        if (board == engine.getPlayerBoard()) {
            playerRenderer.refresh(board);
            playerRenderer.animateShot(x, y, hit);
            playShotCue(hit);
            botShots++;
            if (hit) {
                botHits++;
            }
            addLog("Bot bắn " + toCoordinate(x, y) + ": " + (hit ? "trúng" : "trượt") + ".");
        } else {
            enemyRenderer.refresh(board);
            enemyRenderer.animateShot(x, y, hit);
            playShotCue(hit);
            playerShots++;
            if (hit) {
                playerHits++;
            }
            addLog("Bạn bắn " + toCoordinate(x, y) + ": " + (hit ? "trúng" : "trượt") + ".");
        }

        updateStats();
        if (hit) {
            String shooter = (board == engine.getEnemyBoard()) ? "Bạn" : "Bot";
            updateStatus(shooter + " đã bắn trúng!");
        }
    }

    @Override
    public void onShipSunk(Board board, Ship ship) {
        updateShipCounters();
        if (board == engine.getEnemyBoard()) {
            playerShipsSunk++;
            addLog("Bạn đánh chìm " + ship.getName() + ".");
            enemyRenderer.animateShipSunk(ship);
            updateStatus("BẠN ĐÃ ĐÁNH CHÌM TÀU " + ship.getName().toUpperCase() + "! " +
                    "Mời tiếp tục.");
        } else {
            botShipsSunk++;
            addLog("Bot đánh chìm " + ship.getName() + " của bạn.");
            playerRenderer.animateShipSunk(ship);
            updateStatus("Tàu " + ship.getName().toUpperCase() + " của bạn đã bị đánh chìm!");
        }
    }

    @Override
    public void onTurnChanged(boolean isPlayerTurn) {
        updateSkillButtons();
        if (isPlayerTurn)
            updateStatus("Lượt của bạn!");
        else
            updateStatus("Lượt của Bot...");
    }

    @Override
    public void onGameOver(String message) {
        enemyRenderer.setRevealShips(true);
        enemyRenderer.refresh(engine.getEnemyBoard());
        updateStatus("GAME OVER: " + message);
        addLog("GAME OVER: " + message);
        updateSkillButtons();
        showGameOverDialog(message);
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
    private void onRadarClick() {
        selectSkill(ActiveSkill.RADAR);
    }

    @FXML
    private void onMissileClick() {
        selectSkill(ActiveSkill.MISSILE);
    }

    @FXML
    private void onRepairClick() {
        if (placementMode || engine.isGameOver() || !engine.isPlayerTurn()) {
            return;
        }

        if (!repairAvailable) {
            updateStatus("Repair đã dùng trong trận này.");
            return;
        }

        useRepair();
    }

    @FXML
    private void onShieldClick() {
        selectSkill(ActiveSkill.SHIELD);
    }

    private void selectSkill(ActiveSkill skill) {
        if (placementMode || engine.isGameOver() || !engine.isPlayerTurn()) {
            return;
        }

        if (skill == ActiveSkill.RADAR && !radarAvailable) {
            updateStatus("Radar đã dùng trong trận này.");
            return;
        }

        if (skill == ActiveSkill.MISSILE && !missileAvailable) {
            updateStatus("Missile đã dùng trong trận này.");
            return;
        }

        if (skill == ActiveSkill.SHIELD && !shieldAvailable) {
            updateStatus("Vùng chặn đã dùng trong trận này.");
            return;
        }

        activeSkill = activeSkill == skill ? ActiveSkill.NONE : skill;
        if (activeSkill == ActiveSkill.SHIELD) {
            int size = GameRules.getShieldSize(botAI.getDifficulty());
            updateStatus("Chọn góc trái trên vùng chặn " + size + "x" + size + " trên bản đồ của bạn.");
            updateSkillButtons();
            return;
        }

        if (activeSkill == ActiveSkill.RADAR) {
            updateStatus("Chọn tâm vùng 3x3 để quét Radar.");
        } else if (activeSkill == ActiveSkill.MISSILE) {
            updateStatus("Chọn góc trái trên vùng Missile 2x2.");
        } else {
            updateStatus("Đã hủy kỹ năng.");
        }
        updateSkillButtons();
    }

    private void updateSkillButtons() {
        if (radarButton == null || missileButton == null || repairButton == null || shieldButton == null) {
            return;
        }

        boolean canUseSkill = !placementMode && engine.isPlayerTurn() && !engine.isGameOver();
        radarButton.setDisable(!canUseSkill || !radarAvailable);
        missileButton.setDisable(!canUseSkill || !missileAvailable);
        repairButton.setDisable(!canUseSkill || !repairAvailable);
        shieldButton.setDisable(!canUseSkill || !shieldAvailable);
        radarButton.setText(radarAvailable ? "RADAR (1)" : "RADAR (0)");
        missileButton.setText(missileAvailable ? "MISSILE 2x2 (1)" : "MISSILE 2x2 (0)");
        repairButton.setText(repairAvailable ? "REPAIR (1)" : "REPAIR (0)");
        int shieldSize = GameRules.getShieldSize(botAI.getDifficulty());
        shieldButton.setText(shieldAvailable ? "CHẶN " + shieldSize + "x" + shieldSize + " (1)"
                : "CHẶN " + shieldSize + "x" + shieldSize + " (0)");
    }

    @FXML
    private void onRestartClick() {
        resetGame();
    }

    @FXML
    private void onRotateShipClick() {
        rotateCurrentShip();
    }

    @FXML
    private void onAutoPlaceClick() {
        if (!placementMode) {
            return;
        }

        clearPlayerBoard();
        placementManager.reset();
        placementManager.autoPlaceRemainingShips(engine.getPlayerBoard());
        playerRenderer.render(engine.getPlayerBoard());
        updateShipCounters();
        updatePlacementStatus();
        addLog("Auto-place: đã tạo đội hình mới.");
    }

    @FXML
    private void onConfirmPlacementClick() {
        if (!placementMode) {
            return;
        }

        if (!placementManager.allShipsPlaced()) {
            updateStatus("Bạn cần đặt đủ tất cả tàu trước khi bắt đầu.");
            return;
        }

        placementMode = false;
        playerRenderer.refresh(engine.getPlayerBoard());
        updateSkillButtons();
        updateStatus("Đã xác nhận đội hình. Lượt của bạn!");
        addLog("Bạn xác nhận đội hình và bắt đầu trận đấu.");
    }

    private void rotateCurrentShip() {
        if (!placementMode || placementManager.allShipsPlaced()) {
            return;
        }

        placementManager.toggleOrientation();
        playerRenderer.refresh(engine.getPlayerBoard());
        updatePlacementStatus();
    }

    private void clearPlayerBoard() {
        Board board = engine.getPlayerBoard();
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                board.setCellState(x, y, CellState.WATER);
            }
        }
        board.getShips().clear();
    }

    private void showGameOverDialog(String message) {
        if (gameOverDialogShown) {
            return;
        }
        gameOverDialogShown = true;

        long elapsedSeconds = Math.max(1, (System.currentTimeMillis() - gameStartMillis) / 1000);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kết thúc trận đấu");
        alert.setHeaderText(message);
        alert.setContentText(
                "Độ khó: " + GameRules.getDisplayName(botAI.getDifficulty()) + "\n"
                        + "Map: " + engine.getPlayerBoard().getSize() + "x" + engine.getPlayerBoard().getSize()
                        + "\n"
                        + "Thời gian: " + elapsedSeconds + " giây\n"
                        + "Bạn: " + playerHits + "/" + playerShots + " hit, " + getAccuracy(playerHits, playerShots)
                        + "%, đánh chìm " + playerShipsSunk + " tàu\n"
                        + "Bot: " + botHits + "/" + botShots + " hit, " + getAccuracy(botHits, botShots)
                        + "%, đánh chìm " + botShipsSunk + " tàu");
        alert.show();
    }

    private void playShotCue(boolean hit) {
        if (!hit) {
            return;
        }

        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (RuntimeException ignored) {
            // Some minimal runtimes do not expose a desktop sound device.
        }
    }

    private void pulseNode(javafx.scene.Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(120), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.035);
        scale.setToY(1.035);
        scale.setCycleCount(2);
        scale.setAutoReverse(true);
        scale.play();

        FadeTransition fade = new FadeTransition(Duration.millis(120), node);
        fade.setFromValue(0.82);
        fade.setToValue(1.0);
        fade.play();
    }

    @FXML
    private void onBackToMenu(javafx.event.ActionEvent event) throws java.io.IOException {
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(App.class.getResource("menu-view.fxml"));
        javafx.scene.Scene scene = new javafx.scene.Scene(fxmlLoader.load(), 1100, 760);
        stage.setScene(scene);
    }

}
