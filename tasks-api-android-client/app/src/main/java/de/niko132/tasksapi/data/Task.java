package de.niko132.tasksapi.data;

import java.util.List;
import java.util.Objects;

public class Task {

    public long id;
    public String title;
    public String description;
    public String status;

    public long projectId;
    public List<Long> assigneeIds;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task user = (Task) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
