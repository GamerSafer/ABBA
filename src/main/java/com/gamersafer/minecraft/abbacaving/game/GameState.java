package com.gamersafer.minecraft.abbacaving.game;

public enum GameState {

    RUNNING("In Game"),
    DONE("Game Over");

    private final String displayName;

    GameState(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

}
