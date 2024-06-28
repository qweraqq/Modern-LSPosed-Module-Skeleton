package com.shen1991.lsp;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class ClassloaderHook implements IHook{
    private static final String TAG = "[LSP-DEMO-Classloader] ";

    private static XposedModuleInterface.PackageLoadedParam lpparam;
    private static final DisableFlagSecureHook disableFlagSecureHook = new DisableFlagSecureHook();
    // private static final JustTrustMeHook justTrustMeHook = new JustTrustMeHook();


    @XposedHooker
    @SuppressWarnings("all")
    public class ClassloaderHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        @SuppressWarnings("unused")
        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback){
        }
        @AfterInvocation
        @SuppressWarnings("unused")
        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback){
            if (callback.getThrowable() != null) return;
            Class<?> cls = (Class<?>) callback.getResult();
            if (cls == null ) return;
            String name = cls.getName();
            if (! name.startsWith("java.") &&
                    ! name.startsWith("android.") &&
                    ! name.startsWith("com.shen1991") &&
                    ! name.startsWith("org.chromium") &&
                    ! name.startsWith("com.android") &&
                    ! name.startsWith("org.bouncycastle") &&
                    ! name.startsWith("javax") &&
                    ! name.startsWith("D2a") // proguard
            ) {
                ;
                ModuleEntry.module.log(TAG + "classLoader loadClass: " + name);
            }

            String packageName = lpparam.getPackageName();
            // hook encrypted class
            if (packageName.equalsIgnoreCase("SOME.ENCRYPTED.PACKAGE") &&
                    name.endsWith("SOME.DECRYPTED.CLASS")){

                // 防止截屏
                disableFlagSecureHook.processHook(ModuleEntry.module, lpparam);

                // SSL
                // justTrustMeHook.processHook(ModuleEntry.module, lpparam);

                // Some custom hook

                // Native inline hook
                // System.loadLibrary("xx");
            }

        }
    }

    @Override
    public void processHook(XposedModule xposedModule, XposedModuleInterface.PackageLoadedParam param) {
        lpparam = param;
        Class<?> classloaderClazz = XXHelpers.findClassIfExists(
                "java.lang.ClassLoader",
                param.getClassLoader());
        if (classloaderClazz != null) {
            for (Method m: classloaderClazz.getDeclaredMethods()) {
                if(m.getName().equals("loadClass") &&
                        m.getParameterCount() == 1 &&
                        m.getParameterTypes()[0] == String.class){
                    xposedModule.log(TAG + "hook " + classloaderClazz.getCanonicalName() + "." + m.getName());
                    xposedModule.hook(m, ClassloaderHooker.class);
                }
            }
        }
    }
}
