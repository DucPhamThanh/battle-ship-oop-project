package com.battleship.model;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    private final String name;
    private final int size;
    private final boolean vertical;
    private int health;
    private final List<int[]> positions;

    public Ship(String name, int size, boolean vertical) {
        this.name = name;
        this.size = size;
        this.vertical = vertical;
        this.health = size;
        this.positions = new ArrayList<>();
    }

    public void addPosition(int x, int y) {
        positions.add(new int[]{x, y});
    }

    public List<int[]> getPositions() {
        return positions;
    }

    public boolean isSunk() {
        return health <= 0;
    }

    public void hit() {
        health--;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public boolean isVertical() {
        return vertical;
    }
}
