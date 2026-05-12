package com.nore.teams.api;

import java.util.UUID;

public record PlayerView(UUID playerId, String username, boolean online) {
}
