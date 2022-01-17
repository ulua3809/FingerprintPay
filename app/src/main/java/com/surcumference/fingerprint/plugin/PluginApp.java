package com.surcumference.fingerprint.plugin;

import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class PluginApp {

    private static PluginType sPluginType = null;
    private static PluginTarget sPluginTarget = null;

    public static void setup(PluginType pluginType, PluginTarget pluginTarget) {
        sPluginType = pluginType;
        sPluginTarget = pluginTarget;
    }

    public static void setup(String pluginTypeName, PluginTarget pluginTarget) {
        for (PluginType pluginType: PluginType.values()) {
            if (pluginType.name().equalsIgnoreCase(pluginTypeName)) {
                sPluginType = pluginType;
                sPluginTarget = pluginTarget;
                return;
            }
        }
        throw new RuntimeException("Unsupported plugin type:" + pluginTypeName);
    }

    public static PluginType getCurrentType() {
        if (sPluginType == null) {
            throw new NullPointerException("PluginApp not initial yet");
        }
        return sPluginType;
    }

    public static PluginTarget getCurrentTarget() {
        if (sPluginTarget == null) {
            throw new NullPointerException("PluginApp not initial yet");
        }
        return sPluginTarget;
    }

    public static<T> T runActionBaseOnCurrentPluginType(Map<PluginType, Callable<T>> actionMap) {
        PluginType pluginType = PluginApp.getCurrentType();
        switch (pluginType) {
            case Riru:
            case Zygisk:
            case Xposed:
                if (!actionMap.containsKey(pluginType)) {
                    throw new IllegalArgumentException("Plugin type:" + pluginType.name() + " is not in actionMap");
                }
                break;
            default:
                throw new RuntimeException("Unsupported plugin type:" + pluginType.name());
        }
        switch (pluginType) {
            case Riru:
            case Zygisk:
            case Xposed:
                try {
                    return actionMap.get(pluginType).call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            default:
                throw new RuntimeException("Unsupported plugin type:" + pluginType.name());
        }
    }

    public static void iterateAllPluginTarget(Consumer<PluginTarget> consumer) {
        for (PluginTarget pluginTarget: PluginTarget.values()) {
            consumer.accept(pluginTarget);
        }
    }
}
