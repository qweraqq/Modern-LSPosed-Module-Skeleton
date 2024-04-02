package com.shen1991.lsp;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;


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
