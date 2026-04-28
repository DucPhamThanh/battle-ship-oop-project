package com.battleship.logic;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import java.util.ArrayList;
import java.util.List;

public class PlacementManager {
    private int currentShipIndex = 0;
    private final int[] shipSizes = { 5, 4, 3, 3, 2 };
    private final String[] shipNames = { "Carrier", "Battleship", "Cruiser", "Submarine", "Destroyer" };
    private final List<int[]> currentSelection = new ArrayList<>();

    public void reset() {
        currentShipIndex = 0;
        currentSelection.clear();
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
    public int getCurrentShipIndex() { return currentShipIndex; }
    public String getCurrentShipName() { return shipNames[currentShipIndex]; }
    public int getCurrentShipSize() { return shipSizes[currentShipIndex]; }
    public int getRemainingCells() { return shipSizes[currentShipIndex] - currentSelection.size(); }
    public boolean allShipsPlaced() { return currentShipIndex >= shipSizes.length; }
    public List<int[]> getCurrentSelection() { return currentSelection; }
    public int getTotalShips() { return shipSizes.length; }
}
