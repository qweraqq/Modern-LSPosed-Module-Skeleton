package com.shen1991.lsp;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public interface IHook {
    public void processHook(XposedModule xposedModule,
                            XposedModuleInterface.PackageLoadedParam param);
}
