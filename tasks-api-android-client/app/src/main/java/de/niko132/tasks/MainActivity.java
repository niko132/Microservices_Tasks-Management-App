package de.niko132.tasks;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import de.niko132.tasks.utils.MainFunction;
import de.niko132.tasksapi.ApiException;
import de.niko132.tasksapi.data.User;

public class MainActivity extends AppCompatActivity {

    private View rootView = null;
    private RecyclerView usersList = null;
    private Button registerButton = null;
    private Button loginButton = null;

    private UserListRecyclerAdapter userListRecyclerAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.rootView = findViewById(R.id.main_activity_root);
        this.usersList = findViewById(R.id.users_activity_users_list);
        this.registerButton = findViewById(R.id.users_activity_register_button);
        this.loginButton = findViewById(R.id.users_activity_login_button);

        this.usersList.setLayoutManager(new LinearLayoutManager(this));
        this.userListRecyclerAdapter = new UserListRecyclerAdapter();
        this.usersList.setAdapter(this.userListRecyclerAdapter);

        this.registerButton.setOnClickListener(view -> {
            Intent registerActivityIntent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(registerActivityIntent);
        });

        this.loginButton.setOnClickListener(view -> {
            Intent loginActivityIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginActivityIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LocalUsers().loadUsers(this)
                .thenCompose(new MainFunction<>(this) {
                    @Override
                    public Void applyOnMain(List<User> users) {
                        MainActivity.this.userListRecyclerAdapter.setUsers(users);
                        return null;
                    }
                }).exceptionally(throwable -> {
                    ApiException.showError(rootView, throwable);
                    return null;
                });
    }

    public static class UserListRecyclerAdapter extends RecyclerView.Adapter<UserListRecyclerAdapter.UserViewHolder> {

        public static class UserViewHolder extends RecyclerView.ViewHolder {

            TextView usernameText;
            TextView emailText;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);

                this.usernameText = itemView.findViewById(R.id.users_activity_row_item_user_username);
                this.emailText = itemView.findViewById(R.id.users_activity_row_item_user_email);
            }
        }

        private List<User> users = Collections.emptyList();

        public void setUsers(List<User> users) {
            this.users = users;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_main_row_item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = this.users.get(position);
            holder.usernameText.setText(user.username);
            holder.emailText.setText(user.email);

            holder.itemView.setOnClickListener(view -> {
                Intent projectsActivityIntent = new Intent(view.getContext(), ProjectsActivity.class);
                projectsActivityIntent.putExtra("user", new Gson().toJson(user));
                view.getContext().startActivity(projectsActivityIntent);
            });
        }

        @Override
        public int getItemCount() {
            return this.users.size();
        }

    }
}