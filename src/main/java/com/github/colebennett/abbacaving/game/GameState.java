package com.github.colebennett.abbacaving.game;

public enum GameState {

    WAITING("Waiting"),
    STARTING("Starting"),
    RUNNING("In Game"),
    DONE("Game Over");

    private final String displayName;

    GameState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
