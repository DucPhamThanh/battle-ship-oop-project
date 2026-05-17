package com.battleship.logic;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import java.util.ArrayList;
import java.util.List;

public class PlacementManager {
    private int currentShipIndex = 0;
    private int[] shipSizes = GameRules.getShipSizes(BotAI.Difficulty.MEDIUM);
    private String[] shipNames = GameRules.getShipNames(BotAI.Difficulty.MEDIUM);
    private final List<int[]> currentSelection = new ArrayList<>();
    private boolean vertical = false;

    public void configureForDifficulty(BotAI.Difficulty difficulty) {
        shipSizes = GameRules.getShipSizes(difficulty);
        shipNames = GameRules.getShipNames(difficulty);
        reset();
    }

    public void reset() {
        currentShipIndex = 0;
        currentSelection.clear();
        vertical = false;
    }

    public void toggleOrientation() {
        vertical = !vertical;
        currentSelection.clear();
    }

    public boolean isVertical() {
        return vertical;
    }

    public String getOrientationName() {
        return vertical ? "dọc" : "ngang";
    }

    public List<int[]> getPreviewCells(int x, int y) {
        List<int[]> cells = new ArrayList<>();
        if (allShipsPlaced()) {
            return cells;
        }

        int shipSize = getCurrentShipSize();
        for (int i = 0; i < shipSize; i++) {
            int nx = vertical ? x : x + i;
            int ny = vertical ? y + i : y;
            cells.add(new int[] { nx, ny });
        }
        return cells;
    }

    public boolean canPlaceCurrentShip(Board board, int x, int y) {
        return areCellsPlaceable(board, getPreviewCells(x, y));
    }

    public Ship placeCurrentShip(Board board, int x, int y) {
        if (!canPlaceCurrentShip(board, x, y)) {
            return null;
        }

        Ship ship = new Ship(shipNames[currentShipIndex], shipSizes[currentShipIndex], vertical);
        for (int[] pos : getPreviewCells(x, y)) {
            board.setCellState(pos[0], pos[1], CellState.SHIP);
            ship.addPosition(pos[0], pos[1]);
        }
        board.getShips().add(ship);
        currentShipIndex++;
        currentSelection.clear();
        return ship;
    }

    public void autoPlaceRemainingShips(Board board) {
        java.util.Random random = new java.util.Random();
        while (!allShipsPlaced()) {
            boolean placed = false;
            while (!placed) {
                vertical = random.nextBoolean();
                int x = random.nextInt(board.getSize());
                int y = random.nextInt(board.getSize());
                placed = placeCurrentShip(board, x, y) != null;
            }
        }
    }

    public boolean areCellsPlaceable(Board board, List<int[]> cells) {
        if (cells.isEmpty()) {
            return false;
        }

        for (int[] cell : cells) {
            int x = cell[0];
            int y = cell[1];
            if (x < 0 || x >= board.getSize() || y < 0 || y >= board.getSize()) {
                return false;
            }
            if (board.getCellState(x, y) != CellState.WATER) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidNextCell(int x, int y, Board board) {
        if (board.getCellState(x, y) != CellState.WATER) return false;
        if (isAlreadySelected(x, y)) return false;
        if (currentSelection.isEmpty()) return true;

        int[] last = currentSelection.get(currentSelection.size() - 1);
        boolean adjacent = (Math.abs(x - last[0]) == 1 && y == last[1]) || (x == last[0] && Math.abs(y - last[1]) == 1);
        if (!adjacent) return false;

        if (currentSelection.size() >= 2) {
            int[] first = currentSelection.get(0);
            boolean horizontal = first[1] == last[1];
            if (horizontal) return y == first[1];
            else return x == first[0];
        }
        return true;
    }

    public boolean isAlreadySelected(int x, int y) {
        return currentSelection.stream().anyMatch(p -> p[0] == x && p[1] == y);
    }

    public void addCell(int x, int y) {
        currentSelection.add(new int[]{x, y});
    }

    public boolean isShipComplete() {
        return currentSelection.size() == shipSizes[currentShipIndex];
    }

    public Ship commitShip() {
        boolean vertical = currentSelection.size() > 1 && currentSelection.get(0)[0] == currentSelection.get(1)[0];
        Ship ship = new Ship(shipNames[currentShipIndex], shipSizes[currentShipIndex], vertical);
        for (int[] pos : currentSelection) {
            ship.addPosition(pos[0], pos[1]);
        }
        currentShipIndex++;
        currentSelection.clear();
        return ship;
    }

    // Getters
    public int getCurrentShipIndex() {
        return currentShipIndex;
    }

    public String getCurrentShipName() {
        return shipNames[currentShipIndex];
    }

    public int getCurrentShipSize() {
        return shipSizes[currentShipIndex];
    }

    public int getRemainingCells() {
        return shipSizes[currentShipIndex] - currentSelection.size();
    }

    public boolean allShipsPlaced() {
        if (currentShipIndex >= shipSizes.length)
            return true;
        else
            return false;
    }

    public List<int[]> getCurrentSelection() {
        return currentSelection;
    }

    public int getTotalShips() {
        return shipSizes.length;
    }
}
