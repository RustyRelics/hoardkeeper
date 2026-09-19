package com.rustyrelic.hytale.hoardkeeper;

import java.util.List;
import java.util.Map;

/**
 * The result of one StackEngine.run() call — rule 8: what moved where, and what stayed and why.
 * <p>
 * Items that stayed in inventory are split into two reasons, decided here (not by the command that
 * prints them) because deciding "why" is a rule, and StackEngine is the only class that knows the
 * rules:
 * <ul>
 *   <li>noEligibleChest — no chest in range already held this item (rule 1: a chest that doesn't
 *       already have a stack of it is never a destination), so it was never attempted anywhere.</li>
 *   <li>chestsFull — at least one chest in range did already hold this item and was tried, but had no
 *       room left for the rest.</li>
 * </ul>
 */
public class StackReport {

    public record ChestResult(BlockPos position, int movedCount) {
    }

    private final List<ChestResult> chestResults;
    private final int totalMoved;
    private final Map<String, Integer> noEligibleChest;
    private final Map<String, Integer> chestsFull;
    private final int skippedExcludedCount;

    public StackReport(List<ChestResult> chestResults, int totalMoved, Map<String, Integer> noEligibleChest,
                        Map<String, Integer> chestsFull, int skippedExcludedCount) {
        this.chestResults = chestResults;
        this.totalMoved = totalMoved;
        this.noEligibleChest = noEligibleChest;
        this.chestsFull = chestsFull;
        this.skippedExcludedCount = skippedExcludedCount;
    }

    public List<ChestResult> getChestResults() {
        return chestResults;
    }

    public int getTotalMoved() {
        return totalMoved;
    }

    public Map<String, Integer> getNoEligibleChest() {
        return noEligibleChest;
    }

    public Map<String, Integer> getChestsFull() {
        return chestsFull;
    }

    public int getSkippedExcludedCount() {
        return skippedExcludedCount;
    }

    /**
     * The chat report text — the same for /hk stack and the Hoardstone, since both just print
     * whatever StackEngine decided happened.
     */
    public String toChatMessage() {
        StringBuilder out = new StringBuilder();
        out.append("Hoardkeeper: moved ").append(totalMoved).append(" item(s)");
        if (skippedExcludedCount > 0) {
            out.append(" (skipped ").append(skippedExcludedCount).append(" excluded chest(s))");
        }
        out.append('\n');

        for (ChestResult chestResult : chestResults) {
            out.append("  ").append(chestResult.position()).append(": ")
                    .append(chestResult.movedCount()).append(" moved\n");
        }

        for (Map.Entry<String, Integer> entry : chestsFull.entrySet()) {
            out.append("  ").append(entry.getKey()).append(" x").append(entry.getValue())
                    .append(" stayed in inventory -- chests holding it are full\n");
        }

        for (Map.Entry<String, Integer> entry : noEligibleChest.entrySet()) {
            out.append("  ").append(entry.getKey()).append(" x").append(entry.getValue())
                    .append(" stayed in inventory -- nothing in range holds it\n");
        }

        return out.toString().stripTrailing();
    }

}
