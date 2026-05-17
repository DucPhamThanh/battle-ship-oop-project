package com.battleship.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.battleship.model.Board;
import com.battleship.model.CellState;
import org.junit.Test;

public class BotAITest {
    @Test
    public void hardBotTargetsAdjacentCellAfterSingleHit() {
        BotAI bot = new BotAI();
        bot.setDifficulty(BotAI.Difficulty.HARD);
        Board board = new Board();
        board.setCellState(4, 4, CellState.HIT);

        int[] move = bot.calculateNextMove(board);

        int distance = Math.abs(move[0] - 4) + Math.abs(move[1] - 4);
        assertEquals(1, distance);
    }

    @Test
    public void hardBotContinuesFromOpenLineEndAfterDirectionKnown() {
        BotAI bot = new BotAI();
        bot.setDifficulty(BotAI.Difficulty.HARD);
        Board board = new Board();
        board.setCellState(4, 4, CellState.HIT);
        board.setCellState(5, 4, CellState.HIT);
        board.setCellState(3, 4, CellState.MISS);

        int[] move = bot.calculateNextMove(board);

        assertEquals(6, move[0]);
        assertEquals(4, move[1]);
    }

    @Test
    public void hardBotChoosesOneOfTwoLineEndsWhenBothAreOpen() {
        BotAI bot = new BotAI();
        bot.setDifficulty(BotAI.Difficulty.HARD);
        Board board = new Board();
        board.setCellState(4, 4, CellState.HIT);
        board.setCellState(5, 4, CellState.HIT);

        int[] move = bot.calculateNextMove(board);

        boolean isLeftEnd = move[0] == 3 && move[1] == 4;
        boolean isRightEnd = move[0] == 6 && move[1] == 4;
        assertTrue(isLeftEnd || isRightEnd);
    }
}
