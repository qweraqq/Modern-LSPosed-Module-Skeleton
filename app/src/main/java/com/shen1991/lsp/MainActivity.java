package com.shen1991.lsp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import com.shen1991.lsp.databinding.ActivityMainBinding;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class MainActivity extends Activity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onServiceBind(@NonNull XposedService service) {

                SharedPreferences prefs = service.getRemotePreferences(Constants.LSP_SETTINGS_NAME);
                binding.env.setText("TARGET APP: " + prefs.getString(Constants.LSP_PROPERTY_NAME, null));
                prefs.registerOnSharedPreferenceChangeListener((p, s) ->
                        {
                            if (s != null && s.equals(Constants.LSP_PROPERTY_NAME)) {
                                binding.env.setText("TARGET APP: " + prefs.getString(Constants.LSP_PROPERTY_NAME, null));
                            }
                        }
                );

                binding.requestRefresh.setOnClickListener(view ->
                    binding.env.setText("TARGET APP: " + prefs.getString(Constants.LSP_PROPERTY_NAME, null))
                );

                binding.setApp.setOnClickListener(view -> {
                    String app = null;
                    if (binding.targetAppInput.getText() != null) {
                        app = binding.targetAppInput.getText().toString().strip();
                        if (app.isBlank()){
                            app = null;
                        }
                    }
                    prefs.edit().putString(Constants.LSP_PROPERTY_NAME, app).apply();
                    binding.env.setText("TARGET APP: " + prefs.getString(Constants.LSP_PROPERTY_NAME, null));
                });

            }

            @Override
            public void onServiceDied(@NonNull XposedService xposedService) {

            }
        });

    }

}