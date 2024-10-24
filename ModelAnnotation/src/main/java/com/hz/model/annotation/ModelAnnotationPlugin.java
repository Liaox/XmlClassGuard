package com.hz.model.annotation;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ModelAnnotationPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // 注册扩展，允许用户配置多个包名
        AnnotationConfig extension = project.getExtensions().create("annotationConfig", AnnotationConfig.class, project);

        // 注册 AddAnnotationTask
        AddAnnotationTask addAnnotationTask = project.getTasks().create("addAnnotationTask", AddAnnotationTask.class);
        // 监听编译任务
        project.getTasks().whenTaskAdded(task -> {
            if (task.getName().startsWith("compile") && task.getName().endsWith("JavaWithJavac")) {
                task.doFirst(t -> {
                    addAnnotationTask.setSubPackagePaths(extension.getSubPackagePaths());
                    addAnnotationTask.setChannel(extension.getChannel());
                    // 从任务名称中获取 buildType 和 flavor
                    String buildType = getBuildTypeFromTaskName(task.getName());
                    String flavor = getFlavorFromTaskName(task.getName());
                    // 将编译变体信息传递给 AddAnnotationTask
                    addAnnotationTask.setBuildType(buildType);
                    addAnnotationTask.setFlavor(flavor);
                });

                task.finalizedBy(addAnnotationTask); // 编译任务完成后执行 addAnnotationTask
            }
        });
    }

    private String getBuildTypeFromTaskName(String taskName) {
        if (taskName.contains("Debug")) {
            return "debug";
        } else if (taskName.contains("Release")) {
            return "release";
        }
        // 其他自定义 buildType 逻辑可以在这里添加
        return "debug"; // 默认返回 debug
    }

    private String getFlavorFromTaskName(String taskName) {
        // 假设 flavor 在 buildType 之前
        if (taskName.contains("Debug") || taskName.contains("Release")) {
            int flavorStartIndex = "compile".length();
            int flavorEndIndex = taskName.indexOf("DebugJavaWithJavac");
            if (flavorEndIndex == -1) {
                flavorEndIndex = taskName.indexOf("ReleaseJavaWithJavac");
            }
            return taskName.substring(flavorStartIndex, flavorEndIndex).toLowerCase();
        }
        return ""; // 如果没有 flavor，返回空字符串
    }
}