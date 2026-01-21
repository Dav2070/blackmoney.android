package de.butler_kassensysteme.twa;

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider;

import java.util.concurrent.CompletableFuture;

public class TokenProvider implements ConnectionTokenProvider {
    @Override
    public void fetchConnectionToken(ConnectionTokenCallback callback) {
        CompletableFuture<String> future = new CompletableFuture<>();

        LauncherActivity.postMessage(new PostMessageObject("createStripeConnectionToken", null), future);
        String result = future.join();

        callback.onSuccess(result);
    }
}
