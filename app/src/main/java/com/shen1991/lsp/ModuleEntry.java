package com.shen1991.lsp;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.Objects;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@SuppressWarnings("unused")
public class ModuleEntry extends XposedModule{
    static final String TAG = "[LSP-DEMO] ";

    private static final JustTrustMeHook justTrustMeHook = new JustTrustMeHook();
    private static final DisableFlagSecureHook disableFlagSecureHook = new DisableFlagSecureHook();
    private static final ClassloaderHook classloaderHook = new ClassloaderHook();
    public static XposedModule module;


    public ModuleEntry(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        ModuleEntry.module = this;
    }


    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);

        // Remote Preferences
        // https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API
        SharedPreferences sharedPreferences = this.getRemotePreferences(Constants.LSP_SETTINGS_NAME);
        String target = sharedPreferences.getString(Constants.LSP_PROPERTY_NAME, null);

        if(target == null) {
            return;
        }

        if (! param.getPackageName().contains(target)){
            return;
        }

        ModuleEntry.module.log(TAG + "----------");
        ModuleEntry.module.log(TAG + "onPackageLoaded: " + param.getPackageName());
        ModuleEntry.module.log(TAG + "param classloader is " + param.getClassLoader());
        ModuleEntry.module.log(TAG + "module apk path: " + this.getApplicationInfo().sourceDir);
        ModuleEntry.module.log(TAG + "----------");

        justTrustMeHook.processHook(module, param);
        disableFlagSecureHook.processHook(module, param);
        classloaderHook.processHook(module, param);
    }

}
