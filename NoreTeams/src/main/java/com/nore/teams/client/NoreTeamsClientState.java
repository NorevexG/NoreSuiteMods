package com.nore.teams.client;

import com.nore.teams.network.NoreTeamsNetwork;

public final class NoreTeamsClientState {
    private static NoreTeamsNetwork.SnapshotPayload snapshot = emptySnapshot();

    private NoreTeamsClientState() {
    }

    public static NoreTeamsNetwork.SnapshotPayload snapshot() {
        return snapshot;
    }

    public static void setSnapshot(NoreTeamsNetwork.SnapshotPayload payload) {
        snapshot = payload;
    }

    public static void clear() {
        snapshot = emptySnapshot();
    }

    private static NoreTeamsNetwork.SnapshotPayload emptySnapshot() {
        return new NoreTeamsNetwork.SnapshotPayload("", false, false, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of());
    }
}
