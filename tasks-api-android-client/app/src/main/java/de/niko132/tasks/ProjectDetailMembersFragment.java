package de.niko132.tasks;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.niko132.tasks.utils.MainFunction;
import de.niko132.tasksapi.ApiException;
import de.niko132.tasksapi.ProjectsApiClient;
import de.niko132.tasksapi.UsersApiClient;
import de.niko132.tasksapi.data.Project;
import de.niko132.tasksapi.data.User;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProjectDetailMembersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProjectDetailMembersFragment extends Fragment {

    private static final String ARG_USER = "user";
    private static final String ARG_PROJECT = "project";

    private User user;
    private Project project;

    public ProjectDetailMembersFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ProjectDetailMembersFragment.
     */
    public static ProjectDetailMembersFragment newInstance(User user, Project project) {
        ProjectDetailMembersFragment fragment = new ProjectDetailMembersFragment();
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

        refreshList();
    }

    private void refreshList() {
        new ProjectsApiClient(getContext(), this.user.token)
                .getProjectMembers(this.project.id)
                .thenApplyAsync(memberIds -> {
                    UsersApiClient usersApiClient = new UsersApiClient(getContext(), user.token);
                    return Arrays.stream(memberIds)
                            .map(memberId -> usersApiClient.getUser(memberId).join())
                            .collect(Collectors.toList());
                })
                .thenCompose(new MainFunction<>(getContext()) {
                    @Override
                    public Object applyOnMain(List<User> users) {
                        ProjectDetailMembersFragment.this.memberListRecyclerAdapter.setUsers(users);
                        return null;
                    }
                })
                .exceptionally(throwable -> {
                    ApiException.showError(ProjectDetailMembersFragment.this.getView(), throwable);
                    return null;
                });
    }

    private RecyclerView membersList = null;
    private FloatingActionButton addMemberButton = null;

    private MemberListRecyclerAdapter memberListRecyclerAdapter = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_project_detail_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.membersList = view.findViewById(R.id.project_detail_fragment_members_list);
        this.addMemberButton = view.findViewById(R.id.project_detail_fragment_add_member_button);

        this.membersList.setLayoutManager(new LinearLayoutManager(getContext()));
        this.memberListRecyclerAdapter = new MemberListRecyclerAdapter();
        this.membersList.setAdapter(this.memberListRecyclerAdapter);

        this.addMemberButton.setOnClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = requireActivity().getLayoutInflater();

            View rootView = inflater.inflate(R.layout.fragment_project_detail_add_member_dialog, null);
            EditText emailEdit = rootView.findViewById(R.id.project_detail_fragment_add_member_dialog_email_edit);
            builder.setTitle("Add Member")
                    .setView(rootView)
                    .setPositiveButton("Add", (dialog, id) -> {
                        new UsersApiClient(getContext(), user.token)
                                .getUsers(emailEdit.getText().toString())
                                .thenComposeAsync(users -> {
                                    if (users == null || users.length == 0 || users[0] == null)
                                        throw new RuntimeException("User does not exist");

                                    project.memberIds.add(users[0].id);
                                    return new ProjectsApiClient(getContext(), user.token)
                                            .saveProject(project);
                                }).thenAccept(project -> {
                                    ProjectDetailMembersFragment.this.project = project;
                                    refreshList();
                                }).exceptionally(throwable -> {
                                    ApiException.showError(ProjectDetailMembersFragment.this.getView(), throwable);
                                    return null;
                                });
                        dialog.cancel();
                    })
                    .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
            builder.create().show();
        });
    }

    public class MemberListRecyclerAdapter extends RecyclerView.Adapter<MemberListRecyclerAdapter.MemberViewHolder> {

        public static class MemberViewHolder extends RecyclerView.ViewHolder {

            TextView usernameText;
            TextView emailText;
            ImageButton deleteButton;

            public MemberViewHolder(@NonNull View itemView) {
                super(itemView);

                this.usernameText = itemView.findViewById(R.id.project_detail_fragment_row_item_user_username);
                this.emailText = itemView.findViewById(R.id.project_detail_fragment_row_item_user_email);
                this.deleteButton = itemView.findViewById(R.id.project_detail_fragment_row_item_delete_button);
            }
        }

        private List<User> users = Collections.emptyList();

        public void setUsers(List<User> users) {
            this.users = users;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_project_detail_row_item_member, parent, false);
            return new MemberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            User user = this.users.get(position);
            holder.usernameText.setText(user.username);
            holder.emailText.setText(user.email);

            holder.deleteButton.setOnClickListener(view -> {
                List<User> usersCopy = new ArrayList<>(users);
                usersCopy.remove(position);
                project.memberIds = usersCopy.stream().map(user1 -> user1.id).collect(Collectors.toList());
                new ProjectsApiClient(getContext(), ProjectDetailMembersFragment.this.user.token)
                        .saveProject(project)
                        .thenCompose(new MainFunction<>(getContext()) {
                            @Override
                            public Object applyOnMain(Project project) {
                                ProjectDetailMembersFragment.this.project = project;
                                refreshList();
                                return null;
                            }
                        }).exceptionally((Function<Throwable, Void>) throwable -> {
                            ApiException.showError(getView(), throwable);
                            return null;
                        });
            });
        }

        @Override
        public int getItemCount() {
            return this.users.size();
        }

    }
}