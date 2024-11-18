package de.niko132.tasksapi;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletionException;

public class ApiException extends Exception {

    private String serverMessage = null;

    public ApiException() {
    }

    public ApiException(String message) {
        super(message);

        this.serverMessage = message;
        JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);
        if (jsonObject != null) {
            this.serverMessage = jsonObject.get("message").getAsString();
        }
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);

        this.serverMessage = message;
        JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);
        if (jsonObject != null) {
            this.serverMessage = jsonObject.get("message").getAsString();
        }
    }

    public ApiException(Throwable cause) {
        super(cause);
    }

    public ApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

        this.serverMessage = message;
        JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);
        if (jsonObject != null) {
            this.serverMessage = jsonObject.get("message").getAsString();
        }
    }

    public String getServerMessage() {
        return this.serverMessage;
    }

    public static void showError(View root, Throwable throwable) {
        if (throwable == null) return;

        ApiException apiException = null;
        if (throwable instanceof CompletionException) throwable = throwable.getCause();
        if (throwable == null) return;

        if (throwable instanceof ApiException) apiException = (ApiException) throwable;
        else if (throwable.getCause() != null && throwable.getCause() instanceof ApiException)
            apiException = (ApiException) throwable.getCause();

        String message = apiException != null ? apiException.getServerMessage() : throwable.getMessage();
        message = message != null ? message : "Unknown error";
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
    }
}
