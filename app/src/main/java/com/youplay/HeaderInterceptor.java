package com.youplay;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by tan on 16/02/17.
 **/

public class HeaderInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder newRequest = request.newBuilder();
        newRequest.header("key", BuildConfig.youtube_key);
        return chain.proceed(newRequest.build());
    }
}
