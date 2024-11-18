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

public class RegisterActivity extends AppCompatActivity {

    private View rootView = null;
    private EditText usernameEdit = null;
    private EditText emailEdit = null;
    private EditText passwordEdit = null;
    private Button registerButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        this.rootView = findViewById(R.id.register_activity_root);
        this.usernameEdit = findViewById(R.id.register_activity_username_edit);
        this.emailEdit = findViewById(R.id.register_activity_email_edit);
        this.passwordEdit = findViewById(R.id.register_activity_password_edit);
        this.registerButton = findViewById(R.id.register_activity_register_button);

        this.registerButton.setOnClickListener(view -> {
            String username = usernameEdit.getText().toString();
            String email = emailEdit.getText().toString();
            String password = passwordEdit.getText().toString();

            User user = new User();
            user.username = username;
            user.email = email;
            user.password = password;

            new UsersApiClient(this).register(user)
                    .thenAcceptAsync(user1 -> new LocalUsers().addUser(RegisterActivity.this, user1).join())
                    .thenAccept(unused -> RegisterActivity.this.finish())
                    .exceptionally(throwable -> {
                        ApiException.showError(RegisterActivity.this.rootView, throwable);
                        return null;
                    });
        });
    }
}