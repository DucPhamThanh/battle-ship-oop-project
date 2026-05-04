package com.battleship.logic;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;

public class GameEngine {
    private Board playerBoard;
    private Board enemyBoard;
    private boolean playerTurn;
    private boolean gameOver;
    private java.util.List<GameEventListener> listeners = new java.util.ArrayList<>();

    public GameEngine() {
        reset();
    }

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    public void reset() {
        playerBoard = new Board();
        enemyBoard = new Board();
        playerTurn = true;
        gameOver = false;
        setupEnemyShips();
    }

    private void setupEnemyShips() {
        int[] sizes = { 5, 4, 3, 3, 2 };
        String[] names = { "Battleship", "Destroyer", "PatrolBoat", "RescueShip", "Plane" };
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < sizes.length; i++) {
            boolean placed = false;
            while (!placed) {
                int x = random.nextInt(Board.SIZE);
                int y = random.nextInt(Board.SIZE);
                boolean vertical = random.nextBoolean();
                placed = enemyBoard.placeShip(new Ship(names[i], sizes[i], vertical), x, y);
            }
        }
    }

    public boolean processShot(Board board, int x, int y) {
        CellState state = board.getCellState(x, y);
        boolean hit = false;
        if (state == CellState.SHIP) {
            board.setCellState(x, y, CellState.HIT);
            Ship ship = board.getShipAt(x, y);
            if (ship != null) {
                ship.hit();
                if (ship.isSunk()) {
                    markShipAsSunk(board, ship);
                    listeners.forEach(l -> l.onShipSunk(board, ship));
                }
            }
            hit = true;
        } else {
            board.setCellState(x, y, CellState.MISS);
            hit = false;
        }
        
        boolean finalHit = hit;
        listeners.forEach(l -> l.onShotProcessed(board, x, y, finalHit));
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
    public Board getPlayerBoard() { return playerBoard; }
    public Board getEnemyBoard() { return enemyBoard; }
    public boolean isPlayerTurn() { return playerTurn; }
    public void setPlayerTurn(boolean playerTurn) { 
        this.playerTurn = playerTurn; 
        listeners.forEach(l -> l.onTurnChanged(playerTurn));
    }
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
}
