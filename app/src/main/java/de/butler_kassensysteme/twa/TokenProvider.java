package de.butler_kassensysteme.twa;

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider;

import java.util.concurrent.CompletableFuture;

import de.butler_kassensysteme.twa.Message.Message;

public class TokenProvider implements ConnectionTokenProvider {
    @Override
    public void fetchConnectionToken(ConnectionTokenCallback callback) {
        CompletableFuture<String> future = new CompletableFuture<>();

        LauncherActivity.postMessage(new Message("createStripeConnectionToken"), future);
        String result = future.join();

        callback.onSuccess(result);
    }
}
