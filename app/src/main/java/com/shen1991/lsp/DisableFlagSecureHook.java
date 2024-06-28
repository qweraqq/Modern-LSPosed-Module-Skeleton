package com.shen1991.lsp;

import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class DisableFlagSecureHook implements IHook{
    private static final String TAG = "[LSP-DEMO-FlagSecure] ";
    @XposedHooker
    @SuppressWarnings("all")
    public class ReturnFalseHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        @SuppressWarnings("unused")
        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback){
            callback.returnAndSkip(false);
        }
        @AfterInvocation
        @SuppressWarnings("unused")
        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback){

        }
    }

    @XposedHooker
    @SuppressWarnings("all")
    public class RemoveSecureFlagHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        @SuppressWarnings("unused")
        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback){
            int flags = (int) callback.getArgs()[0];
            flags = flags & (~WindowManager.LayoutParams.FLAG_SECURE);
            callback.getArgs()[0] = flags;
        }
        @AfterInvocation
        @SuppressWarnings("unused")
        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback){

        }
    }

    @XposedHooker
    @SuppressWarnings("all")
    public class RemoveSetSecureHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        @SuppressWarnings("unused")
        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback){
            callback.getArgs()[0] = false;
        }
        @AfterInvocation
        @SuppressWarnings("unused")
        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback){

        }
    }

    @XposedHooker
    @SuppressWarnings("all")
    public class RemoveSecureParamHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        @SuppressWarnings("unused")
        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback){
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) callback.getArgs()[1];
            params.flags = params.flags & (~WindowManager.LayoutParams.FLAG_SECURE);
            callback.getArgs()[1] = params;
        }
        @AfterInvocation
        @SuppressWarnings("unused")
        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback){

        }
    }

    @Override
    public void processHook(XposedModule xposedModule, XposedModuleInterface.PackageLoadedParam param) {
        // 1
        for(Method method: Window.class.getDeclaredMethods()){
            if(method.getName().equals("setFlags") &&
                    method.getParameterCount() == 2 &&
                    method.getParameterTypes()[0] == int.class &&
                    method.getParameterTypes()[1] == int.class) {
                xposedModule.log(TAG + "hook " + Window.class.getCanonicalName() + "." + method.getName());
                xposedModule.hook(method, RemoveSecureFlagHooker.class);
            }
        }

        for(Method method: SurfaceView.class.getDeclaredMethods()){
            if(method.getName().equals("setSecure") &&
                    method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0] == boolean.class) {
                xposedModule.log(TAG + "hook " + SurfaceView.class.getCanonicalName() + "." + method.getName());
                xposedModule.hook(method, RemoveSetSecureHooker.class);
            }
        }

        Class<?> windowManagerGlobalClazz = XXHelpers.findClassIfExists(
                "android.view.WindowManagerGlobal",
                param.getClassLoader()
        );
        if (windowManagerGlobalClazz != null) {
            for (Method method : windowManagerGlobalClazz.getDeclaredMethods()) {
                if ((method.getName().equals("addView") || method.getName().equals("updateViewLayout")) &&
                        method.getParameterCount() >= 2 &&
                        method.getParameterTypes()[1] == ViewGroup.LayoutParams.class) {
                    xposedModule.log(TAG + "hook " + windowManagerGlobalClazz.getCanonicalName() + "." + method.getName());
                    xposedModule.hook(method, RemoveSecureParamHooker.class);
                }
            }
        }
    }
}
