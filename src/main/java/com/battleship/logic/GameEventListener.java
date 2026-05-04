package com.battleship.logic;

import com.battleship.model.Board;
import com.battleship.model.Ship;

public interface GameEventListener {
    void onShotProcessed(Board board, int x, int y, boolean hit);
    void onShipSunk(Board board, Ship ship);
    void onTurnChanged(boolean isPlayerTurn);
    void onGameOver(String message);
    void onStatusChanged(String message);
}
