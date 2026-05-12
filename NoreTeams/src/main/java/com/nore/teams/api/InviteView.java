package com.nore.teams.api;

import java.util.UUID;

public record InviteView(UUID partyId, String partyName, UUID ownerId, String ownerName) {
}
