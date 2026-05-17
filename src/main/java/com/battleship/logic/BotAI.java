package com.battleship.logic;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import com.battleship.model.Ship;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class BotAI {
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    private enum Direction {
        HORIZONTAL, VERTICAL
    }

    private static final int HUNT_PLACEMENT_WEIGHT = 4;
    private static final int HIT_PLACEMENT_WEIGHT = 50;
    private static final int CHECKERBOARD_BONUS = 18;
    private static final int CENTER_BONUS_WEIGHT = 2;
    private static final int ORTHOGONAL_SUNK_PENALTY = 14;
    private static final int DIAGONAL_SUNK_PENALTY = 8;
    private static final double EASY_TARGET_CHANCE = 0.50;
    private static final double MEDIUM_CHECKERBOARD_CHANCE = 0.70;

    private Difficulty difficulty = Difficulty.MEDIUM;
    private boolean targetMode = false;
    private final List<int[]> targetQueue = new ArrayList<>();
    private final List<int[]> botShots = new ArrayList<>();
    private final List<int[]> currentHits = new ArrayList<>();
    private final List<int[]> blockedCells = new ArrayList<>();
    private Direction lockedDirection = null;
    private final Random random = new Random();
    private int boardSize = Board.DEFAULT_SIZE;

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
        currentHits.clear();
        blockedCells.clear();
        lockedDirection = null;
    }

    public void setBlockedCells(List<int[]> cells) {
        blockedCells.clear();
        if (cells == null) {
            return;
        }

        for (int[] cell : cells) {
            blockedCells.add(new int[] { cell[0], cell[1] });
        }
    }

    public void forgetShot(int x, int y) {
        botShots.removeIf(shot -> shot[0] == x && shot[1] == y);
        targetQueue.removeIf(target -> target[0] == x && target[1] == y);
        currentHits.removeIf(hit -> hit[0] == x && hit[1] == y);
    }

    public int[] calculateNextMove() {
        return calculateNextMove(null);
    }

    public int[] calculateNextMove(Board targetBoard) {
        int[] move = null;
        boardSize = targetBoard != null ? targetBoard.getSize() : Board.DEFAULT_SIZE;
        if (targetBoard != null && !hasAvailableShot(targetBoard)) {
            blockedCells.clear();
        }

        if (difficulty == Difficulty.HARD && targetBoard != null) {
            move = getFocusedTargetMove(targetBoard);
        }

        if (difficulty == Difficulty.HARD && targetBoard != null && move == null) {
            move = getProbabilityMove(targetBoard);
        }

        if (move == null && difficulty == Difficulty.MEDIUM) {
            move = getTargetMove();
        }

        if (move == null && difficulty == Difficulty.EASY) {
            move = getEasyTargetMove();
        }

        if (move == null) {
            move = getHuntMove(targetBoard);
        }

        botShots.add(new int[] { move[0], move[1] });
        return move;
    }

    private int[] getEasyTargetMove() {
        if (!targetMode || targetQueue.isEmpty()) {
            return null;
        }

        if (random.nextDouble() >= EASY_TARGET_CHANCE) {
            return null;
        }

        if (random.nextDouble() < 0.35) {
            Collections.shuffle(targetQueue, random);
        }

        return getTargetMove();
    }

    private int[] getTargetMove() {
        while (targetMode && !targetQueue.isEmpty()) {
            int[] pos = targetQueue.remove(0);
            if (!isAlreadyShot(pos[0], pos[1]) && !isBlocked(pos[0], pos[1])) {
                return pos;
            }
        }

        targetMode = false;
        lockedDirection = null;
        currentHits.clear();
        return null;
    }

    private int[] getHuntMove(Board targetBoard) {
        if ((difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) && targetBoard != null
                && shouldUseCheckerboardHunt()) {
            List<int[]> parityCells = new ArrayList<>();
            int preferredParity = getPreferredHuntParity(targetBoard);
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    if ((r + c) % 2 == preferredParity && isShootable(targetBoard, r, c)) {
                        parityCells.add(new int[] { r, c });
                    }
                }
            }

            if (!parityCells.isEmpty()) {
                return parityCells.get(random.nextInt(parityCells.size()));
            }
        }

        return randomHunt(targetBoard);
    }

    private int getPreferredHuntParity(Board targetBoard) {
        if (difficulty == Difficulty.MEDIUM) {
            return 0;
        }

        int smallestShip = getRemainingShipSizes(targetBoard).stream().mapToInt(Integer::intValue).min().orElse(2);
        return smallestShip <= 2 ? 0 : random.nextInt(2);
    }

    private boolean shouldUseCheckerboardHunt() {
        return difficulty == Difficulty.HARD || random.nextDouble() < MEDIUM_CHECKERBOARD_CHANCE;
    }

    private int[] getProbabilityMove(Board targetBoard) {
        int[][] scores = new int[targetBoard.getSize()][targetBoard.getSize()];
        List<int[]> focusedHits = getBoardHits(targetBoard);
        List<Integer> remainingShipSizes = getRemainingShipSizes(targetBoard);
        boolean requireAllFocusedHits = focusedHits.size() > 1 && areCollinear(focusedHits);

        for (int shipSize : remainingShipSizes) {
            scoreShipPlacements(targetBoard, scores, focusedHits, requireAllFocusedHits, shipSize, true);
            scoreShipPlacements(targetBoard, scores, focusedHits, requireAllFocusedHits, shipSize, false);
        }

        return pickHighestScoreMove(targetBoard, scores, focusedHits);
    }

    private int[] getFocusedTargetMove(Board targetBoard) {
        List<List<int[]>> hitClusters = getHitClusters(targetBoard);
        if (hitClusters.isEmpty()) {
            return null;
        }

        List<int[]> targetCluster = chooseTargetCluster(hitClusters);
        List<int[]> candidates = getLineEndCandidates(targetBoard, targetCluster);
        if (candidates.isEmpty()) {
            candidates = getAdjacentCandidates(targetBoard, targetCluster);
        }

        return pickBestTargetCandidate(targetBoard, candidates, targetCluster);
    }

    private List<List<int[]>> getHitClusters(Board targetBoard) {
        List<List<int[]>> clusters = new ArrayList<>();
        boolean[][] visited = new boolean[targetBoard.getSize()][targetBoard.getSize()];

        for (int x = 0; x < targetBoard.getSize(); x++) {
            for (int y = 0; y < targetBoard.getSize(); y++) {
                if (visited[x][y] || targetBoard.getCellState(x, y) != CellState.HIT) {
                    continue;
                }

                clusters.add(collectHitCluster(targetBoard, visited, x, y));
            }
        }

        return clusters;
    }

    private List<int[]> collectHitCluster(Board targetBoard, boolean[][] visited, int startX, int startY) {
        List<int[]> cluster = new ArrayList<>();
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { startX, startY });
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int[] cell = queue.remove();
            cluster.add(cell);

            int[][] neighbors = { { cell[0] + 1, cell[1] }, { cell[0] - 1, cell[1] },
                    { cell[0], cell[1] + 1 }, { cell[0], cell[1] - 1 } };
            for (int[] neighbor : neighbors) {
                int nx = neighbor[0];
                int ny = neighbor[1];
                if (nx < 0 || nx >= targetBoard.getSize() || ny < 0 || ny >= targetBoard.getSize()
                        || visited[nx][ny]) {
                    continue;
                }

                if (targetBoard.getCellState(nx, ny) == CellState.HIT) {
                    visited[nx][ny] = true;
                    queue.add(new int[] { nx, ny });
                }
            }
        }

        return cluster;
    }

    private List<int[]> chooseTargetCluster(List<List<int[]>> hitClusters) {
        List<int[]> bestCluster = hitClusters.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (List<int[]> cluster : hitClusters) {
            int score = cluster.size() * 20 + countCurrentHitOverlap(cluster) * 100;
            if (score > bestScore) {
                bestScore = score;
                bestCluster = cluster;
            }
        }

        return bestCluster;
    }

    private int countCurrentHitOverlap(List<int[]> cluster) {
        int overlap = 0;
        for (int[] hit : currentHits) {
            if (containsCell(cluster, hit[0], hit[1])) {
                overlap++;
            }
        }
        return overlap;
    }

    private List<int[]> getLineEndCandidates(Board targetBoard, List<int[]> hits) {
        List<int[]> candidates = new ArrayList<>();
        if (hits.size() < 2 || !areCollinear(hits)) {
            return candidates;
        }

        if (isSameX(hits)) {
            int x = hits.get(0)[0];
            int minY = hits.get(0)[1];
            int maxY = hits.get(0)[1];
            for (int[] hit : hits) {
                minY = Math.min(minY, hit[1]);
                maxY = Math.max(maxY, hit[1]);
            }
            addIfShootable(targetBoard, candidates, x, minY - 1);
            addIfShootable(targetBoard, candidates, x, maxY + 1);
        } else {
            int y = hits.get(0)[1];
            int minX = hits.get(0)[0];
            int maxX = hits.get(0)[0];
            for (int[] hit : hits) {
                minX = Math.min(minX, hit[0]);
                maxX = Math.max(maxX, hit[0]);
            }
            addIfShootable(targetBoard, candidates, minX - 1, y);
            addIfShootable(targetBoard, candidates, maxX + 1, y);
        }

        return candidates;
    }

    private List<int[]> getAdjacentCandidates(Board targetBoard, List<int[]> hits) {
        List<int[]> candidates = new ArrayList<>();
        for (int[] hit : hits) {
            addIfShootable(targetBoard, candidates, hit[0] + 1, hit[1]);
            addIfShootable(targetBoard, candidates, hit[0] - 1, hit[1]);
            addIfShootable(targetBoard, candidates, hit[0], hit[1] + 1);
            addIfShootable(targetBoard, candidates, hit[0], hit[1] - 1);
        }
        return candidates;
    }

    private void addIfShootable(Board targetBoard, List<int[]> candidates, int x, int y) {
        if (x < 0 || x >= targetBoard.getSize() || y < 0 || y >= targetBoard.getSize()
                || !isShootable(targetBoard, x, y)) {
            return;
        }

        if (!containsCell(candidates, x, y)) {
            candidates.add(new int[] { x, y });
        }
    }

    private int[] pickBestTargetCandidate(Board targetBoard, List<int[]> candidates, List<int[]> targetCluster) {
        if (candidates.isEmpty()) {
            return null;
        }

        int bestScore = Integer.MIN_VALUE;
        List<int[]> bestCandidates = new ArrayList<>();
        for (int[] candidate : candidates) {
            int score = scoreTargetCandidate(targetBoard, candidate, targetCluster);
            if (score > bestScore) {
                bestScore = score;
                bestCandidates.clear();
                bestCandidates.add(candidate);
            } else if (score == bestScore) {
                bestCandidates.add(candidate);
            }
        }

        return bestCandidates.get(random.nextInt(bestCandidates.size()));
    }

    private int scoreTargetCandidate(Board targetBoard, int[] candidate, List<int[]> targetCluster) {
        int score = 1000;
        score += getCenterBonus(targetBoard.getSize(), candidate[0], candidate[1]) * CENTER_BONUS_WEIGHT;
        score += getFocusedHitAdjacencyBonus(targetCluster, candidate[0], candidate[1]) * 4;
        score -= getSunkNeighborPenalty(targetBoard, candidate[0], candidate[1]);

        List<Integer> remainingShipSizes = getRemainingShipSizes(targetBoard);
        boolean requireAllHits = targetCluster.size() > 1 && areCollinear(targetCluster);
        for (int shipSize : remainingShipSizes) {
            score += countCandidatePlacements(targetBoard, candidate, targetCluster, requireAllHits, shipSize, true) * 60;
            score += countCandidatePlacements(targetBoard, candidate, targetCluster, requireAllHits, shipSize, false) * 60;
        }

        return score;
    }

    private int countCandidatePlacements(Board targetBoard, int[] candidate, List<int[]> targetCluster,
            boolean requireAllHits, int shipSize, boolean vertical) {
        int count = 0;
        int maxX = vertical ? targetBoard.getSize() : targetBoard.getSize() - shipSize + 1;
        int maxY = vertical ? targetBoard.getSize() - shipSize + 1 : targetBoard.getSize();

        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                List<int[]> cells = buildPlacement(x, y, shipSize, vertical);
                if (!containsCell(cells, candidate[0], candidate[1]) || !isValidPlacement(targetBoard, cells)) {
                    continue;
                }

                int coveredHits = countCoveredHits(cells, targetCluster);
                if ((requireAllHits && coveredHits == targetCluster.size()) || (!requireAllHits && coveredHits > 0)) {
                    count++;
                }
            }
        }

        return count;
    }

    private void scoreShipPlacements(Board targetBoard, int[][] scores, List<int[]> focusedHits,
            boolean requireAllFocusedHits, int shipSize, boolean vertical) {
        int maxX = vertical ? targetBoard.getSize() : targetBoard.getSize() - shipSize + 1;
        int maxY = vertical ? targetBoard.getSize() - shipSize + 1 : targetBoard.getSize();

        for (int x = 0; x < maxX; x++) {
            for (int y = 0; y < maxY; y++) {
                List<int[]> cells = buildPlacement(x, y, shipSize, vertical);
                if (!isValidPlacement(targetBoard, cells)) {
                    continue;
                }

                int coveredHits = countCoveredHits(cells, focusedHits);
                if (!focusedHits.isEmpty() && coveredHits == 0) {
                    continue;
                }
                if (requireAllFocusedHits && coveredHits < focusedHits.size()) {
                    continue;
                }

                int weight = focusedHits.isEmpty() ? getHuntPlacementWeight() : getHitPlacementWeight() * coveredHits;
                for (int[] cell : cells) {
                    if (isShootable(targetBoard, cell[0], cell[1])) {
                        scores[cell[0]][cell[1]] += weight;
                    }
                }
            }
        }
    }

    private List<int[]> buildPlacement(int startX, int startY, int shipSize, boolean vertical) {
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < shipSize; i++) {
            int x = vertical ? startX : startX + i;
            int y = vertical ? startY + i : startY;
            cells.add(new int[] { x, y });
        }
        return cells;
    }

    private boolean isValidPlacement(Board targetBoard, List<int[]> cells) {
        for (int[] cell : cells) {
            CellState state = targetBoard.getCellState(cell[0], cell[1]);
            if (state == CellState.MISS || state == CellState.SUNK) {
                return false;
            }
        }
        return true;
    }

    private int countCoveredHits(List<int[]> cells, List<int[]> hits) {
        int count = 0;
        for (int[] hit : hits) {
            if (containsCell(cells, hit[0], hit[1])) {
                count++;
            }
        }
        return count;
    }

    private boolean containsCell(List<int[]> cells, int x, int y) {
        return cells.stream().anyMatch(cell -> cell[0] == x && cell[1] == y);
    }

    private List<int[]> getBoardHits(Board targetBoard) {
        List<int[]> hits = new ArrayList<>();
        for (int x = 0; x < targetBoard.getSize(); x++) {
            for (int y = 0; y < targetBoard.getSize(); y++) {
                if (targetBoard.getCellState(x, y) == CellState.HIT) {
                    hits.add(new int[] { x, y });
                }
            }
        }
        return hits;
    }

    private List<Integer> getRemainingShipSizes(Board targetBoard) {
        List<Integer> sizes = new ArrayList<>();
        for (Ship ship : targetBoard.getShips()) {
            if (!ship.isSunk()) {
                sizes.add(ship.getSize());
            }
        }
        return sizes;
    }

    private boolean areCollinear(List<int[]> hits) {
        return isSameX(hits) || isSameY(hits);
    }

    private boolean isSameX(List<int[]> hits) {
        boolean sameX = true;
        int x = hits.get(0)[0];

        for (int[] hit : hits) {
            sameX = sameX && hit[0] == x;
        }
        return sameX;
    }

    private boolean isSameY(List<int[]> hits) {
        boolean sameY = true;
        int y = hits.get(0)[1];

        for (int[] hit : hits) {
            sameY = sameY && hit[1] == y;
        }
        return sameY;
    }

    private int[] pickHighestScoreMove(Board targetBoard, int[][] scores, List<int[]> focusedHits) {
        int preferredParity = choosePreferredParity(scores);
        int bestScore = Integer.MIN_VALUE;
        List<int[]> bestMoves = new ArrayList<>();

        for (int x = 0; x < targetBoard.getSize(); x++) {
            for (int y = 0; y < targetBoard.getSize(); y++) {
                if (!isShootable(targetBoard, x, y)) {
                    continue;
                }

                int score = scores[x][y];
                if (score <= 0) {
                    continue;
                }

                score = adjustMoveScore(targetBoard, focusedHits, preferredParity, x, y, score);
                if (score > bestScore) {
                    bestScore = score;
                    bestMoves.clear();
                    bestMoves.add(new int[] { x, y });
                } else if (score == bestScore) {
                    bestMoves.add(new int[] { x, y });
                }
            }
        }

        if (bestMoves.isEmpty() || (!focusedHits.isEmpty() && bestScore <= 0)) {
            return null;
        }

        return bestMoves.get(random.nextInt(bestMoves.size()));
    }

    private int choosePreferredParity(int[][] scores) {
        int evenScore = 0;
        int oddScore = 0;
        for (int x = 0; x < scores.length; x++) {
            for (int y = 0; y < scores[x].length; y++) {
                if ((x + y) % 2 == 0) {
                    evenScore += scores[x][y];
                } else {
                    oddScore += scores[x][y];
                }
            }
        }
        return evenScore >= oddScore ? 0 : 1;
    }

    private int adjustMoveScore(Board targetBoard, List<int[]> focusedHits, int preferredParity, int x, int y,
            int baseScore) {
        int score = baseScore;
        if (focusedHits.isEmpty()) {
            score += getCenterBonus(targetBoard.getSize(), x, y) * CENTER_BONUS_WEIGHT;
            if ((x + y) % 2 == preferredParity) {
                score += CHECKERBOARD_BONUS;
            }
            score -= getSunkNeighborPenalty(targetBoard, x, y);
        } else {
            score += getFocusedHitAdjacencyBonus(focusedHits, x, y);
        }
        return score;
    }

    private int getCenterBonus(int size, int x, int y) {
        int distanceFromCenter = Math.abs((2 * x) - (size - 1)) + Math.abs((2 * y) - (size - 1));
        return Math.max(0, (2 * size) - 2 - distanceFromCenter);
    }

    private int getFocusedHitAdjacencyBonus(List<int[]> focusedHits, int x, int y) {
        int bonus = 0;
        for (int[] hit : focusedHits) {
            int distance = Math.abs(hit[0] - x) + Math.abs(hit[1] - y);
            if (distance == 1) {
                bonus += 20;
            }
        }
        return bonus;
    }

    private int getSunkNeighborPenalty(Board targetBoard, int x, int y) {
        int penalty = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || nx >= targetBoard.getSize() || ny < 0 || ny >= targetBoard.getSize()) {
                    continue;
                }

                if (targetBoard.getCellState(nx, ny) == CellState.SUNK) {
                    penalty += Math.abs(dx) + Math.abs(dy) == 1 ? ORTHOGONAL_SUNK_PENALTY : DIAGONAL_SUNK_PENALTY;
                }
            }
        }
        return penalty;
    }

    private int getHuntPlacementWeight() {
        return HUNT_PLACEMENT_WEIGHT;
    }

    private int getHitPlacementWeight() {
        return HIT_PLACEMENT_WEIGHT;
    }

    private boolean isShootable(Board targetBoard, int x, int y) {
        if (x < 0 || x >= targetBoard.getSize() || y < 0 || y >= targetBoard.getSize()) {
            return false;
        }
        CellState state = targetBoard.getCellState(x, y);
        return (state == CellState.WATER || state == CellState.SHIP) && !isAlreadyShot(x, y) && !isBlocked(x, y);
    }

    private boolean hasAvailableShot(Board targetBoard) {
        for (int x = 0; x < targetBoard.getSize(); x++) {
            for (int y = 0; y < targetBoard.getSize(); y++) {
                CellState state = targetBoard.getCellState(x, y);
                if ((state == CellState.WATER || state == CellState.SHIP) && !isAlreadyShot(x, y)
                        && !isBlocked(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBlocked(int x, int y) {
        return blockedCells.stream().anyMatch(cell -> cell[0] == x && cell[1] == y);
    }

    private int[] randomHunt(Board targetBoard) {
        int x, y;
        do {
            x = random.nextInt(boardSize);
            y = random.nextInt(boardSize);
        } while (targetBoard != null ? !isShootable(targetBoard, x, y) : isAlreadyShot(x, y));
        return new int[] { x, y };
    }

    public void notifyShipSunk() {
        targetMode = false;
        targetQueue.clear();
        currentHits.clear();
        lockedDirection = null;
    }

    public void recordHit(int x, int y) {
        if (difficulty == Difficulty.EASY) {
            targetMode = true;
            addNeighborTargets(x, y);
            return;
        }

        targetMode = true;
        addCurrentHit(x, y);

        if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {
            updateLockedDirection(x, y);
            if (lockedDirection != null) {
                targetQueue.clear();
                addLineTargets(x, y);
                return;
            }
        }

        addNeighborTargets(x, y);
    }

    private void addCurrentHit(int x, int y) {
        if (currentHits.stream().noneMatch(p -> p[0] == x && p[1] == y)) {
            currentHits.add(new int[] { x, y });
        }
    }

    private void addNeighborTargets(int x, int y) {
        List<int[]> neighbors = new ArrayList<>();
        neighbors.add(new int[] { x + 1, y });
        neighbors.add(new int[] { x - 1, y });
        neighbors.add(new int[] { x, y + 1 });
        neighbors.add(new int[] { x, y - 1 });
        Collections.shuffle(neighbors, random);
        for (int[] n : neighbors) {
            addIfValidAtFront(n[0], n[1]);
        }
    }

    private void updateLockedDirection(int x, int y) {
        if (lockedDirection != null) {
            return;
        }

        for (int[] hit : currentHits) {
            if (hit[0] == x && hit[1] == y) {
                continue;
            }

            if (hit[0] == x) {
                lockedDirection = Direction.VERTICAL;
                return;
            }

            if (hit[1] == y) {
                lockedDirection = Direction.HORIZONTAL;
                return;
            }
        }
    }

    private void addLineTargets(int x, int y) {
        if (lockedDirection == Direction.HORIZONTAL) {
            int minX = x;
            int maxX = x;
            for (int[] hit : currentHits) {
                if (hit[1] == y) {
                    minX = Math.min(minX, hit[0]);
                    maxX = Math.max(maxX, hit[0]);
                }
            }
            addIfValidAtFront(maxX + 1, y);
            addIfValidAtFront(minX - 1, y);
        } else if (lockedDirection == Direction.VERTICAL) {
            int minY = y;
            int maxY = y;
            for (int[] hit : currentHits) {
                if (hit[0] == x) {
                    minY = Math.min(minY, hit[1]);
                    maxY = Math.max(maxY, hit[1]);
                }
            }
            addIfValidAtFront(x, maxY + 1);
            addIfValidAtFront(x, minY - 1);
        }
    }

    private void addIfValidAtFront(int nx, int ny) {
        if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && !isAlreadyShot(nx, ny)) {
            // Xóa ô nếu đã tồn tại trong queue để đưa nó lên vị trí ưu tiên đầu tiên
            targetQueue.removeIf(p -> p[0] == nx && p[1] == ny);
            targetQueue.add(0, new int[] { nx, ny });
        }
    }

    private boolean isAlreadyShot(int x, int y) { // Kiểm tra xem ô được bắn chưa
        return botShots.stream().anyMatch(s -> s[0] == x && s[1] == y);
    }
}
