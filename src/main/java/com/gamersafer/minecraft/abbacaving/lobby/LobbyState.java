package com.gamersafer.minecraft.abbacaving.lobby;

public enum LobbyState {

    WAITING("Waiting for players"),
    STARTING("Starting game");

    private final String displayName;

    LobbyState(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

}
