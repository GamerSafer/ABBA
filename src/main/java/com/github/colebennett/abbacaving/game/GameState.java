package com.github.colebennett.abbacaving.game;

public enum GameState {

    WAITING("Waiting"),
    STARTING("Starting"),
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
