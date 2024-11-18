package de.niko132.tasksapi;

import android.content.Context;

import java.util.concurrent.CompletableFuture;

import de.niko132.tasksapi.data.Project;

public class ProjectsApiClient extends ApiClientBase {

    public ProjectsApiClient(Context context, String token) {
        super(context, token);
    }

    public CompletableFuture<Project[]> getProjects() {
        return get("/projects", Project[].class);
    }

    public CompletableFuture<Project> getProject(long projectId) {
        return get("/projects/" + projectId, Project.class);
    }

    public CompletableFuture<Project> saveProject(Project project) {
        return put("/projects/" + project.id, project, Project.class);
    }

    public CompletableFuture<Project> addProject(Project project) {
        return post("/projects", project, Project.class);
    }

    public CompletableFuture<Project> deleteProject(long projectId) {
        return delete("/projects/" + projectId, Project.class);
    }

    public CompletableFuture<Long[]> getProjectMembers(long projectId) {
        return get("/projects/" + projectId + "/members", Long[].class);
    }

}