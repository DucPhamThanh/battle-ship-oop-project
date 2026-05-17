package com.battleship.logic;

public final class GameRules {
    private GameRules() {
    }

    public static int getBoardSize(BotAI.Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 8;
            case HARD:
                return 12;
            case MEDIUM:
            default:
                return 10;
        }
    }

    public static int[] getShipSizes(BotAI.Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return new int[] { 4, 3, 3, 2 };
            case HARD:
                return new int[] { 5, 4, 4, 3, 3, 2 };
            case MEDIUM:
            default:
                return new int[] { 5, 4, 3, 3, 2 };
        }
    }

    public static String[] getShipNames(BotAI.Difficulty difficulty) {
        int[] sizes = getShipSizes(difficulty);
        int[] totalBySize = countBySize(sizes);
        int[] seenBySize = new int[totalBySize.length];
        String[] names = new String[sizes.length];

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            seenBySize[size]++;
            names[i] = totalBySize[size] > 1
                    ? "Tàu_size_" + size + "_" + seenBySize[size]
                    : "Tàu_size_" + size;
        }

        return names;
    }

    public static int getBotShotsPerTurn(BotAI.Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 1;
            case MEDIUM:
            case HARD:
                return 2;
            default:
                return 1;
        }
    }

    public static int getShieldSize(BotAI.Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 1;
            case HARD:
                return 3;
            case MEDIUM:
            default:
                return 2;
        }
    }

    public static String getDisplayName(BotAI.Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return "Dễ";
            case HARD:
                return "Khó";
            case MEDIUM:
            default:
                return "Trung bình";
        }
    }

    private static int[] countBySize(int[] sizes) {
        int largest = 0;
        for (int size : sizes) {
            largest = Math.max(largest, size);
        }

        int[] counts = new int[largest + 1];
        for (int size : sizes) {
            counts[size]++;
        }
        return counts;
    }
}
