package ldes.client.treenodesupplier.domain.entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class MemberVersionRecord {
    private final String versionOf;
    private final LocalDateTime timestamp;

    public MemberVersionRecord(String versionOf, LocalDateTime timestamp) {
        this.versionOf = versionOf;
        this.timestamp = timestamp;
    }

    public String getVersionOf() {
        return versionOf;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberVersionRecord that)) return false;
        return Objects.equals(versionOf, that.versionOf) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionOf, timestamp);
    }
}
