package com.hz.model.annotation;
import com.android.build.gradle.AppExtension;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public abstract class AddAnnotationTask extends DefaultTask {
    private String buildType;
    private String flavor;
    private List<String> subPackagePaths;
    private String channel;
    @Inject
    public AddAnnotationTask(String variantName, AnnotationConfig extension){
        this.buildType = variantName.toLowerCase();
        this.subPackagePaths = extension.getSubPackagePaths();
        this.channel = extension.getChannel();
        //暂不支持flavor配置。
        this.flavor = "";
    }
//    @Input
//    public String getChannel() {
//        return channel;
//    }
//
//    public void setChannel(String channel) {
//        this.channel = channel;
//    }
//    @Input
//    public String getBuildType() {
//        return buildType;
//    }
//
//    public void setBuildType(String buildType) {
//        this.buildType = buildType;
//    }
//
//    @Input
//    public String getFlavor() {
//        return flavor;
//    }
//
//    public void setFlavor(String flavor) {
//        this.flavor = flavor;
//    }
//    @Input
//    public List<String> getSubPackagePaths() {
//        return subPackagePaths;
//    }
//
//    public void setSubPackagePaths(List<String> subPackagePaths) {
//        this.subPackagePaths = subPackagePaths;
//    }
    @TaskAction
    public void addAnnotations() {
        // 构建 classes 根目录
        File classRootDir = new File(getProject().getBuildDir().getAbsolutePath() +
                "/intermediates/javac/" + (flavor.isEmpty() ? "" : (flavor + "/")) + buildType + "/classes");

        String annotationDesc = "Lcom/google/gson/annotations/SerializedName;"; // 更新为 Gson 的 SerializedName 描述符
        if (classRootDir.exists()) {
            for (String subPackage : subPackagePaths) {
                System.out.println("processing package models:"+subPackage);
                File subPackageDir = new File(classRootDir, subPackage.replace('.', '/'));
                if (subPackageDir.exists()) {
                    try {
                        processClassesInDirectory(subPackageDir, annotationDesc);
                        getLogger().lifecycle("Annotation injection completed for package: " + subPackage);
                    } catch (IOException e) {
                        getLogger().error("Failed to process classes in package: " + subPackage, e);
                    }
                } else {
                    getLogger().warn("Package directory does not exist: " + subPackageDir.getAbsolutePath());
                }
            }
        } else {
            getLogger().warn("Class root directory does not exist: " + classRootDir.getAbsolutePath());
        }

        File kotlinClassDir = new File(getProject().getBuildDir().getAbsolutePath() +
                "/tmp/kotlin-classes/" + buildType);
        if (kotlinClassDir.exists()) {
            for (String subPackage : subPackagePaths) {
                System.out.println("processing package models:"+subPackage);
                File subPackageDir = new File(kotlinClassDir, subPackage.replace('.', '/'));
                if (subPackageDir.exists()) {
                    try {
                        processClassesInDirectory(subPackageDir, annotationDesc);
                        getLogger().lifecycle("Annotation injection completed for package: " + subPackage);
                    } catch (IOException e) {
                        getLogger().error("Failed to process classes in package: " + subPackage, e);
                    }
                } else {
                    getLogger().warn("Package directory does not exist: " + subPackageDir.getAbsolutePath());
                }
            }
        } else {
            getLogger().warn("Class root directory does not exist: " + kotlinClassDir.getAbsolutePath());
        }
    }

    private void processClassesInDirectory(File dir, String annotationDesc) throws IOException {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                processClassesInDirectory(file, annotationDesc);
            } else if (file.getName().endsWith(".class")) {
                injectAnnotationToClass(file, annotationDesc);
            }
        }
    }

    private void injectAnnotationToClass(File classFile, String annotationDesc) throws IOException {
        FileInputStream fis = new FileInputStream(classFile);
        ClassReader classReader = new ClassReader(fis);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        AddAnnotationClassVisitor classVisitor = new AddAnnotationClassVisitor(classWriter, annotationDesc,channel);
        classReader.accept(classVisitor, 0);

        byte[] modifiedClassBytes = classWriter.toByteArray();
        FileOutputStream fos = new FileOutputStream(classFile);
        fos.write(modifiedClassBytes);
        fos.close();
        fis.close();
        getLogger().lifecycle("Modified class file: " + classFile.getAbsolutePath());
    }
}