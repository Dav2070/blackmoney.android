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

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import com.google.androidbrowserhelper.BuildConfig;

import org.jspecify.annotations.NonNull;

public class LauncherActivity extends com.google.androidbrowserhelper.trusted.LauncherActivity {
    private CustomTabsClient mClient = null;
    private CustomTabsSession mSession = null;
    private final Uri URL = Uri.parse("https://butler-kassensysteme.de/");
    private final String TAG = "butler";

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

    private final CustomTabsCallback customTabsCallback =
            new CustomTabsCallback() {
                @Override
                public void onPostMessage(@NonNull String message, @Nullable Bundle extras) {
                    super.onPostMessage(message, extras);

                    Log.d(TAG, "Got message: " + message);
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
                    Log.d(TAG, "Message channel ready.");
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
}
