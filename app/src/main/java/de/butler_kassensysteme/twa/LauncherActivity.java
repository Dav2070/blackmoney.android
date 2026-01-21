/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.butler_kassensysteme.twa;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.core.content.ContextCompat;

import com.google.androidbrowserhelper.BuildConfig;
import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback;
import com.stripe.stripeterminal.external.callable.ReaderCallback;
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener;
import com.stripe.stripeterminal.external.models.CollectPaymentIntentConfiguration;
import com.stripe.stripeterminal.external.models.ConfirmPaymentIntentConfiguration;
import com.stripe.stripeterminal.external.models.ConnectionConfiguration;
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.PaymentIntentParameters;
import com.stripe.stripeterminal.external.models.PaymentMethodType;
import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.TerminalException;
import com.stripe.stripeterminal.log.LogLevel;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class LauncherActivity extends com.google.androidbrowserhelper.trusted.LauncherActivity {
    private CustomTabsClient mClient = null;
    public static CustomTabsSession mSession = null;
    private final Uri URL = Uri.parse("https://blackmoney.ngrok.app/");
    private final String TAG = "butler";
    static Map<String, CompletableFuture<String>> postMessageResolvers = new HashMap<>();
    AtomicReference<Reader> readerReference = new AtomicReference<>();

    private final PaymentIntentCallback createPaymentIntentCallback = new PaymentIntentCallback() {
        @Override
        public void onSuccess(@NonNull PaymentIntent paymentIntent) {
            // Placeholder for handling successful operation
            Terminal.getInstance().processPaymentIntent(
                    paymentIntent,
                    new CollectPaymentIntentConfiguration.Builder().build(),
                    new ConfirmPaymentIntentConfiguration.Builder().build(),
                    processPaymentIntentCallback
            );
        }

        @Override
        public void onFailure(@NonNull TerminalException e) {
            // Placeholder for handling exception
            Log.d(TAG, "Failure in PaymentIntentCallback");
        }
    };

    private final PaymentIntentCallback processPaymentIntentCallback = new PaymentIntentCallback() {
        @Override
        public void onSuccess(@NonNull PaymentIntent paymentIntent) {
            String id = paymentIntent.getId();
            System.out.println("processPaymentIntent succeeded: " + id);

            if (id != null) {
                // Notify your backend to capture the PaymentIntent
                postMessage(new PostMessageObject("capturePaymentIntent", id), null);
            } else {
                // TODO
                System.out.println("Payment collected offline");
            }
        }

        @Override
        public void onFailure(@NonNull TerminalException e) {
            System.out.println("processPaymentIntent failed: " + e);
        }
    };

    private final ReaderCallback connectReaderCallback = new ReaderCallback() {
        @Override
        public void onSuccess(@NonNull Reader reader) {
            // Placeholder for handling successful operation
            Log.d(TAG, "Success in ReaderCallback");

            Terminal.getInstance().createPaymentIntent(
                    new PaymentIntentParameters.Builder().setAmount(100L).setCurrency("eur").build(),
                    createPaymentIntentCallback
            );
        }

        @Override
        public void onFailure(@NonNull TerminalException e) {
            // Placeholder for handling exception
            Log.d(TAG, "Failure in ReaderCallback");
        }
    };

    private final Callback discoverReadersCallback = new Callback() {
        @Override
        public void onSuccess() {
            // Placeholder for handling successful operation
            Reader reader = readerReference.get();

            if (reader != null) {
                Terminal.getInstance().connectReader(
                        reader,
                        new ConnectionConfiguration.TapToPayConnectionConfiguration(
                                "tml_GWN4AAdCp2mNjl",
                                true,
                                new TapToPayReaderListener() {}
                        ),
                        connectReaderCallback
                );
            }
        }

        @Override
        public void onFailure(@NonNull TerminalException e) {
            // Placeholder for handling exception
            Log.d(TAG, "Failure");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        bindCustomTabsService();
    }

    @Override
    protected Uri getLaunchingUrl() {
        // Get the original launch Url.
        return super.getLaunchingUrl();
    }

    public static void postMessage(PostMessageObject data, @Nullable CompletableFuture<String> future) {
        if (mSession == null) return;

        if (future != null) {
            postMessageResolvers.put(data.type, future);
        }

        mSession.postMessage(data.toString(), null);
    }

    private final CustomTabsCallback customTabsCallback =
            new CustomTabsCallback() {
                @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
                @Override
                public void onPostMessage(@NonNull String message, @Nullable Bundle extras) {
                    super.onPostMessage(message, extras);
                    String type = "";

                    try {
                        JSONObject json = new JSONObject(message);
                        type = json.getString("type");

                        switch (type) {
                            case "startPayment":
                                requestPermissionsIfNecessary();

                                if (!Terminal.isInitialized()) {
                                    // Initialize the Terminal as soon as possible
                                    try {
                                        Terminal.init(
                                                getApplicationContext(), LogLevel.VERBOSE, new TokenProvider(),
                                                new TerminalEventListener(), null);
                                    } catch (TerminalException e) {
                                        throw new RuntimeException("Location services are required to initialize the Terminal.", e);
                                    }
                                }

                                DiscoveryConfiguration.TapToPayDiscoveryConfiguration config = new DiscoveryConfiguration.TapToPayDiscoveryConfiguration(true);

                                Terminal.getInstance().discoverReaders(
                                        config,
                                        readers -> {
                                            if (!readers.isEmpty()) {
                                                readerReference.set(readers.get(0));
                                            }
                                        },
                                        discoverReadersCallback
                                );
                                break;
                            default:
                                CompletableFuture<String> resolver = postMessageResolvers.get(type);
                                if (resolver == null) return;

                                if (type.equals("createStripeConnectionToken")) {
                                    resolver.complete(json.getString("secret"));
                                }

                                postMessageResolvers.remove(type);
                                break;
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onRelationshipValidationResult(
                        int relation,
                        @NonNull Uri requestedOrigin,
                        boolean result,
                        @Nullable Bundle extras
                ) {
                    // If this fails:
                    // - Have you called warmup?
                    // - Have you set up Digital Asset Links correctly?
                    // - Double check what browser you're using.
                    if (!result) {
                        Log.d(TAG, "Validation failed.");
                    }
                }

                // Listens for navigation, requests the postMessage channel when one completes.
                @Override
                public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
                    if (navigationEvent != NAVIGATION_FINISHED) {
                        return;
                    }

                    // If this fails:
                    // - Have you included PostMessageService in your AndroidManifest.xml?
                    boolean result = mSession.requestPostMessageChannel(URL, URL, new Bundle());

                    if (!result) {
                        Log.d(TAG, "PostMessage channel request failed.");
                    }
                }

                @Override
                public void onMessageChannelReady(@Nullable Bundle extras) {
                    Log.d(TAG, "Message channel ready." + (mSession != null));
                }
            };

    private void bindCustomTabsService() {
        String packageName = CustomTabsClient.getPackageName(this, null);
        assert packageName != null;

        CustomTabsClient.bindCustomTabsService(this, packageName,
                new CustomTabsServiceConnection() {
                    @Override
                    public void onCustomTabsServiceConnected(
                            @NonNull ComponentName name,
                            @NonNull CustomTabsClient client
                    ) {
                        mClient = client;

                        // Note: validateRelationship requires warmup to have been called.
                        client.warmup(0L);

                        mSession = mClient.newSession(customTabsCallback);

                        launch();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {
                        mClient = null;
                    }
                });
    }

    private void launch() {
        new TrustedWebActivityIntentBuilder(URL).build(mSession)
                .launchTrustedWebActivity(LauncherActivity.this);
    }

    private void requestPermissionsIfNecessary() {
        // Check for location and bluetooth permissions
        List<String> deniedPermissions = new ArrayList<>();

        if (!isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            deniedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            deniedPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (!isGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            deniedPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        if (!deniedPermissions.isEmpty()) {
            // If we don't have them yet, request them before doing anything else
            //requestPermissionLauncher.launch(deniedPermissions.toArray(new String[0]));
            PermissionRequestActivity.requestPermissions(this, deniedPermissions.toArray(new String[deniedPermissions.size()]));
        } else if (!Terminal.isInitialized() && verifyGpsEnabled()) {
            initialize();
        }
    }

    private Boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(
                this,
                permission
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean verifyGpsEnabled() {
        // TODO: Check if GPS is enabled and prompt the user to enable it if not.
        // For this example, we'll just return true.
        return true;
    }

    private void initialize() {
        // TODO: Initialize the Stripe Terminal SDK.
        Log.d(TAG, "Stripe Terminal is ready to be initialized.");
    }
}
