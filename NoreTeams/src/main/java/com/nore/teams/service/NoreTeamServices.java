package com.nore.teams.service;

public final class NoreTeamServices {
    private static final NoreTeamService TEAMS = new OpenPacReflectiveTeamService();

    private NoreTeamServices() {
    }

    public static NoreTeamService teams() {
        return TEAMS;
    }
}
