package com.nebuxe.mobileoffloading;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final int PERMISSIONS_REQUEST_CODE = 111;

    private LinearLayout mainContainer;
    private Button bMaster, bWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setEventListeners();
        mainContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String[] ungrantedPermissions = checkPermissions();
        Log.d("OFLOD", ungrantedPermissions + "");
        if (ungrantedPermissions.length > 0) {
            askPermissions(ungrantedPermissions);
        } else {
            mainContainer.setVisibility(View.VISIBLE);
        }
    }

    private void bindViews() {
        mainContainer = findViewById(R.id.main_container);
        bMaster = findViewById(R.id.b_master);
        bWorker = findViewById(R.id.b_worker);
    }

    private void setEventListeners() {
        bMaster.setOnClickListener((view) -> {
            startClientDiscoveryActivity();
        });


        bWorker.setOnClickListener((view) -> {
            startWorkAdvertisementActivity();
        });
    }

    private void startClientDiscoveryActivity() {
        Intent intent = new Intent(getApplicationContext(), ClientDiscoveryActivity.class);
        startActivity(intent);
    }

    private void startWorkAdvertisementActivity() {
        Intent intent = new Intent(getApplicationContext(), WorkAdvertisementActivity.class);
        startActivity(intent);
    }

    private String[] checkPermissions() {

        ArrayList<String> ungrantedPermissions = new ArrayList<>();

        try {

            String packageName = getPackageName();
            PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            String[] permissions = packageInfo.requestedPermissions;

            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ungrantedPermissions.add(permission);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String[] _ungrantedPermissions = new String[ungrantedPermissions.size()];
        _ungrantedPermissions = ungrantedPermissions.toArray(_ungrantedPermissions);
        return _ungrantedPermissions;
    }

    private void askPermissions(String[] requiredPermissions) {
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSIONS_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(getApplicationContext(), "Please provide all necessary permissions", Toast.LENGTH_LONG).show();
                    onBackPressed();
                    finish();
                }
            }
        }
    }

}