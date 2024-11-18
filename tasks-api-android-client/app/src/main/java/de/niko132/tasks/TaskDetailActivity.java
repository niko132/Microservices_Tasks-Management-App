package de.niko132.tasks;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.niko132.tasks.utils.MainFunction;
import de.niko132.tasksapi.ApiException;
import de.niko132.tasksapi.ProjectsApiClient;
import de.niko132.tasksapi.TasksApiClient;
import de.niko132.tasksapi.UsersApiClient;
import de.niko132.tasksapi.data.Comment;
import de.niko132.tasksapi.data.Project;
import de.niko132.tasksapi.data.Task;
import de.niko132.tasksapi.data.User;

public class TaskDetailActivity extends AppCompatActivity {

    private View rootView = null;
    private EditText taskNameEdit = null;
    private RecyclerView commentList = null;
    private EditText commentEdit = null;
    private ImageButton commentSendButton = null;

    private CommentListRecyclerAdapter commentListRecyclerAdapter = null;

    private User user = null;
    private Project project = null;
    private Task task = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        this.user = new Gson().fromJson(getIntent().getStringExtra("user"), User.class);
        this.project = new Gson().fromJson(getIntent().getStringExtra("project"), Project.class);
        this.task = new Gson().fromJson(getIntent().getStringExtra("task"), Task.class);

        Toolbar toolbar = findViewById(R.id.task_detail_activity_toolbar);
        setSupportActionBar(toolbar);

        this.rootView = findViewById(R.id.task_detail_activity_root);
        this.taskNameEdit = findViewById(R.id.task_detail_activity_task_name_edit);
        this.commentList = findViewById(R.id.task_detail_activity_comment_list);
        this.commentEdit = findViewById(R.id.task_detail_activity_comment_edit);
        this.commentSendButton = findViewById(R.id.task_detail_activity_comment_send_button);

        this.commentList.setLayoutManager(new LinearLayoutManager(this));
        this.commentListRecyclerAdapter = new CommentListRecyclerAdapter();
        this.commentList.setAdapter(this.commentListRecyclerAdapter);

        this.taskNameEdit.setText(task.title);
        this.taskNameEdit.setOnEditorActionListener((textView, i, keyEvent) -> {
            TaskDetailActivity.this.task.title = textView.getText().toString();
            saveTask();
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
            return true;
        });

        this.commentSendButton.setOnClickListener(view -> {
            String text = commentEdit.getText().toString();
            if (text.isBlank()) return;

            Comment comment = new Comment();
            comment.userId = user.id;
            comment.taskId = task.id;
            comment.text = text;

            new TasksApiClient(TaskDetailActivity.this, user.token)
                    .addComment(comment, task.id)
                    .thenCompose(new MainFunction<>(TaskDetailActivity.this) {
                        @Override
                        public Object applyOnMain(Comment comment) {
                            refreshContent();
                            return null;
                        }
                    })
                    .exceptionally((Function<Throwable, Void>) throwable -> {
                        ApiException.showError(rootView, throwable);
                        return null;
                    });


            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            commentEdit.setText("");
        });
    }

    private void saveTask() {
        new TasksApiClient(TaskDetailActivity.this, user.token)
                .saveTask(task)
                .thenCompose(new MainFunction<>(TaskDetailActivity.this) {
                    @Override
                    public Object applyOnMain(Task task) {
                        TaskDetailActivity.this.task = task;
                        refreshContent();
                        return null;
                    }
                })
                .exceptionally((Function<Throwable, Void>) throwable -> {
                    ApiException.showError(rootView, throwable);
                    return null;
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshContent();
    }

    private void refreshContent() {
        UsersApiClient usersApiClient = new UsersApiClient(this, user.token);
        CompletableFuture.supplyAsync(() -> project.memberIds.stream()
                .map(memberId -> usersApiClient.getUser(memberId).join())
                .collect(Collectors.toList())).thenCompose(new MainFunction<>(this) {
            @Override
            public Void applyOnMain(List<User> users) {
                commentListRecyclerAdapter.setUsers(users);
                return null;
            }
        }).exceptionally((Function<Throwable, Void>) throwable -> {
            ApiException.showError(rootView, throwable);
            return null;
        });

        new TasksApiClient(this, user.token)
                .getComments(task.id)
                .thenApplyAsync(comments -> {
                    Arrays.stream(comments).forEach(comment -> {
                        User author = usersApiClient.getUser(comment.userId).join();
                        comment.username = author.username;
                    });
                    return comments;
                })
                .thenCompose(new MainFunction<>(this) {
                    @Override
                    public Object applyOnMain(Comment[] comments) {
                        commentListRecyclerAdapter.setComments(List.of(comments));
                        return null;
                    }
                }).exceptionally((Function<Throwable, Void>) throwable -> {
                    ApiException.showError(rootView, throwable);
                    return null;
                });
    }

    public class CommentListRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public static class TaskViewHolder extends RecyclerView.ViewHolder {

            EditText descriptionEdit;
            RadioButton statusOpenButton;
            RadioButton statusInprogressButton;
            RadioButton statusClosedButton;
            Spinner assigneeDropdown;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);

                descriptionEdit = itemView.findViewById(R.id.task_detail_activity_row_item_task_description_edit);
                statusOpenButton = itemView.findViewById(R.id.task_detail_activity_row_item_task_status_open_button);
                statusInprogressButton = itemView.findViewById(R.id.task_detail_activity_row_item_task_status_inprogress_button);
                statusClosedButton = itemView.findViewById(R.id.task_detail_activity_row_item_task_status_closed_button);
                assigneeDropdown = itemView.findViewById(R.id.task_detail_activity_row_item_task_assignee_dropdown);
            }
        }

        public static class CommentViewHolder extends RecyclerView.ViewHolder {

            TextView userText;
            TextView textText;

            public CommentViewHolder(@NonNull View itemView) {
                super(itemView);

                this.userText = itemView.findViewById(R.id.task_detail_activity_row_item_comment_user);
                this.textText = itemView.findViewById(R.id.task_detail_activity_row_item_comment_text);
            }
        }

        private List<User> users = Collections.emptyList();
        private List<Comment> comments = Collections.emptyList();

        private void setUsers(List<User> users) {
            this.users = users;
            this.notifyItemChanged(0);
        }

        public void setComments(List<Comment> comments) {
            this.comments = comments;
            this.notifyItemRangeChanged(1, comments.size());
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_task_detail_row_item_task, parent, false);
                return new TaskViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_task_detail_row_item_comment, parent, false);
                return new CommentViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                TaskViewHolder taskViewHolder = (TaskViewHolder) holder;

                taskViewHolder.descriptionEdit.setRawInputType(InputType.TYPE_CLASS_TEXT);
                taskViewHolder.descriptionEdit.setText(task.description);
                taskViewHolder.descriptionEdit.setOnEditorActionListener((textView, i, keyEvent) -> {
                    task.description = textView.getText().toString();
                    saveTask();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
                });

                if ("open".equalsIgnoreCase(task.status)) taskViewHolder.statusOpenButton.setChecked(true);
                taskViewHolder.statusOpenButton.setOnCheckedChangeListener((compoundButton, checked) -> {
                    if (checked) {
                        task.status = "open";
                        saveTask();
                    }
                });

                if ("inprogress".equalsIgnoreCase(task.status)) taskViewHolder.statusInprogressButton.setChecked(true);
                taskViewHolder.statusInprogressButton.setOnCheckedChangeListener((compoundButton, checked) -> {
                    if (checked) {
                        task.status = "inprogress";
                        saveTask();
                    }
                });

                if ("closed".equalsIgnoreCase(task.status)) taskViewHolder.statusClosedButton.setChecked(true);
                taskViewHolder.statusClosedButton.setOnCheckedChangeListener((compoundButton, checked) -> {
                    if (checked) {
                        task.status = "closed";
                        saveTask();
                    }
                });

                List<User> dropdownUsers = new ArrayList<>();
                dropdownUsers.add(null);
                dropdownUsers.addAll(users);

                List<String> usersList = dropdownUsers.stream().map(user -> user != null ? user.username : "<unassigned>").collect(Collectors.toList());
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(taskViewHolder.itemView.getContext(), android.R.layout.simple_spinner_item, usersList);
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                taskViewHolder.assigneeDropdown.setAdapter(arrayAdapter);

                int userPosition = task.assigneeIds.stream().findFirst().map(userId -> {
                    for (int i = 0; i < users.size(); i++) {
                        if (users.get(i).id == userId) return i;
                    }
                    return null;
                }).map(i -> i + 1).orElse(0);
                taskViewHolder.assigneeDropdown.setSelection(userPosition, false);
                taskViewHolder.assigneeDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        User user = dropdownUsers.get(position);
                        List<Long> newAssignees = user != null ? List.of(user.id) : Collections.emptyList();
                        if (!newAssignees.equals(task.assigneeIds)) {
                            task.assigneeIds = newAssignees;
                            saveTask();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        task.assigneeIds = Collections.emptyList();
                        saveTask();
                    }
                });
            } else {
                position = position - 1;
                Comment comment = this.comments.get(position);

                CommentViewHolder commentViewHolder = (CommentViewHolder) holder;

                commentViewHolder.userText.setText(comment.username);
                commentViewHolder.textText.setText(comment.text);
            }
        }

        @Override
        public int getItemCount() {
            return this.comments.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }
    }
}