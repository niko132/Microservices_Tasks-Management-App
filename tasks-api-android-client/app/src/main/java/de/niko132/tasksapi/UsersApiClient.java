package de.niko132.tasksapi;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import de.niko132.tasksapi.data.User;

public class UsersApiClient extends ApiClientBase {

    public UsersApiClient(Context context) {
        super(context);
    }

    public UsersApiClient(Context context, String token) {
        super(context, token);
    }

    public CompletableFuture<User> register(User user) {
        return post("/users", user, User.class);
    }

    public CompletableFuture<User> login(User user) {
        return post("/users/login", user, User.class);
    }

    public CompletableFuture<User[]> getUsers() {
        return getUsers(null);
    }

    public CompletableFuture<User[]> getUsers(String email) {
        Map<String, String> queryParams = new HashMap<>();
        if (email != null) queryParams.put("email", email);
        return get("/users", queryParams, User[].class);
    }

    public CompletableFuture<User> getUser(long userId) {
        return get("/users/" + userId, User.class);
    }

}