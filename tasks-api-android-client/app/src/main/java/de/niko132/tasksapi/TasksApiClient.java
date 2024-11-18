package de.niko132.tasksapi;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import de.niko132.tasksapi.data.Comment;
import de.niko132.tasksapi.data.Task;

public class TasksApiClient extends ApiClientBase {

    public TasksApiClient(Context context, String token) {
        super(context, token);
    }

    public CompletableFuture<Task[]> getTasks(long projectId) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("projectId", String.valueOf(projectId));
        return get("/tasks", queryParams, Task[].class);
    }

    public CompletableFuture<Task> getTask(String taskId) {
        return get("/task/" + taskId, Task.class);
    }

    public CompletableFuture<Task> saveTask(Task task) {
        return put("/tasks/" + task.id, task, Task.class);
    }

    public CompletableFuture<Task> addTask(Task task) {
        return post("/tasks", task, Task.class);
    }

    public CompletableFuture<Task> deleteTask(long taskId) {
        return delete("/tasks/" + taskId, Task.class);
    }

    public CompletableFuture<Comment[]> getComments(long taskId) {
        return get("/tasks/" + taskId + "/comments", Comment[].class);
    }

    public CompletableFuture<Comment> addComment(Comment comment, long taskId) {
        return post("/tasks/" + taskId + "/comments", comment, Comment.class);
    }

}