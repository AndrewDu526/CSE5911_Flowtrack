package model;

import java.util.Objects;

public class BeaconId {
    public final String type;
    public final String uuid;
    public final int major, minor;

    public BeaconId(String type, String uuid, int major, int minor) {
        this.type = type.toLowerCase();
        this.uuid = uuid.toLowerCase();
        this.major = major;
        this.minor = minor;
    }

    @Override
    public String toString() {
        return type + ":" + uuid + "-" + major + "-" + minor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeaconId)) return false;
        BeaconId other = (BeaconId) o;
        return type.equals(other.type)
                && uuid.equals(other.uuid)
                && major == other.major
                && minor == other.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, uuid, major, minor);
    }
}
