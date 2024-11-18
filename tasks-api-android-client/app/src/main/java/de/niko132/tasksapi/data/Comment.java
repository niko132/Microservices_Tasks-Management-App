package de.niko132.tasksapi.data;

import java.util.Objects;

public class Comment {

    public long id;
    public long taskId;
    public long userId;
    public String username;
    public String text;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment user = (Comment) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
