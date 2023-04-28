package com.gamersafer.minecraft.abbacaving.lobby;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LobbyQueue {

    private final String mapName;
    private final int maxPlayers;
    private final int requiredPlayers;
    private final List<UUID> playerQueue;

    private int counter = 0;
    private QueueState state;
    private boolean forceStart = false;
    private final List<CompletableFuture<?>> waitingFutures = new ArrayList<>();

    public LobbyQueue(final String mapName, final int maxPlayers, final List<UUID> playerQueue, final int required) {
        this.mapName = mapName;
        this.maxPlayers = maxPlayers;
        this.playerQueue = playerQueue;
        this.state = QueueState.WAITING;
        this.requiredPlayers = required;
    }

    public boolean acceptingNewPlayers() {
        return this.playerQueue.size() < this.maxPlayers;
    }

    public void addPlayer(final UUID uuid) {
        this.playerQueue().add(uuid);
    }

    public void removePlayer(final UUID uuid) {
        this.playerQueue().remove(uuid);
    }

    public int maxPlayers() {
        return this.maxPlayers;
    }

    public String mapName() {
        return this.mapName;
    }

    public List<UUID> playerQueue() {
        return this.playerQueue;
    }

    public QueueState state() {
        return this.state;
    }

    public void state(final QueueState state) {
        this.state = state;
    }

    public int counter() {
        return this.counter;
    }

    public void counter(final int value) {
        this.counter = value;
    }

    public int incrementCounter() {
        return ++this.counter;
    }

    public int decrementCounter() {
        return --this.counter;
    }

    public boolean forceStart() {
        return this.forceStart;
    }

    public void forceStart(final boolean forceStart) {
        this.forceStart = forceStart;
    }

    public void addWaitingFuture(final CompletableFuture<?> future) {
        this.waitingFutures.add(future);
    }

    public boolean isWaitingForFutures() {
        for (final CompletableFuture<?> future : this.waitingFutures) {
            if (!future.isDone()) {
                return true;
            }
        }

        return false;
    }

    public void clearWaitingFutures() {
        this.waitingFutures.clear();
    }

    public int startPlayeramount() {
        return this.requiredPlayers;
    }

}
