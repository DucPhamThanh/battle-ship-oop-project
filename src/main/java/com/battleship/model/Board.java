package com.battleship.model;

import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int SIZE = 10;
    private final CellState[][] grid;
    private final List<Ship> ships;

    public Board() {
        grid = new CellState[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                grid[i][j] = CellState.WATER;
            }
        }
        ships = new ArrayList<>();
    }

    public boolean placeShip(Ship ship, int x, int y) {
        if (canPlaceShip(ship, x, y)) {
            int size = ship.getSize();
            if (ship.isVertical()) {
                for (int i = y; i < y + size; i++) {
                    grid[x][i] = CellState.SHIP;
                    ship.addPosition(x, i);
                }
            } else {
                for (int i = x; i < x + size; i++) {
                    grid[i][y] = CellState.SHIP;
                    ship.addPosition(i, y);
                }
            }
            ships.add(ship);
            return true;
        }
        return false;
    }

    private boolean canPlaceShip(Ship ship, int x, int y) {
        int size = ship.getSize();

        if (ship.isVertical()) {
            if (y + size > SIZE) return false;
            for (int i = y; i < y + size; i++) {
                if (grid[x][i] != CellState.WATER) return false;
            }
        } else {
            if (x + size > SIZE) return false;
            for (int i = x; i < x + size; i++) {
                if (grid[i][y] != CellState.WATER) return false;
            }
        }
        return true;
    }

    public CellState getCellState(int x, int y) {
        return grid[x][y];
    }

    public void setCellState(int x, int y, CellState state) {
        grid[x][y] = state;
    }

    public Ship getShipAt(int x, int y) {
        for (Ship ship : ships) {
            for (int[] pos : ship.getPositions()) {
                if (pos[0] == x && pos[1] == y) {
                    return ship;
                }
            }
        }
        return null;
    }

    public boolean isAllSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }
    
    public List<Ship> getShips() {
        return ships;
    }
}
