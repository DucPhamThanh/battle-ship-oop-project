package com.battleship.logic;

import com.battleship.model.Board;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotAI {
    private boolean targetMode = false;
    private final List<int[]> targetQueue = new ArrayList<>();
    private final List<int[]> botShots = new ArrayList<>();
    private final Random random = new Random();

    public void reset() {
        targetMode = false;
        targetQueue.clear();
        botShots.clear();
    }

    public int[] calculateNextMove() {
        int x, y;
        while (targetMode && !targetQueue.isEmpty()) {
            int[] pos = targetQueue.remove(0);
            if (!isAlreadyShot(pos[0], pos[1])) {
                x = pos[0];
                y = pos[1];
                botShots.add(new int[]{x, y});
                return new int[]{x, y};
            }
        }

        // Hunt mode or target queue empty
        targetMode = false;
        do {
            x = random.nextInt(Board.SIZE);
            y = random.nextInt(Board.SIZE);
        } while (isAlreadyShot(x, y));

        botShots.add(new int[]{x, y});
        return new int[]{x, y};
    }

    public void notifyShipSunk() {
        targetMode = false;
        targetQueue.clear();
    }

    public void recordHit(int x, int y) {
        targetMode = true;
        int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
        for (int[] n : neighbors) {
            int nx = n[0], ny = n[1];
            if (nx >= 0 && nx < Board.SIZE && ny >= 0 && ny < Board.SIZE && !isAlreadyShot(nx, ny)) {
                targetQueue.add(new int[]{nx, ny});
            }
        }
    }

    private boolean isAlreadyShot(int x, int y) {
        return botShots.stream().anyMatch(s -> s[0] == x && s[1] == y);
    }
}
