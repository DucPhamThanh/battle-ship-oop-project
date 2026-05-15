package com.battleship.logic;

import com.battleship.model.Board;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BotAI {
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    private Difficulty difficulty = Difficulty.MEDIUM;
    private boolean targetMode = false;
    private final List<int[]> targetQueue = new ArrayList<>();
    private final List<int[]> botShots = new ArrayList<>();
    private int[] firstHit = null; // Vị trí phát bắn trúng đầu tiên của chuỗi hiện tại
    private final Random random = new Random();

    // Getter/Setter
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void reset() {
        targetMode = false;
        targetQueue.clear();
        botShots.clear();
        firstHit = null;
    }

    public int[] calculateNextMove() {
        int x = -1, y = -1;

        // 1. Target Mode
        while (targetMode == true && targetQueue.isEmpty() == false) {
            int[] pos = targetQueue.remove(0);
            if (!isAlreadyShot(pos[0], pos[1])) {
                x = pos[0];
                y = pos[1];
                break;
            }
        }

        // 2. Hunt Mode
        if (x == -1) {
            targetMode = false;
            if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {
                List<int[]> parityCells = new ArrayList<>();
                for (int r = 0; r < Board.SIZE; r++) {
                    for (int c = 0; c < Board.SIZE; c++) {
                        if ((r + c) % 2 == 0 && !isAlreadyShot(r, c)) {
                            parityCells.add(new int[] { r, c });
                        }
                    }
                }

                if (!parityCells.isEmpty()) {
                    int[] selected = parityCells.get(random.nextInt(parityCells.size()));
                    x = selected[0];
                    y = selected[1];
                }
            }

            if (x == -1) {
                int[] pos = randomHunt();
                x = pos[0];
                y = pos[1];
            }
        }

        botShots.add(new int[] { x, y });
        return new int[] { x, y };
    }

    private int[] randomHunt() {
        int x, y;
        do {
            x = random.nextInt(Board.SIZE);
            y = random.nextInt(Board.SIZE);
        } while (isAlreadyShot(x, y));
        return new int[] { x, y };
    }

    public void notifyShipSunk() {
        firstHit = null;
    }

    public void recordHit(int x, int y) {
        targetMode = true;

        // 1. Thêm 4 hướng xung quanh vào đầu hàng đợi để ưu tiên kiểm tra ngay
        int[][] neighbors = { { x + 1, y }, { x - 1, y }, { x, y + 1 }, { x, y - 1 } };
        for (int[] n : neighbors) {
            addIfValidAtFront(n[0], n[1]);
        }

        // 2. Nếu đã có phát bắn trúng trước đó, xác định hướng và ưu tiên cực cao cho
        // trục này
        if (firstHit == null) {
            firstHit = new int[] { x, y };
        } else {
            int dx = x - firstHit[0];
            int dy = y - firstHit[1];

            // Chuẩn hóa hướng (về -1, 0, 1)
            if (dx != 0)
                dx = dx / Math.abs(dx);
            if (dy != 0)
                dy = dy / Math.abs(dy);

            // Ưu tiên tuyệt đối cho các ô nằm trên cùng trục (đưa lên đầu hàng đợi)
            addIfValidAtFront(x + dx, y + dy);
            addIfValidAtFront(firstHit[0] - dx, firstHit[1] - dy);
        }
    }

    private void addIfValidAtFront(int nx, int ny) {
        if (nx >= 0 && nx < Board.SIZE && ny >= 0 && ny < Board.SIZE && !isAlreadyShot(nx, ny)) {
            // Xóa ô nếu đã tồn tại trong queue để đưa nó lên vị trí ưu tiên đầu tiên
            targetQueue.removeIf(p -> p[0] == nx && p[1] == ny);
            targetQueue.add(0, new int[] { nx, ny });
        }
    }

    private boolean isAlreadyShot(int x, int y) { // Kiểm tra xem ô được bắn chưa
        return botShots.stream().anyMatch(s -> s[0] == x && s[1] == y);
    }
}
