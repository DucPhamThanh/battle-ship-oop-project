package com.battleship.model;

import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int DEFAULT_SIZE = 10;
    public static final int SIZE = DEFAULT_SIZE;
    private final int size;
    private final CellState[][] grid;
    private final List<Ship> ships;

    public Board() {
        this(DEFAULT_SIZE);
    }

    public Board(int size) {
        this.size = size;
        grid = new CellState[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = CellState.WATER;
            }
        }
        ships = new ArrayList<>();
    }

    private boolean canPlaceShip(Ship ship, int x, int y) { // xác định nơi có thể đặt tàu
        int size = ship.getSize();

        if (x < 0 || x >= this.size || y < 0 || y >= this.size) {
            return false;
        }

        if (ship.isVertical() == true) {
            if (y + size > this.size)
                return false;
            for (int i = y; i < y + size; i++) {
                if (grid[x][i] != CellState.WATER)
                    return false;
            }
        } else {
            if (x + size > this.size)
                return false;
            for (int i = x; i < x + size; i++) {
                if (grid[i][y] != CellState.WATER)
                    return false;
            }
        }
        return true;
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

    // Kiểm tra ô x,y có tàu không?
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

    // Getter & Setter
    public CellState getCellState(int x, int y) {
        return grid[x][y];
    }

    public void setCellState(int x, int y, CellState state) {
        grid[x][y] = state;
    }

    public List<Ship> getShips() {
        return ships;
    }

    public int getSize() {
        return size;
    }
}
