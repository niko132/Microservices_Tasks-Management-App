package de.niko132.tasksapi.data;

import java.util.List;
import java.util.Objects;

public class Project {

    public long id;
    public String name;
    public long ownerId;
    public List<Long> memberIds;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project user = (Project) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
