package com.hz.model.annotation;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.LibraryVariant;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.util.internal.TextUtil;

public class ModelAnnotationPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // 注册扩展，允许用户配置多个包名
        AnnotationConfig extension = project.getExtensions().create("annotationConfig", AnnotationConfig.class, project);
        if (project.getPlugins().hasPlugin(AppPlugin.class)){
            AppExtension android = (AppExtension) project.getExtensions().getByName("android");
            project.afterEvaluate(project1 -> {
                android.getApplicationVariants().all(variant -> {
                    createAnnotationTasks(project1, variant,extension);
                });
            });
        }else if (project.getPlugins().hasPlugin(LibraryPlugin.class)){
            LibraryExtension lib = project.getExtensions().getByType(LibraryExtension.class);
            project.afterEvaluate(project1 -> {
                lib.getLibraryVariants().all(variant -> {
                    createAnnotationTasks(project1, variant,extension);
                });
            });
        }





//        // 注册 AddAnnotationTask
//        AddAnnotationTask addAnnotationTask = project.getTasks().create("addAnnotationTask", AddAnnotationTask.class);
//        // 监听编译任务
//        project.getTasks().whenTaskAdded(task -> {
//            if (task.getName().equals("compileDebugJavaWithJavac") || task.getName().equals("compileReleaseJavaWithJavac")) {
//                task.doFirst(t -> {
//                    addAnnotationTask.setSubPackagePaths(extension.getSubPackagePaths());
//                    addAnnotationTask.setChannel(extension.getChannel());
//                    // 从任务名称中获取 buildType 和 flavor
//                    String buildType = getBuildTypeFromTaskName(task.getName());
//                    String flavor = getFlavorFromTaskName(task.getName());
//                    // 将编译变体信息传递给 AddAnnotationTask
//                    addAnnotationTask.setBuildType(buildType);
//                    addAnnotationTask.setFlavor(flavor);
//                });
//
//                task.finalizedBy(addAnnotationTask); // 编译任务完成后执行 addAnnotationTask
//            }
//        });
    }
    private void createAnnotationTasks(Project project, ApplicationVariant variant,AnnotationConfig extension){
        String variantName = TextUtil.capitalize(variant.getName());
        String taskName = "addAnnotationTask"+variantName;
        Task task =  project.getTasks().findByName(taskName);
        if (task==null){
            task = project.getTasks().create(taskName,AddAnnotationTask.class,variantName,extension);
        }
        String compileJavacTask = "compile"+variantName+"JavaWithJavac";
        Task comileTask = project.getTasks().findByName(compileJavacTask);
        if (comileTask!=null){
            comileTask.finalizedBy(task);
        }
    }

    private void createAnnotationTasks(Project project, LibraryVariant variant, AnnotationConfig extension){
        String variantName = TextUtil.capitalize(variant.getName());
        String taskName = "addAnnotationTask"+variantName;
        Task task =  project.getTasks().findByName(taskName);
        if (task==null){
            task = project.getTasks().create(taskName,AddAnnotationTask.class,variantName,extension);
        }
        String compileJavacTask = "compile"+variantName+"JavaWithJavac";
        Task comileTask = project.getTasks().findByName(compileJavacTask);
        if (comileTask!=null){
            comileTask.finalizedBy(task);
        }
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