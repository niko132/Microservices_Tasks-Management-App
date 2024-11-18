package de.niko132.tasks;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

import de.niko132.tasksapi.ApiException;
import de.niko132.tasksapi.UsersApiClient;
import de.niko132.tasksapi.data.User;

public class LoginActivity extends AppCompatActivity {

    private View rootView = null;
    private EditText emailEdit = null;
    private EditText passwordEdit = null;
    private Button loginButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.rootView = findViewById(R.id.login_activity_root);
        this.emailEdit = findViewById(R.id.login_activity_email_edit);
        this.passwordEdit = findViewById(R.id.login_activity_password_edit);
        this.loginButton = findViewById(R.id.login_activity_login_button);

        this.loginButton.setOnClickListener(view -> {
            String email = emailEdit.getText().toString();
            String password = passwordEdit.getText().toString();

            User user = new User();
            user.email = email;
            user.password = password;

            new UsersApiClient(this).login(user)
                    .thenAcceptAsync(user1 -> new LocalUsers().addUser(LoginActivity.this, user1).join())
                    .thenAccept(unused -> LoginActivity.this.finish())
                    .exceptionally(throwable -> {
                        ApiException.showError(LoginActivity.this.rootView, throwable);
                        return null;
                    });
        });
    }
}