package de.butler_kassensysteme.twa;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissionRequestActivity extends AppCompatActivity {
    private static final String PERMISSIONS = "PERMISSIONS";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            PermissionRequestActivity.this::onPermissionResult
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissionLauncher.launch(getIntent().getStringArrayExtra(PERMISSIONS));

        finish();
    }

    void onPermissionResult(Map<String, Boolean> result) {
        List<String> deniedPermissions = result.entrySet().stream()
                .filter(it -> !it.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        /*
        // If we receive a response to our permission check, initialize
        if (deniedPermissions.isEmpty() && !Terminal.isInitialized() && verifyGpsEnabled()) {
            initialize();
        }
        */
    }

    public static void requestPermissions(Context context, String[] permissions) {
        Intent intent = new Intent(context, PermissionRequestActivity.class);
        intent.putExtra(PERMISSIONS, permissions);
        context.startActivity(intent);
    }
}
