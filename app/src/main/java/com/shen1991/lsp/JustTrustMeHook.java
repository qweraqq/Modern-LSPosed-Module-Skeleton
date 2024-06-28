package com.shen1991.lsp;

import android.annotation.SuppressLint;
import android.net.http.X509TrustManagerExtensions;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

public class JustTrustMeHook implements IHook{
    private static final String TAG = "[LSP-DEMO-JustTrustMe] ";

    @XposedHooker
    @SuppressWarnings("all")
    public class DoNothingHooker implements XposedInterface.Hooker {
        @BeforeInvocation
        @SuppressWarnings("unused")
        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback){
            callback.returnAndSkip(null);
        }
        @AfterInvocation
        @SuppressWarnings("unused")
        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback){

        }
    }

    @Keep
    @SuppressLint("CustomX509TrustManager")
    @SuppressWarnings("all")
    private static class ImSureItsLegitTrustManager implements X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        @SuppressWarnings("all")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @SuppressLint("TrustAllX509TrustManager")
        @SuppressWarnings("all")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @SuppressWarnings("all")
        public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String host) throws CertificateException {
            return new ArrayList<>();
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


    @Keep
    @SuppressWarnings("unused")
    private static javax.net.ssl.SSLSocketFactory getEmptySSLFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new ImSureItsLegitTrustManager()}, null);
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }
    }

    @Override
    public void processHook(XposedModule xposedModule, XposedModuleInterface.PackageLoadedParam param) {


        for (Method method : X509TrustManagerExtensions.class.getDeclaredMethods()) {
            if (method.getName().equals("checkServerTrusted") &&
                    method.getParameterCount() == 3 &&
                    method.getParameterTypes()[0] == X509Certificate[].class &&
                    method.getParameterTypes()[1] == String.class &&
                    method.getParameterTypes()[2] == String.class) {
                xposedModule.log(TAG + "hook " + X509TrustManagerExtensions.class.getCanonicalName() + "." + method.getName());
                @XposedHooker
                class Hooker implements XposedInterface.Hooker {
                    @BeforeInvocation
                    @SuppressWarnings("unused")
                    public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback) {
                        List<X509Certificate> list = new ArrayList<>();
                        Collections.addAll(list, (X509Certificate[]) callback.getArgs()[0]);
                        callback.returnAndSkip(list);
                    }

                    @AfterInvocation
                    @SuppressWarnings("unused")
                    public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback) {

                    }
                }
                xposedModule.hook(method, Hooker.class);
            }
        }


        Class<?> networkSecurityTrustManagerClazz = XXHelpers.findClassIfExists(
                "android.security.net.config.NetworkSecurityTrustManager",
                param.getClassLoader()
        );
        if (networkSecurityTrustManagerClazz != null) {
            for (Method method: networkSecurityTrustManagerClazz.getDeclaredMethods()){
                if(method.getName().equals("checkPins") &&
                        method.getParameterCount() ==1 &&
                        method.getParameterTypes()[0] == List.class){
                    xposedModule.log(TAG + "hook " + networkSecurityTrustManagerClazz.getCanonicalName() + "." + method.getName());
                    @XposedHooker
                    class Hooker implements XposedInterface.Hooker {
                        @BeforeInvocation
                        @SuppressWarnings("unused")
                        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback) {
                            callback.returnAndSkip(null);
                        }

                        @AfterInvocation
                        @SuppressWarnings("unused")
                        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback) {

                        }
                    }
                    xposedModule.hook(method, Hooker.class);
                }
            }
        }

        Class<?> trustManagerFactoryClazz = XXHelpers.findClassIfExists(
                "javax.net.ssl.TrustManagerFactory",
                param.getClassLoader()
        );


        if(trustManagerFactoryClazz != null){
            for(Method method: trustManagerFactoryClazz.getDeclaredMethods()){
                if(method.getName().equals("getTrustManagers")){
                    xposedModule.log(TAG + "hook " + trustManagerFactoryClazz.getCanonicalName() + "." + method.getName());
                    @XposedHooker
                    class Hooker implements XposedInterface.Hooker {
                        @BeforeInvocation
                        @SuppressWarnings("unused")
                        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback) {
                            callback.returnAndSkip(null);
                        }

                        @SuppressLint("PrivateApi")
                        @AfterInvocation
                        @SuppressWarnings("unused")
                        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback) {


                            Class<?> trustManagerImplClazz = null;
                            try {
                                trustManagerImplClazz = Class.forName("com.android.org.conscrypt.TrustManagerImpl");
                            } catch (Exception ignore){

                            }
                            if (trustManagerImplClazz != null) {

                                TrustManager[] managers = (TrustManager[]) callback.getResult();
                                if (managers != null && managers.length > 0 && trustManagerImplClazz.isInstance(managers[0]))
                                    return;
                            }
                            callback.setResult(new TrustManager[]{new ImSureItsLegitTrustManager()});
                        }
                    }
                    xposedModule.hook(method, Hooker.class);
                }
            }
        }


        Class<?> httpsURLConnectionClazz = XXHelpers.findClassIfExists(
                "javax.net.ssl.HttpsURLConnection",
                param.getClassLoader()
        );
        if (httpsURLConnectionClazz != null) {
            for (Method method: httpsURLConnectionClazz.getDeclaredMethods()){
                if (method.getName().equals("setDefaultHostnameVerifier") ||
                        method.getName().equals("setSSLSocketFactory") ||
                        method.getName().equals("setHostnameVerifier")){
                    xposedModule.log(TAG + "hook " + httpsURLConnectionClazz.getCanonicalName() + "." + method.getName());
                    xposedModule.hook(method, DoNothingHooker.class);
                }
            }
        }


        Class<?> webViewClientClazz = XXHelpers.findClassIfExists(
                "android.webkit.WebViewClient",
                param.getClassLoader()
        );
        if (webViewClientClazz != null) {
            for(Method method: webViewClientClazz.getDeclaredMethods()){
                if(method.getName().equals("onReceivedSslError") && method.getParameterTypes()[1] == android.webkit.SslErrorHandler.class){
                    xposedModule.log(TAG + "hook " + webViewClientClazz.getCanonicalName() + "." + method.getName());
                    @XposedHooker
                    class Hooker implements XposedInterface.Hooker {
                        @BeforeInvocation
                        @SuppressWarnings("unused")
                        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback) {
                            ((android.webkit.SslErrorHandler) callback.getArgs()[1]).proceed();
                            callback.returnAndSkip(null);
                        }

                        @AfterInvocation
                        @SuppressWarnings("unused")
                        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback) {
                        }
                    }
                    xposedModule.hook(method, Hooker.class);
                }

                if(method.getName().equals("onReceivedError")){
                    xposedModule.log(TAG + "hook " + webViewClientClazz.getCanonicalName() + "." + method.getName());
                    xposedModule.hook(method, DoNothingHooker.class);

                }

            }
        }


        Class<?> SSLContextClazz = XXHelpers.findClassIfExists(
                "javax.net.ssl.SSLContext",
                param.getClassLoader()
        );
        if(SSLContextClazz != null) {
            for(Method method : SSLContextClazz.getDeclaredMethods()){
                if (method.getName().equals("init") &&
                        method.getParameterCount() == 3 &&
                        method.getParameterTypes()[0] == KeyManager[].class &&
                        method.getParameterTypes()[1] == TrustManager[].class &&
                        method.getParameterTypes()[2] == SecureRandom.class){
                    xposedModule.log(TAG + "hook " + SSLContextClazz.getCanonicalName() + "." + method.getName());
                    @XposedHooker
                    class Hooker implements XposedInterface.Hooker {
                        @BeforeInvocation
                        @SuppressWarnings("unused")
                        public static void beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback) {
                            callback.getArgs()[0] = null;
                            callback.getArgs()[1] = new TrustManager[]{new ImSureItsLegitTrustManager()};
                            callback.getArgs()[2] = null;
                        }

                        @AfterInvocation
                        @SuppressWarnings("unused")
                        public static void afterInvocation(@NonNull XposedInterface.AfterHookCallback callback) {
                        }
                    }
                    xposedModule.hook(method, Hooker.class);
                }
            }
        }

        Class<?> certificatePinnerClazz = XXHelpers.findClassIfExists(
                "okhttp3.CertificatePinner",
                param.getClassLoader()
        );
        if (certificatePinnerClazz != null) {
            for (Method method: certificatePinnerClazz.getDeclaredMethods()){
                if(method.getName().equals("check") &&
                        method.getParameterCount() == 2 &&
                        method.getParameterTypes()[0] == String.class &&
                        method.getParameterTypes()[1] == List.class){
                    xposedModule.log(TAG + "hook " + certificatePinnerClazz.getCanonicalName() + "." + method.getName());
                    xposedModule.hook(method, DoNothingHooker.class);
                }
            }
        }

    }

}
