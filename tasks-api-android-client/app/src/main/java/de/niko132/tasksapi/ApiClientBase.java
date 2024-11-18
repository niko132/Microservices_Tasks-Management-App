package de.niko132.tasksapi;

import android.content.Context;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import de.niko132.tasks.R;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;
import okhttp3.CertificatePinner;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class ApiClientBase {

    public static final MediaType JSON = MediaType.get("application/json");

    private static final String API_URL_PREFIX = "https://185.128.119.187/api";

    private final OkHttpClient httpClient;
    private String token;

    public ApiClientBase(Context context) {
        this(context, null);
    }

    public ApiClientBase(Context context, String token) {
        try (InputStream is = context.getResources().openRawResource(R.raw.cert)) {
            X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(is);

            SSLFactory sslFactory = SSLFactory.builder()
                    .withTrustMaterial(trustManager)
                    .build();
            SSLSocketFactory sslSocketFactory = sslFactory.getSslSocketFactory();

            CertificatePinner certificatePinner = new CertificatePinner.Builder()
                    .add("185.128.119.187", "sha256/9GXgIbYs8khxZQVHpET0Dma9Dvie0uteMquUuSZPY6Y=")
                    .build();
            this.httpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .certificatePinner(certificatePinner)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public <T> CompletableFuture<T> get(String url, Class<T> c) {
        return get(url, Collections.emptyMap(), c);
    }

    public <T> CompletableFuture<T> get(String url, Map<String, String> params, Class<T> c) {
        HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(API_URL_PREFIX + url).newBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            httpUrlBuilder.addQueryParameter(param.getKey(), param.getValue());
        }
        Request request = newRequestBuilder()
                .url(httpUrlBuilder.build())
                .get()
                .build();
        return makeRequest(request, c);
    }

    public <T> CompletableFuture<T> post(String url, Object data, Class<T> c) {
        String json = new Gson().toJson(data);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = newRequestBuilder()
                .url(API_URL_PREFIX + url)
                .post(body)
                .build();
        return makeRequest(request, c);
    }

    public <T> CompletableFuture<T> put(String url, Object data, Class<T> c) {
        String json = new Gson().toJson(data);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = newRequestBuilder()
                .url(API_URL_PREFIX + url)
                .put(body)
                .build();
        return makeRequest(request, c);
    }

    public <T> CompletableFuture<T> delete(String url, Class<T> c) {
        Request request = newRequestBuilder()
                .url(API_URL_PREFIX + url)
                .delete()
                .build();
        return makeRequest(request, c);
    }

    private <T> CompletableFuture<T> makeRequest(Request request, Class<T> c) {
        return CompletableFuture.supplyAsync(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.body() == null) throw new CompletionException(new NullPointerException());
                if (!response.isSuccessful()) throw new CompletionException(new ApiException(response.body().string()));
                return new Gson().fromJson(response.body().string(), c);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private Request.Builder newRequestBuilder() {
        Request.Builder requestBuilder = new Request.Builder();
        if (this.token != null) requestBuilder = requestBuilder.header("Authorization", "Bearer: " + token);
        return requestBuilder;
    }

}
