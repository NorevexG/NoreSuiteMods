package com.nore.teams.api;

import java.util.UUID;

public record MemberView(UUID playerId, String username, String rank, boolean owner) {
    public MemberView(UUID playerId, String username, String rank) {
        this(playerId, username, rank, false);
    }
}
