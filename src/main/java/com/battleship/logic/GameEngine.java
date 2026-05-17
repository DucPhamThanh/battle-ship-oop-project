package com.battleship.logic;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import java.util.*;

public class GameEngine {
    private static final int HARD_LAYOUT_CANDIDATES = 350;
    private Board playerBoard;
    private Board enemyBoard;
    private boolean playerTurn;
    private boolean gameOver;
    private List<GameEventListener> listeners = new ArrayList<>();
    private final Random random = new Random();

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    public void reset() {
        reset(BotAI.Difficulty.MEDIUM);
    }

    public void reset(BotAI.Difficulty difficulty) {
        playerBoard = new Board(GameRules.getBoardSize(difficulty));
        enemyBoard = createEnemyBoard(difficulty);
        playerTurn = true;
        gameOver = false;
    }

    public GameEngine() {
        reset();
    }

    private Board createEnemyBoard(BotAI.Difficulty difficulty) {
        if (difficulty == BotAI.Difficulty.HARD) {
            return createHardEnemyBoard();
        }
        return createRandomEnemyBoard(difficulty);
    }

    private Board createRandomEnemyBoard(BotAI.Difficulty difficulty) {
        Board board = new Board(GameRules.getBoardSize(difficulty));
        placeRandomShips(board, difficulty);
        return board;
    }

    private void placeRandomShips(Board board, BotAI.Difficulty difficulty) {
        int[] shipSizes = GameRules.getShipSizes(difficulty);
        String[] shipNames = GameRules.getShipNames(difficulty);
        for (int i = 0; i < shipSizes.length; i++) {
            boolean placed = false;
            while (!placed) {
                int x = random.nextInt(board.getSize());
                int y = random.nextInt(board.getSize());
                boolean vertical = random.nextBoolean();
                placed = board.placeShip(new Ship(shipNames[i], shipSizes[i], vertical), x, y);
            }
        }
    }

    private Board createHardEnemyBoard() {
        Board bestBoard = null;
        int bestScore = Integer.MIN_VALUE;

        for (int i = 0; i < HARD_LAYOUT_CANDIDATES; i++) {
            Board candidate = createRandomEnemyBoard(BotAI.Difficulty.HARD);
            int score = scoreEnemyLayout(candidate) + random.nextInt(12);
            if (score > bestScore) {
                bestScore = score;
                bestBoard = candidate;
            }
        }

        return bestBoard != null ? bestBoard : createRandomEnemyBoard(BotAI.Difficulty.HARD);
    }

    private int scoreEnemyLayout(Board board) {
        int verticalCount = 0;
        int edgeShips = 0;
        int score = 0;

        for (Ship ship : board.getShips()) {
            if (ship.isVertical()) {
                verticalCount++;
            }

            int edgeDistance = getShipEdgeDistance(board, ship);
            if (edgeDistance <= 1) {
                edgeShips++;
            }

            score += edgeDistance <= 1 ? 18 : 0;
            score += getShipSpreadScore(board, ship);

            if (ship.getSize() >= 4) {
                score -= getShipCenterScore(board, ship) * 3;
            }

            if (ship.getSize() == 2) {
                score += edgeDistance <= 1 ? 28 : 0;
                score += getShipCenterScore(board, ship) <= 4 ? 14 : 0;
            }
        }

        int horizontalCount = board.getShips().size() - verticalCount;
        score += Math.min(verticalCount, horizontalCount) * 35;
        score -= Math.abs(verticalCount - horizontalCount) * 12;
        score += edgeShips >= 2 && edgeShips <= 4 ? 45 : -25;

        int touchingPairs = countTouchingShipPairs(board);
        if (touchingPairs >= 1 && touchingPairs <= 4) {
            score += 35;
        } else if (touchingPairs > 8) {
            score -= 45;
        }

        return score;
    }

    private int getShipSpreadScore(Board board, Ship ship) {
        int score = 0;
        int[] center = getShipCenter(ship);
        for (Ship other : board.getShips()) {
            if (ship == other) {
                continue;
            }
            int[] otherCenter = getShipCenter(other);
            int distance = Math.abs(center[0] - otherCenter[0]) + Math.abs(center[1] - otherCenter[1]);
            score += Math.min(distance, 8);
        }
        return score;
    }

    private int getShipEdgeDistance(Board board, Ship ship) {
        int minX = board.getSize() - 1;
        int minY = board.getSize() - 1;
        int maxX = 0;
        int maxY = 0;
        for (int[] pos : ship.getPositions()) {
            minX = Math.min(minX, pos[0]);
            minY = Math.min(minY, pos[1]);
            maxX = Math.max(maxX, pos[0]);
            maxY = Math.max(maxY, pos[1]);
        }
        return Math.min(Math.min(minX, minY), Math.min(board.getSize() - 1 - maxX, board.getSize() - 1 - maxY));
    }

    private int getShipCenterScore(Board board, Ship ship) {
        int[] center = getShipCenter(ship);
        int distanceFromCenter = Math.abs((2 * center[0]) - (board.getSize() - 1))
                + Math.abs((2 * center[1]) - (board.getSize() - 1));
        return Math.max(0, (2 * board.getSize()) - 2 - distanceFromCenter);
    }

    private int[] getShipCenter(Ship ship) {
        int sumX = 0;
        int sumY = 0;
        for (int[] pos : ship.getPositions()) {
            sumX += pos[0];
            sumY += pos[1];
        }
        return new int[] { sumX / ship.getPositions().size(), sumY / ship.getPositions().size() };
    }

    private int countTouchingShipPairs(Board board) {
        int touchingPairs = 0;
        List<Ship> ships = board.getShips();
        for (int i = 0; i < ships.size(); i++) {
            for (int j = i + 1; j < ships.size(); j++) {
                if (areShipsTouching(ships.get(i), ships.get(j))) {
                    touchingPairs++;
                }
            }
        }
        return touchingPairs;
    }

    private boolean areShipsTouching(Ship a, Ship b) {
        for (int[] posA : a.getPositions()) {
            for (int[] posB : b.getPositions()) {
                int distance = Math.abs(posA[0] - posB[0]) + Math.abs(posA[1] - posB[1]);
                if (distance == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean processShot(Board board, int x, int y) {
        CellState state = board.getCellState(x, y);
        boolean hit = false;
        if (state == CellState.SHIP) {
            board.setCellState(x, y, CellState.HIT);
            Ship ship = board.getShipAt(x, y);
            if (ship != null) {
                ship.hit();
            }
            hit = true;
        } else {
            board.setCellState(x, y, CellState.MISS);
            hit = false;
        }

        boolean finalHit = hit;
        listeners.forEach(l -> l.onShotProcessed(board, x, y, finalHit));

        if (hit == true) {
            Ship ship = board.getShipAt(x, y);
            if (ship != null && ship.isSunk() == true) {
                markShipAsSunk(board, ship);
                listeners.forEach(l -> l.onShipSunk(board, ship));
            }
        }
        return hit;
    }

    public void markShipAsSunk(Board board, Ship ship) {
        for (int[] pos : ship.getPositions()) {
            board.setCellState(pos[0], pos[1], CellState.SUNK);
        }
    }

    public void notifyStatus(String message) {
        listeners.forEach(l -> l.onStatusChanged(message));
    }

    public void endGame(String message) {
        gameOver = true;
        listeners.forEach(l -> l.onGameOver(message));
    }

    // Getters and Setters
    public Board getPlayerBoard() {
        return playerBoard;
    }

    public Board getEnemyBoard() {
        return enemyBoard;
    }

    public boolean isPlayerTurn() {
        return playerTurn;
    }

    public void setPlayerTurn(boolean playerTurn) {
        this.playerTurn = playerTurn;
        listeners.forEach(l -> l.onTurnChanged(playerTurn));
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }
}
