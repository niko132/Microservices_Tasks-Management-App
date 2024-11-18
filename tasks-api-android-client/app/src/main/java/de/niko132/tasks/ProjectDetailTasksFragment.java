package de.niko132.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

import de.niko132.tasks.utils.MainFunction;
import de.niko132.tasksapi.ApiException;
import de.niko132.tasksapi.ProjectsApiClient;
import de.niko132.tasksapi.TasksApiClient;
import de.niko132.tasksapi.data.Project;
import de.niko132.tasksapi.data.Task;
import de.niko132.tasksapi.data.User;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProjectDetailTasksFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProjectDetailTasksFragment extends Fragment {

    private static final String ARG_USER = "user";
    private static final String ARG_PROJECT = "project";

    private User user;
    private Project project;

    public ProjectDetailTasksFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ProjectDetailTasksFragment.
     */
    public static ProjectDetailTasksFragment newInstance(User user, Project project) {
        ProjectDetailTasksFragment fragment = new ProjectDetailTasksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER, new Gson().toJson(user));
        args.putString(ARG_PROJECT, new Gson().toJson(project));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.user = new Gson().fromJson(getArguments().getString(ARG_USER), User.class);
            this.project = new Gson().fromJson(getArguments().getString(ARG_PROJECT), Project.class);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshContent();
    }

    private void refreshContent() {
        new ProjectsApiClient(getContext(), user.token)
                .getProject(project.id)
                .thenCompose(new MainFunction<Project, Project>(getContext()) {
                    @Override
                    public Project applyOnMain(Project project) {
                        ProjectDetailTasksFragment.this.project = project;
                        return project;
                    }
                })
                .thenComposeAsync(project -> new TasksApiClient(getContext(), user.token).getTasks(project.id))
                .thenCompose(new MainFunction<>(getContext()) {
                    @Override
                    public Void applyOnMain(Task[] tasks) {
                        ProjectDetailTasksFragment.this.taskListRecyclerAdapter.setTasks(new ArrayList<>(Arrays.asList(tasks)));
                        return null;
                    }
                })
                .exceptionally(throwable -> {
                    ApiException.showError(getView(), throwable);
                    return null;
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_project_detail_tasks, container, false);
    }

    private RecyclerView tasksList = null;
    private FloatingActionButton addTaskButton = null;

    private TaskListRecyclerAdapter taskListRecyclerAdapter = null;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.tasksList = view.findViewById(R.id.project_detail_fragment_tasks_list);
        this.addTaskButton = view.findViewById(R.id.project_detail_fragment_add_tasks_button);

        this.tasksList.setLayoutManager(new LinearLayoutManager(getContext()));
        this.taskListRecyclerAdapter = new TaskListRecyclerAdapter();
        this.tasksList.setAdapter(this.taskListRecyclerAdapter);

        this.addTaskButton.setOnClickListener(view1 -> {
            Task task = new Task();
            task.title = "New Task";
            task.description = "Enter your description";
            task.projectId = project.id;
            task.status = "open";

            new TasksApiClient(getContext(), user.token)
                    .addTask(task)
                    .thenCompose(new MainFunction<>(getContext()) {
                        @Override
                        public Object applyOnMain(Task task) {
                            Intent taskDetailActivityIntent = new Intent(view1.getContext(), TaskDetailActivity.class);
                            taskDetailActivityIntent.putExtra("user", new Gson().toJson(user));
                            taskDetailActivityIntent.putExtra("project", new Gson().toJson(project));
                            taskDetailActivityIntent.putExtra("task", new Gson().toJson(task));
                            view1.getContext().startActivity(taskDetailActivityIntent);
                            return null;
                        }
                    }).exceptionally((Function<Throwable, Void>) throwable -> {
                        ApiException.showError(getView(), throwable);
                        return null;
                    });
        });
    }

    public class TaskListRecyclerAdapter extends RecyclerView.Adapter<TaskListRecyclerAdapter.TaskViewHolder> {

        public static class TaskViewHolder extends RecyclerView.ViewHolder {

            TextView titleText;
            TextView descriptionText;
            ImageView statusImage;
            ImageButton deleteButton;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);

                this.titleText = itemView.findViewById(R.id.project_detail_fragment_row_item_task_title);
                this.descriptionText = itemView.findViewById(R.id.project_detail_fragment_row_item_task_description);
                this.statusImage = itemView.findViewById(R.id.project_detail_fragment_row_item_task_status_image);
                this.deleteButton = itemView.findViewById(R.id.project_detail_fragment_row_item_task_delete_button);
            }
        }

        private List<Task> tasks = Collections.emptyList();

        public void setTasks(List<Task> tasks) {
            this.tasks = tasks;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_project_detail_row_item_task, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            Task task = this.tasks.get(position);
            holder.titleText.setText(task.title);
            holder.descriptionText.setText(task.description);

            if ("open".equalsIgnoreCase(task.status)) {
                holder.statusImage.setImageResource(R.drawable.baseline_brightness_low_24);
            } else if ("inprogress".equalsIgnoreCase(task.status)) {
                holder.statusImage.setImageResource(R.drawable.baseline_brightness_medium_24);
            } else if ("closed".equalsIgnoreCase(task.status)) {
                holder.statusImage.setImageResource(R.drawable.baseline_brightness_high_24);
            } else {
                holder.statusImage.setImageResource(R.drawable.baseline_horizontal_rule_24);
            }

            holder.itemView.setOnClickListener(view -> {
                Intent taskDetailActivityIntent = new Intent(view.getContext(), TaskDetailActivity.class);
                taskDetailActivityIntent.putExtra("user", new Gson().toJson(user));
                taskDetailActivityIntent.putExtra("project", new Gson().toJson(project));
                taskDetailActivityIntent.putExtra("task", new Gson().toJson(task));
                view.getContext().startActivity(taskDetailActivityIntent);
            });

            holder.deleteButton.setOnClickListener(view -> new TasksApiClient(getContext(), user.token)
                    .deleteTask(task.id)
                    .thenCompose(new MainFunction<>(getContext()) {
                        @Override
                        public Void applyOnMain(Task task1) {
                            refreshContent();
                            return null;
                        }
                    }).exceptionally((Function<Throwable, Void>) throwable -> {
                        ApiException.showError(getView(), throwable);
                        return null;
                    }));
        }

        @Override
        public int getItemCount() {
            return this.tasks.size();
        }

    }
}