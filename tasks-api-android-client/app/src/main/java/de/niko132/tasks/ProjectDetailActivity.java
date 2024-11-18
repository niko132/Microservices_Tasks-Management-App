package de.niko132.tasks;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import de.niko132.tasks.utils.MainFunction;
import de.niko132.tasksapi.ProjectsApiClient;
import de.niko132.tasksapi.data.Project;
import de.niko132.tasksapi.data.User;

public class ProjectDetailActivity extends AppCompatActivity {

    private View rootView = null;
    private EditText projectNameEdit = null;
    private FrameLayout fragmentContainer = null;
    private BottomNavigationView bottomNavigationView = null;

    private ProjectDetailTasksFragment tasksFragment = null;
    private ProjectDetailMembersFragment membersFragment = null;

    private User user = null;
    private Project project = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        this.user = new Gson().fromJson(getIntent().getStringExtra("user"), User.class);
        this.project = new Gson().fromJson(getIntent().getStringExtra("project"), Project.class);

        Toolbar toolbar = findViewById(R.id.project_detail_activity_toolbar);
        setSupportActionBar(toolbar);

        this.rootView = findViewById(R.id.project_detail_activity_root);
        this.projectNameEdit = findViewById(R.id.project_detail_activity_project_name_edit);
        this.fragmentContainer = findViewById(R.id.project_detail_activity_fragment_container);
        this.bottomNavigationView = findViewById(R.id.project_detail_activity_bottom_navigation);

        this.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.project_detail_bottom_menu_tasks) {
                setCurrentFragment(this.tasksFragment);
            } else if (item.getItemId() == R.id.project_detail_bottom_menu_members) {
                setCurrentFragment(this.membersFragment);
            }
            return true;
        });

        this.projectNameEdit.setText(project.name);
        this.projectNameEdit.setOnEditorActionListener((textView, i, keyEvent) -> {
            ProjectDetailActivity.this.project.name = textView.getText().toString();
            new ProjectsApiClient(ProjectDetailActivity.this, user.token)
                    .saveProject(project)
                    .thenCompose(new MainFunction<>(ProjectDetailActivity.this) {
                        @Override
                        public Void applyOnMain(Project project) {
                            ProjectDetailActivity.this.project = project;
                            refreshContent();
                            return null;
                        }
                    });
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
            return true;
        });

        refreshContent();
    }

    private void refreshContent() {
        this.tasksFragment = ProjectDetailTasksFragment.newInstance(user, project);
        this.membersFragment = ProjectDetailMembersFragment.newInstance(user, project);
        this.bottomNavigationView.setSelectedItemId(this.bottomNavigationView.getSelectedItemId());
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.project_detail_activity_fragment_container, fragment)
                .commit();
    }
}