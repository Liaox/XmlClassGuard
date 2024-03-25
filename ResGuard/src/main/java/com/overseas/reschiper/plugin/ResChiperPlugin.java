package com.overseas.reschiper.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.builder.model.SourceProvider;
import com.android.tools.r8.internal.F;
import com.overseas.reschiper.plugin.internal.AGP;
import com.overseas.reschiper.plugin.tasks.RCFindConstraintReferencedIdsTask;
import com.overseas.reschiper.plugin.tasks.ResChiperTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Plugin for integrating ResChiper into an Android Gradle project.
 */
public class ResChiperPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        checkApplicationPlugin(project);
        AppExtension android = (AppExtension) project.getExtensions().getByName("android");
        project.getExtensions().create("resChiper", Extension.class);
        project.afterEvaluate(project1 -> android.getApplicationVariants().all(variant -> {
            variant.getSourceSets().forEach(sourceSet -> {
                sourceSet.getResDirectories().forEach(dirs->{
                    if (dirs.exists() && dirs.isDirectory()){
                        File[] childDirs = dirs.listFiles();
//                        if (childDirs!=null){
//                            for (File file : childDirs) {
//                                if (file!=null && file.exists()){
//                                    if (file.getName().startsWith("drawable") || file.getName().startsWith("mipmap")){
////                                        System.out.println("-------------image---dirs--------------->:"+file);
//                                        if (file.isDirectory()){
//                                            File[] files = file.listFiles();
//                                            if (files!=null){
//                                                for (File file1 : files) {
//                                                    if (file1!=null && file1.exists() && file1.isFile() && (file1.getName().endsWith(".jpg") || file1.getName().endsWith(".png"))){
//                                                        System.out.println("-------------image---file--------------->:"+file1);
//                                                        File newFile = new File(file1.getParent(),"temp_"+file1.getName());
//                                                        try {
//                                                            boolean create = newFile.createNewFile();
//                                                            System.out.println("-------------create new file--------------->:"+create);
//                                                        } catch (IOException e) {
//                                                            throw new RuntimeException(e);
//                                                        }
//                                                        try {
//                                                            FileInputStream inputStream = new FileInputStream(file1);
//                                                            FileOutputStream outputStream = new FileOutputStream(newFile);
//                                                            int c;
//                                                            while ((c=inputStream.read())!=-1){
//                                                                outputStream.write(c);
//                                                            }
//                                                            outputStream.write(0);
//                                                            inputStream.close();
//                                                            outputStream.close();
//                                                            boolean del = file1.delete();
//                                                            System.out.println("-------------del old file--------------->:"+del);
//                                                            boolean r = newFile.renameTo(file1);
//                                                            System.out.println("-------------rename new file--------------->:"+r);
//                                                        } catch (FileNotFoundException e) {
//                                                            throw new RuntimeException(e);
//                                                        } catch (IOException e) {
//                                                            throw new RuntimeException(e);
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
                    }
                });
            });
            createResChiperTask(project1, variant);
            String variantName = variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
            createResChiperFindConstraintReferencedIdsTask(project1,variantName);
        }));
    }

    /**
     * Creates a ResChiper task for the given variant.
     *
     * @param project The Gradle project.
     * @param variant The Android application variant.
     */
    private void createResChiperTask(@NotNull Project project, @NotNull ApplicationVariant variant) {
        String variantName = variant.getName().substring(0, 1).toUpperCase() + variant.getName().substring(1);
        String bundleTaskName = "bundle" + variantName;
        if (project.getTasks().findByName(bundleTaskName) == null)
            return;
        String taskName = "resChiper" + variantName;
        ResChiperTask resChiperTask;
        if (project.getTasks().findByName(taskName) == null)
            resChiperTask = project.getTasks().create(taskName, ResChiperTask.class);
        else
            resChiperTask = (ResChiperTask) project.getTasks().getByName(taskName);

        resChiperTask.setVariantScope(variant);
        resChiperTask.doFirst(task -> {
            printResChiperBuildConfiguration();
            printProjectBuildConfiguration(project);
        });

        Task bundleTask = project.getTasks().getByName(bundleTaskName);
        Task bundlePackageTask = project.getTasks().getByName("package" + variantName + "Bundle");
        bundleTask.dependsOn(resChiperTask);
        resChiperTask.dependsOn(bundlePackageTask);

        String finalizeBundleTaskName = "sign" + variantName + "Bundle";
        if (project.getTasks().findByName(finalizeBundleTaskName) != null)
            resChiperTask.dependsOn(project.getTasks().getByName(finalizeBundleTaskName));
    }
    private void createResChiperFindConstraintReferencedIdsTask(@NotNull Project project, @NotNull String variantName){
        String resChiperTaskName = "resChiper"+variantName;
        Task resChiperTask = project.getTasks().findByName(resChiperTaskName);
        if (resChiperTask==null) throw new GradleException("ResChiper plugin required");
        String taskName = "resChiperFindConstraintReferencedIds"+variantName;
        Task task ;
        if (project.getTasks().findByName(taskName) == null)
            task = project.getTasks().create(taskName, RCFindConstraintReferencedIdsTask.class,"resChiper",variantName);
        else
            task = (ResChiperTask) project.getTasks().getByName(taskName);
        resChiperTask.dependsOn(task);
    }
    /**
     * Checks if the Android Application plugin is applied to the project.
     *
     * @param project The Gradle project.
     */
    private void checkApplicationPlugin(@NotNull Project project) {
        if (!project.getPlugins().hasPlugin("com.android.application"))
            throw new GradleException("Android Application plugin 'com.android.application' is required");
    }

    /**
     * Prints the ResChiper build configuration information.
     */
    private void printResChiperBuildConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" ResChiper Plugin Configuration:");
        System.out.println("----------------------------------------");
        System.out.println("- ResChiper version:\t" + ResChiper.VERSION);
        System.out.println("- BundleTool version:\t" + ResChiper.BT_VERSION);
        System.out.println("- AGP version:\t\t" + ResChiper.AGP_VERSION);
        System.out.println("- Gradle Wrapper:\t" + ResChiper.GRADLE_WRAPPER_VERSION);
    }

    /**
     * Prints the project's build information.
     *
     * @param project The Android Gradle project.
     */
    private void printProjectBuildConfiguration(@NotNull Project project) {
        System.out.println("----------------------------------------");
        System.out.println(" App Build Information:");
        System.out.println("----------------------------------------");
        System.out.println("- Project name:\t\t\t" + project.getRootProject().getName());
        System.out.println("- AGP version:\t\t\t" + AGP.getAGPVersion(project));
        System.out.println("- Running Gradle version:\t" + project.getGradle().getGradleVersion());
    }
}
