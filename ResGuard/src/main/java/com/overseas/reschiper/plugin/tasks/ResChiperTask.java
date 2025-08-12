package com.overseas.reschiper.plugin.tasks;

import com.android.build.gradle.api.ApplicationVariant;
import com.overseas.reschiper.plugin.command.Command;
import com.overseas.reschiper.plugin.command.model.DuplicateResMergerCommand;
import com.overseas.reschiper.plugin.command.model.FileFilterCommand;
import com.overseas.reschiper.plugin.command.model.ObfuscateBundleCommand;
import com.overseas.reschiper.plugin.command.model.StringFilterCommand;
import com.overseas.reschiper.plugin.Extension;
import com.overseas.reschiper.plugin.model.KeyStore;
import com.overseas.reschiper.plugin.internal.Bundle;
import com.overseas.reschiper.plugin.internal.SigningConfig;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom Gradle task for running ResChiper.
 */
public class ResChiperTask extends DefaultTask {

    private static final Logger logger = Logger.getLogger(ResChiperTask.class.getName());
    private final Extension resChiperExtension = (Extension) getProject().getExtensions().getByName("resChiper");
    private ApplicationVariant variant;
    private KeyStore keyStore;
    private Path bundlePath;
    private Path obfuscatedBundlePath;

    /**
     * Constructor for the ResChiperTask.
     */
    public ResChiperTask() {
        setDescription("Assemble resource proguard for bundle file");
        setGroup("bundle");
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Sets the variant scope for the task.
     *
     * @param variant The ApplicationVariant for the Android application.
     */
    public void setVariantScope(ApplicationVariant variant) {
        this.variant = variant;
        bundlePath = Bundle.getBundleFilePath(getProject(), variant);
        obfuscatedBundlePath = new File(bundlePath.toFile().getParentFile(), resChiperExtension.getObfuscatedBundleName()).toPath();
    }

    /**
     * Executes the ResChiperTask.
     *
     * @throws Exception If an error occurs during execution.
     */
    @TaskAction
    private void execute() throws Exception {
        logger.log(Level.INFO, resChiperExtension.toString());
        keyStore = SigningConfig.getSigningConfig(variant);
        printSignConfiguration();
        printOutputFileLocation();
        prepareUnusedFile();
        Command.Builder builder = Command.builder();
        builder.setBundlePath(bundlePath);
        builder.setOutputPath(obfuscatedBundlePath);

        ObfuscateBundleCommand.Builder obfuscateBuilder = ObfuscateBundleCommand.builder()
                .setEnableObfuscate(resChiperExtension.getEnableObfuscation())
                .setObfuscationMode(resChiperExtension.getObfuscationMode())
                .setMergeDuplicatedResources(resChiperExtension.getMergeDuplicateResources())
                .setWhiteList(resChiperExtension.getWhiteList())
                .setFilterFile(resChiperExtension.getEnableFileFiltering())
                .setFileFilterRules(resChiperExtension.getFileFilterList())
                .setRemoveStr(resChiperExtension.getEnableFilterStrings())
                .setUnusedStrPath(resChiperExtension.getUnusedStringFile())
                .setLanguageWhiteList(resChiperExtension.getLocaleWhiteList());
        if (resChiperExtension.getMappingFile() != null)
            obfuscateBuilder.setMappingPath(resChiperExtension.getMappingFile());

        if (keyStore.storeFile != null && keyStore.storeFile.exists())
            builder.setStoreFile(keyStore.storeFile.toPath())
                    .setKeyAlias(keyStore.keyAlias)
                    .setKeyPassword(keyStore.keyPassword)
                    .setStorePassword(keyStore.storePassword);

        builder.setObfuscateBundleBuilder(obfuscateBuilder.build());

        FileFilterCommand.Builder fileFilterBuilder = FileFilterCommand.builder();
        fileFilterBuilder.setFileFilterRules(resChiperExtension.getFileFilterList());
        builder.setFileFilterBuilder(fileFilterBuilder.build());

        StringFilterCommand.Builder stringFilterBuilder = StringFilterCommand.builder();
        builder.setStringFilterBuilder(stringFilterBuilder.build());

        DuplicateResMergerCommand.Builder duplicateResMergeBuilder = DuplicateResMergerCommand.builder();
        builder.setDuplicateResMergeBuilder(duplicateResMergeBuilder.build());

        Command command = builder.build(builder.build(), Command.TYPE.OBFUSCATE_BUNDLE);
        command.execute(Command.TYPE.OBFUSCATE_BUNDLE);

        aabToApk(keyStore);
    }

    /**
     * Prepares the unused file for filtering.
     */
    private void prepareUnusedFile() {
        String simpleName = variant.getName().replace("Release", "");
        String name = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        String resourcePath = getProject().getBuildDir() + "/outputs/mapping/" + name + "/release/unused_strings.txt";
        File usedFile = new File(resourcePath);

        if (usedFile.exists()) {
            System.out.println("find unused_strings.txt: " + usedFile.getAbsolutePath());
            if (resChiperExtension.getEnableFilterStrings())
                if (resChiperExtension.getUnusedStringFile() == null || resChiperExtension.getUnusedStringFile().isBlank()) {
                    resChiperExtension.setUnusedStringFile(usedFile.getAbsolutePath());
                    logger.log(Level.SEVERE, "replace unused_strings.txt!");
                }
        } else
            logger.log(Level.SEVERE, "not exists unused_strings.txt: " + usedFile.getAbsolutePath()
                    + "\nuse default path: " + resChiperExtension.getUnusedStringFile());
    }

    /**
     * Prints the signing configuration.
     */
    private void printSignConfiguration() {
        System.out.println("----------------------------------------");
        System.out.println(" Signing Configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tKeyStoreFile:\t\t" + keyStore.storeFile);
        System.out.println("\tKeyPassword:\t" + encrypt(keyStore.keyPassword));
        System.out.println("\tAlias:\t\t\t" + encrypt(keyStore.keyAlias));
        System.out.println("\tStorePassword:\t" + encrypt(keyStore.storePassword));
    }

    /**
     * Prints the output file location.
     */
    private void printOutputFileLocation() {
        System.out.println("----------------------------------------");
        System.out.println(" Output configuration");
        System.out.println("----------------------------------------");
        System.out.println("\tFolder:\t\t" + obfuscatedBundlePath.getParent());
        System.out.println("\tFile:\t\t" + obfuscatedBundlePath.getFileName());
        System.out.println("----------------------------------------");
    }

    /**
     * Encrypts a value for printing (partially).
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    private @NotNull String encrypt(String value) {
        if (value == null)
            return "/";
        if (value.length() > 2)
            return value.substring(0, value.length() / 2) + "****";
        return "****";
    }

    private void aabToApk(KeyStore keyStore) {
        try {
            if (keyStore.storeFile != null && keyStore.storeFile.exists()) {
                // 获取 bundletool-all-1.15.6.jar 的路径
                File jarFile = new File(getProject().getBuildDir(), "bundletool-all-1.15.6.jar");
                try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("bundletool-all-1.15.6.jar");
                     FileOutputStream outputStream = new FileOutputStream(jarFile)) {
                    if (resourceStream == null) {
                        throw new IllegalStateException("bundletool-all-1.15.6.jar not found in resources");
                    }
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = resourceStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                if (!jarFile.exists()) {
                    throw new IllegalStateException("bundletool JAR file not found: " + jarFile.getAbsolutePath());
                }

                File bundleDir = obfuscatedBundlePath.toFile().getParentFile();
                File apks = new File(bundleDir, "app.apks");
                if (apks.exists()) {
                    apks.delete();
                }

                String outputPath = apks.getAbsolutePath();
                // 构造命令
                List<String> command = new ArrayList<>();
                command.add("java");
                command.add("-jar");
                command.add(jarFile.getAbsolutePath());
                command.add("build-apks");
                command.add("--bundle=" + obfuscatedBundlePath);
                command.add("--output=" + outputPath);
                command.add("--mode=universal");
                command.add("--ks=" + keyStore.storeFile.getAbsolutePath());
                command.add("--ks-pass=pass:" + keyStore.storePassword);
                command.add("--ks-key-alias=" + keyStore.keyAlias);
                command.add("--key-pass=pass:" + keyStore.keyPassword);

                // 执行命令
                System.out.println("Executing command: " + String.join(" ", command));
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(getProject().getProjectDir()); // 设置工作目录
                processBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
                Process process = processBuilder.start();

                // 捕获输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }

                // 检查退出状态
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Command failed with exit code " + exitCode);
                }

                // 提取 APK 文件
                File apksFile = new File(bundleDir, "app.apks");
                File outApk = new File(bundleDir, "extracted-apks");

                // 清理输出目录
                if (outApk.exists()) {
                    deleteDirectory(outApk);
                }
                Files.createDirectories(outApk.toPath());

                // 根据操作系统选择解压方式
                String osName = System.getProperty("os.name").toLowerCase();
                List<String> extractCommand = new ArrayList<>();

                if (osName.contains("win")) {
                    // Windows 使用 PowerShell
                    extractCommand.add("powershell");
                    extractCommand.add("-Command");
                    extractCommand.add(String.format(
                            "Expand-Archive -Path '%s' -DestinationPath '%s' -Force",
                            apksFile.getAbsolutePath().replace("'", "''"),
                            outApk.getAbsolutePath().replace("'", "''")
                    ));
                } else {
                    // Linux/macOS 使用 unzip
                    extractCommand.add("unzip");
                    extractCommand.add("-o");
                    extractCommand.add(apksFile.getAbsolutePath());
                    extractCommand.add("-d");
                    extractCommand.add(outApk.getAbsolutePath());
                }

                // 执行提取命令
                System.out.println("Executing extract-apks command: " + String.join(" ", extractCommand));
                ProcessBuilder extractProcessBuilder = new ProcessBuilder(extractCommand);
                extractProcessBuilder.directory(getProject().getProjectDir());
                extractProcessBuilder.redirectErrorStream(true);
                Process extractProcess = extractProcessBuilder.start();

                // 捕获输出
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(extractProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }

                // 检查提取过程的退出状态
                int extractExitCode = extractProcess.waitFor();
                if (extractExitCode != 0) {
                    throw new RuntimeException("Extract command failed with exit code " + extractExitCode);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute generateApks task", e);
        }
    }

    // 递归删除目录的辅助方法
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete: " + directory);
        }
    }
}
