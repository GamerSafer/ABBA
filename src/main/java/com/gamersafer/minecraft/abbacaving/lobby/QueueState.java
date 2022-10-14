package com.gamersafer.minecraft.abbacaving.lobby;

public enum QueueState {

    WAITING("Waiting for players"),
    STARTING("Starting game"),
    LOCKED("Game started");

    private final String displayName;

    QueueState(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

}
