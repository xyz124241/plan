package de.deinname.pvpplugin.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Party {

    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private String modeKey;
    private boolean ffa;

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public boolean isLeader(UUID id) { return leader.equals(id); }
    public Set<UUID> getMembers() { return members; }
    public String getModeKey() { return modeKey; }
    public void setModeKey(String modeKey) { this.modeKey = modeKey; }
    public boolean isFfa() { return ffa; }
    public void setFfa(boolean ffa) { this.ffa = ffa; }
}
