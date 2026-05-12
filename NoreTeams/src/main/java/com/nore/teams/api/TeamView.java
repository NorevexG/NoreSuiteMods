package com.nore.teams.api;

import java.util.List;
import java.util.UUID;

public record TeamView(UUID partyId, String name, List<MemberView> members, List<AllyView> allies) {
    public boolean contains(UUID playerId) {
        return members.stream().anyMatch(member -> member.playerId().equals(playerId));
    }
}
