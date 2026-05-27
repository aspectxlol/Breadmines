package com.aspectxlol.breadmines.general.recipes;

public final class RecipeDefinition {

    private final String outputKey;
    private final String inputKey;
    private final int inputAmount;
    private final long createdAtMillis;
    private final long updatedAtMillis;

    public RecipeDefinition(String outputKey, String inputKey, int inputAmount, long createdAtMillis, long updatedAtMillis) {
        this.outputKey = outputKey;
        this.inputKey = inputKey;
        this.inputAmount = inputAmount;
        this.createdAtMillis = createdAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public String getInputKey() {
        return inputKey;
    }

    public int getInputAmount() {
        return inputAmount;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }
}
