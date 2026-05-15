package com.battleship.model;

public enum CellState {
    WATER(0),
    SHIP(1),
    MISS(2),
    HIT(3),
    SUNK(4);

    private final int value;

    private CellState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
