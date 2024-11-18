package de.niko132.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import de.niko132.tasks.utils.MainFunction;
import de.niko132.tasksapi.ApiException;
import de.niko132.tasksapi.ProjectsApiClient;
import de.niko132.tasksapi.data.Project;
import de.niko132.tasksapi.data.User;

public class ProjectsActivity extends AppCompatActivity {

    private View rootView = null;
    private RecyclerView projectsList = null;
    private FloatingActionButton addProjectButton = null;

    private ProjectListRecyclerAdapter projectListRecyclerAdapter = null;

    private User user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        this.user = new Gson().fromJson(getIntent().getStringExtra("user"), User.class);

        this.rootView = findViewById(R.id.projects_activity_root);
        this.projectsList = findViewById(R.id.projects_activity_projects_list);
        this.addProjectButton = findViewById(R.id.projects_activity_add_project_button);

        this.projectsList.setLayoutManager(new LinearLayoutManager(this));
        this.projectListRecyclerAdapter = new ProjectListRecyclerAdapter();
        this.projectsList.setAdapter(this.projectListRecyclerAdapter);

        this.addProjectButton.setOnClickListener(view -> {
            Project project = new Project();
            project.name = "New Project";
            project.ownerId = user.id;
            project.memberIds = List.of(user.id);

            new ProjectsApiClient(ProjectsActivity.this, user.token)
                    .addProject(project)
                    .thenCompose(new MainFunction<>(ProjectsActivity.this) {
                        @Override
                        public Object applyOnMain(Project project) {
                            Intent projectDetailActivityIntent = new Intent(view.getContext(), ProjectDetailActivity.class);
                            projectDetailActivityIntent.putExtra("user", new Gson().toJson(user));
                            projectDetailActivityIntent.putExtra("project", new Gson().toJson(project));
                            startActivity(projectDetailActivityIntent);
                            return null;
                        }
                    }).exceptionally((Function<Throwable, Void>) throwable -> {
                        ApiException.showError(rootView, throwable);
                        return null;
                    });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshContent();
    }

    void refreshContent() {
        new ProjectsApiClient(this, this.user.token)
                .getProjects()
                .thenCompose(new MainFunction<>(this) {
                    @Override
                    public Void applyOnMain(Project[] projects) {
                        ProjectsActivity.this.projectListRecyclerAdapter.setProjects(List.of(projects));
                        return null;
                    }
                })
                .exceptionally(throwable -> {
                    ApiException.showError(ProjectsActivity.this.rootView, throwable);
                    return null;
                });
    }

    public class ProjectListRecyclerAdapter extends RecyclerView.Adapter<ProjectListRecyclerAdapter.ProjectViewHolder> {

        public static class ProjectViewHolder extends RecyclerView.ViewHolder {

            private TextView nameText;
            private ImageButton deleteButton;

            public ProjectViewHolder(@NonNull View itemView) {
                super(itemView);

                this.nameText = itemView.findViewById(R.id.projects_activity_row_item_project_name);
                this.deleteButton = itemView.findViewById(R.id.projects_activity_row_item_project_delete_button);
            }
        }

        private List<Project> projects = Collections.emptyList();

        public void setProjects(List<Project> projects) {
            this.projects = projects;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_projects_row_item_project, parent, false);
            return new ProjectViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
            Project project = this.projects.get(position);
            holder.nameText.setText(project.name);

            holder.itemView.setOnClickListener(view -> {
                Intent projectDetailActivityIntent = new Intent(view.getContext(), ProjectDetailActivity.class);
                projectDetailActivityIntent.putExtra("user", new Gson().toJson(user));
                projectDetailActivityIntent.putExtra("project", new Gson().toJson(project));
                startActivity(projectDetailActivityIntent);
            });

            holder.deleteButton.setOnClickListener(view -> {
                new ProjectsApiClient(ProjectsActivity.this, user.token)
                        .deleteProject(project.id)
                        .thenCompose(new MainFunction<>(ProjectsActivity.this) {
                            @Override
                            public Void applyOnMain(Project project1) {
                                refreshContent();
                                return null;
                            }
                        }).exceptionally((Function<Throwable, Void>) throwable -> {
                            ApiException.showError(rootView, throwable);
                            return null;
                        });
            });
        }

        @Override
        public int getItemCount() {
            return this.projects.size();
        }

    }
}