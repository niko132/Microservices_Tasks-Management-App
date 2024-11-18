package de.niko132.tasks;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import de.niko132.tasksapi.data.User;

public class LocalUsers {

    public CompletableFuture<List<User>> loadUsers(Context context) {
        CompletableFuture<List<User>> usersFuture =  CompletableFuture.supplyAsync(() -> {
            try {
                File usersFile = new File(context.getFilesDir(), "users.json");
                JsonReader jsonReader = new JsonReader(new FileReader(usersFile));
                Type type = new TypeToken<List<User>>(){}.getType();
                return new Gson().fromJson(jsonReader, type);
            } catch (FileNotFoundException e) {
                throw new CompletionException(e);
            }
        });
        return usersFuture.exceptionally(throwable -> Collections.emptyList());
    }

    public CompletableFuture<Void> storeUsers(Context context, List<User> users) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // cleanup
                List<User> usersList = new LinkedList<>(new HashSet<>(users));

                File usersFile = new File(context.getFilesDir(), "users.json");
                try (FileWriter fileWriter = new FileWriter(usersFile)) {
                    new Gson().toJson(usersList, fileWriter);
                }
                return null;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<Void> addUser(Context context, User user) {
        return loadUsers(context)
                .thenApply(users -> {
                    List<User> copy = new ArrayList<>(users);
                    copy.add(user);
                    return copy;
                })
                .thenCompose(users -> storeUsers(context, users));
    }

}
