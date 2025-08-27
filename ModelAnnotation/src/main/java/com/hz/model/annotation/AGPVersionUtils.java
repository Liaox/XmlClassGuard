package com.hz.model.annotation;

import org.gradle.api.Project;

public class AGPVersionUtils {

    /**
     * 获取 AGP 版本
     */
    private static String getAgpVersion(Project project) {
        try {
            // 尝试使用AGP 7.0+的Version类
            Class<?> versionClass = Class.forName("com.android.Version");
            java.lang.reflect.Field field = versionClass.getField("ANDROID_GRADLE_PLUGIN_VERSION");
            String agpVersion = (String) field.get(null);
            System.out.println("AGP version: " + agpVersion);
            return agpVersion;
        } catch (ClassNotFoundException e) {
            // 回退到旧版AGP的Version类
            try {
                Class<?> versionClass = Class.forName("com.android.builder.Version");
                java.lang.reflect.Field field = versionClass.getField("ANDROID_GRADLE_PLUGIN_VERSION");
                String agpVersion = (String) field.get(null);
                System.out.println("AGP version (legacy): " + agpVersion);
                return agpVersion;
            } catch (Exception ex) {
                project.getLogger().warn("Could not determine AGP version: " + ex.getMessage());
            }
        } catch (Exception e) {
            project.getLogger().warn("Could not determine AGP version: " + e.getMessage());
        }

        return null;
    }

    /**
     * 判断 AGP 是否 >= 8.3
     */
    public static boolean isAGP83OrAbove(Project project) {
        String version = getAgpVersion(project);
        System.err.println("AGP版本："+version);
        if (version == null || "unknown".equals(version)) {
            return false;
        }

        String[] parts = version.split("\\.");
        int major = parts.length > 0 ? parseInt(parts[0]) : 0;
        int minor = parts.length > 1 ? parseInt(parts[1]) : 0;

        return major > 8 || (major == 8 && minor >= 3);
    }

    private static int parseInt(String str) {
        try {
            return Integer.parseInt(str.replaceAll("\\D.*", "")); // 去掉可能的 "-beta01"
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
