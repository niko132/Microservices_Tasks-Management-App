package de.niko132.tasksapi.data;

import java.util.Objects;

public class User {

    public long id;
    public String email;
    public String username;
    public String token;

    public String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
