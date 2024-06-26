package com.overseas.reschiper.plugin.command;

import com.android.tools.build.bundletool.flags.Flag;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.exceptions.CommandExecutionException;
import com.google.auto.value.AutoValue;
import com.overseas.reschiper.plugin.android.JarSigner;
import com.overseas.reschiper.plugin.bundle.AppBundleAnalyzer;
import com.overseas.reschiper.plugin.bundle.AppBundlePackager;
import com.overseas.reschiper.plugin.bundle.AppBundleSigner;
import com.overseas.reschiper.plugin.command.extensions.BundleFileFilter;
import com.overseas.reschiper.plugin.command.extensions.BundleStringFilter;
import com.overseas.reschiper.plugin.command.extensions.DuplicateResourceMerger;
import com.overseas.reschiper.plugin.command.model.StringFilterCommand;

import com.overseas.reschiper.plugin.command.model.DuplicateResMergerCommand;
import com.overseas.reschiper.plugin.command.model.FileFilterCommand;
import com.overseas.reschiper.plugin.command.model.ObfuscateBundleCommand;

import com.overseas.reschiper.plugin.obfuscation.ResourcesObfuscator;
import com.overseas.reschiper.plugin.operations.FileOperation;
import com.overseas.reschiper.plugin.parser.Parser;
import com.overseas.reschiper.plugin.parser.xml.StringFilterConfig;
import com.overseas.reschiper.plugin.utils.TimeClock;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileDoesNotExist;
import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

/**
 * Represents a command-line tool for various operations on Android App Bundles.
 *
 * <p>This class provides functionalities for filtering files, filtering strings, merging duplicated
 * resources, and obfuscating resource files within an Android App Bundle (.aab) file.
 * The tool supports various command types, each with its specific set of options and behavior.
 *
 * @see TYPE
 */
@AutoValue
public abstract class Command {

    /**
     * A flag representing the location of the Android App Bundle to be processed.
     */
    public static final Flag<Path> BUNDLE_LOCATION_FLAG = Flag.path("bundle");

    /**
     * A flag representing the location where the output bundle file should be created.
     */
    public static final Flag<Path> OUTPUT_FILE_FLAG = Flag.path("output");

    /**
     * A flag representing the location of the configuration file.
     */
    public static final Flag<Path> CONFIG_LOCATION_FLAG = Flag.path("config");

    /**
     * A flag representing the password for the keystore (used for signing the bundle).
     */
    public static final Flag<String> STORE_PASSWORD_FLAG = Flag.string("storePassword");

    /**
     * A flag representing the alias name of the key (used for signing the bundle).
     */
    public static final Flag<String> KEY_ALIAS_FLAG = Flag.string("keyAlias");

    /**
     * A flag representing the password for the key (used for signing the bundle).
     */
    public static final Flag<String> KEY_PASSWORD_FLAG = Flag.string("keyPassword");

    /**
     * A flag representing the location of the resource mapping file (optional).
     */
    static final Flag<Path> MAPPING_FLAG = Flag.path("mapping");

    /**
     * A logger for logging messages related to the Command class.
     */
    private static final Logger logger = Logger.getLogger(Command.class.getName());

    /**
     * Enumeration of supported command types.
     */
    public enum TYPE {
        FILTER_FILE,
        FILTER_STRING,
        DUPLICATE_RES_MERGE,
        OBFUSCATE_BUNDLE,
    }

    /**
     * Creates a new builder for constructing a Command instance.
     *
     * @return A new instance of the Command.Builder.
     */
    @Contract(" -> new")
    public static @NotNull Command.Builder builder() {
        return new AutoValue_Command.Builder();
    }

    /**
     * Executes the specified command, performing the associated operations on an Android App Bundle.
     *
     * @param commandType The type of command to execute.
     * @return The path to the resulting Android App Bundle file after executing the command.
     * @throws Exception If an error occurs during command execution.
     */
    public Path execute(TYPE commandType) throws Exception {
        TimeClock timeClock = new TimeClock();
        AppBundle appBundle = new AppBundleAnalyzer(getBundlePath()).analyze();
        String out = "";
        if (commandType == TYPE.OBFUSCATE_BUNDLE) {
            ObfuscateBundleCommand bundleCommand = getObfuscateBundleBuilder();
            // filter file
            if (bundleCommand.getFilterFile().isPresent() && bundleCommand.getFilterFile().get()) {
                Set<String> fileFilterRules = new HashSet<>();
                if (bundleCommand.getFileFilterRules().isPresent())
                    fileFilterRules = bundleCommand.getFileFilterRules().get();
                BundleFileFilter filter = new BundleFileFilter(getBundlePath(), appBundle, fileFilterRules);
                appBundle = filter.filter();
            }

            // remove unused strings need to execute before obfuscate
            if (bundleCommand.getRemoveStr().isPresent() && bundleCommand.getRemoveStr().get()) {
                File unusedFile = new File("");
                if (bundleCommand.getUnusedStrPath().isPresent()) {
                    File file = new File(bundleCommand.getUnusedStrPath().get());
                    if (file.exists())
                        unusedFile = file;
                    else
                        System.out.println("unused_strings.txt file is not exists!");
                }
                Set<String> languageWhiteList = new HashSet<>();
                if (bundleCommand.getLanguageWhiteList().isPresent())
                    languageWhiteList = bundleCommand.getLanguageWhiteList().get();
                BundleStringFilter filter = new BundleStringFilter(getBundlePath(), appBundle, unusedFile.getPath(), languageWhiteList);
                appBundle = filter.filter();
            }

            // merge duplicated resources
            if (bundleCommand.getMergeDuplicatedResources().isPresent() && bundleCommand.getMergeDuplicatedResources().get()) {
                DuplicateResourceMerger merger = new DuplicateResourceMerger(getBundlePath(), appBundle, getOutputPath().getParent());
                appBundle = merger.merge();
            }

            // obfuscate bundle
            if (bundleCommand.getEnableObfuscate()) {
                Path mappingPath = null;
                if (bundleCommand.getMappingPath().isPresent())
                    mappingPath = bundleCommand.getMappingPath().get();
                ResourcesObfuscator obfuscator = new ResourcesObfuscator(getBundlePath(), appBundle, bundleCommand.getWhiteList(), getOutputPath().getParent(), mappingPath);
                obfuscator.withMode(obfuscator.getMode(bundleCommand.getObfuscationMode() == null ? "default" : bundleCommand.getObfuscationMode()));
                appBundle = obfuscator.obfuscate();
            }

            // package bundle
            new AppBundlePackager(appBundle, getOutputPath()).execute();
            // sign bundle
            if (bundleCommand.getDisableSign().isEmpty() || !bundleCommand.getDisableSign().get()) {
                AppBundleSigner signer = new AppBundleSigner(getOutputPath());
                getStoreFile().ifPresent(storeFile -> {
                            if (getStorePassword().isPresent() && getKeyAlias().isPresent() && getKeyPassword().isPresent())
                                signer.setBundleSignature(new JarSigner.Signature(storeFile, getStorePassword().get(), getKeyAlias().get(), getKeyPassword().get()));
                        }
                );
                signer.execute();
            }

            out = "----------------------------------------"+
                    " Bundle Summary:"+
                    "----------------------------------------"+
                    "- Bundle packing completed in %s"+
                    "- Reduced bundle file size: %s, (Original: %s, New: %s)%n";

        } else if (commandType == TYPE.DUPLICATE_RES_MERGE) {
            DuplicateResMergerCommand resMergeCommand = getDuplicateResMergeBuilder();
            // merge duplicated resources file
            DuplicateResourceMerger merger = new DuplicateResourceMerger(getBundlePath(), appBundle, getOutputPath().getParent());
            appBundle = merger.merge();
            // package bundle
            new AppBundlePackager(appBundle, getOutputPath()).execute();
            // sign bundle
            if (resMergeCommand.getDisableSign().isPresent() || !resMergeCommand.getDisableSign().get()) {
                AppBundleSigner signer = new AppBundleSigner(getOutputPath());
                getStoreFile().ifPresent(storeFile -> {
                    if (getStorePassword().isPresent() && getKeyAlias().isPresent() && getKeyPassword().isPresent())
                        signer.setBundleSignature(new JarSigner.Signature(storeFile, getStorePassword().get(), getKeyAlias().get(), getKeyPassword().get()));
                });
                signer.execute();
            }

            out = "duplicate resources done, took %s"+
                    "-----------------------------------------"+
                    "\tReduced bundle file size: %s, %s -> %s"+
                    "-----------------------------------------%n";
        } else if (commandType == TYPE.FILTER_FILE && getFileFilterBuilder().isPresent()) {
            FileFilterCommand fileFilterCommand = getFileFilterBuilder().get();
            // filter bundle files
            BundleFileFilter filter = new BundleFileFilter(getBundlePath(), appBundle, fileFilterCommand.getFileFilterRules());
            AppBundle filteredAppBundle = filter.filter();
            // package bundle
            new AppBundlePackager(filteredAppBundle, getOutputPath()).execute();
            // sign bundle
            if (fileFilterCommand.getDisableSign().isPresent() || !fileFilterCommand.getDisableSign().get()) {
                AppBundleSigner signer = new AppBundleSigner(getOutputPath());
                getStoreFile().ifPresent(storeFile -> {
                    if (getStorePassword().isPresent() && getKeyAlias().isPresent() && getKeyPassword().isPresent())
                        signer.setBundleSignature(new JarSigner.Signature(storeFile, getStorePassword().get(), getKeyAlias().get(), getKeyPassword().get()));
                });
                signer.execute();
            }

            out = "filter bundle files done, took %s"+
                    "-----------------------------------------"+
                    "\tReduced bundle file size: %s, %s -> %s"+
                    "-----------------------------------------%n";

        } else if (commandType == TYPE.FILTER_STRING) {
            StringFilterCommand bundleCommand = getStringFilterBuilder();
            // parse config.xml
            StringFilterConfig config = new Parser.XML(bundleCommand.getConfigPath().get()).stringFilterParse();
            if (!config.isActive())
                throw CommandExecutionException.builder()
                        .withInternalMessage("parser attribute filter#isactive can not be 'false' in %s command", commandType.name())
                        .build();
            // filter bundle strings
            BundleStringFilter filter = new BundleStringFilter(getBundlePath(), appBundle, config.getPath(), config.getLanguageWhiteList());
            AppBundle filteredAppBundle = filter.filter();
            // package bundle
            new AppBundlePackager(filteredAppBundle, getOutputPath()).execute();
            // sign bundle
            AppBundleSigner signer = new AppBundleSigner(getOutputPath());
            getStoreFile().ifPresent(storeFile -> {
                if (getStorePassword().isPresent() && getKeyAlias().isPresent() && getKeyPassword().isPresent())
                    signer.setBundleSignature(new JarSigner.Signature(storeFile, getStorePassword().get(), getKeyAlias().get(), getKeyPassword().get()));

            });
            signer.execute();

            out = "filter bundle strings done, took %s"+
                    "-----------------------------------------"+
                    "\tReduced bundle string size: %s, %s -> %s"+
                    "-----------------------------------------%n";
        }

        long rawSize = FileOperation.getFileSizes(getBundlePath().toFile());
        long filteredSize = FileOperation.getFileSizes(getOutputPath().toFile());

        System.out.printf(out, timeClock.getElapsedTime(),
                FileOperation.getNetFileSizeDescription(rawSize - filteredSize),
                FileOperation.getNetFileSizeDescription(rawSize),
                FileOperation.getNetFileSizeDescription(filteredSize));
        return getOutputPath();
    }

    /**
     * Gets the path of the Android App Bundle to be processed.
     *
     * @return The path of the Android App Bundle.
     */
    public abstract Path getBundlePath();

    /**
     * Gets the path where the output bundle file should be created.
     *
     * @return The path for the output bundle file.
     */
    public abstract Path getOutputPath();

    /**
     * Gets the path of the keystore file for signing the bundle (optional).
     *
     * @return The path of the keystore file, if provided.
     */
    public abstract Optional<Path> getStoreFile();

    /**
     * Gets the keystore password (optional).
     *
     * @return The keystore password, if provided.
     */
    public abstract Optional<String> getStorePassword();

    /**
     * Gets the key alias name (optional).
     *
     * @return The key alias name, if provided.
     */
    public abstract Optional<String> getKeyAlias();

    /**
     * Gets the key password (optional).
     *
     * @return The key password, if provided.
     */
    public abstract Optional<String> getKeyPassword();

    /**
     * Gets the builder for string filtering configuration.
     *
     * @return The builder for string filtering configuration.
     */
    public abstract StringFilterCommand getStringFilterBuilder();

    /**
     * Gets the builder for file filtering configuration (optional).
     *
     * @return The builder for file filtering configuration, if provided.
     */
    public abstract Optional<FileFilterCommand> getFileFilterBuilder();

    /**
     * Gets the builder for obfuscating bundle resources configuration.
     *
     * @return The builder for obfuscating bundle resources configuration.
     */
    public abstract ObfuscateBundleCommand getObfuscateBundleBuilder();

    /**
     * Gets the builder for merging duplicated resources configuration.
     *
     * @return The builder for merging duplicated resources configuration.
     */
    public abstract DuplicateResMergerCommand getDuplicateResMergeBuilder();

    /**
     * A builder class for constructing {@link Command} objects with various configuration options.
     * The builder allows setting properties related to processing Android App Bundles, including
     * filtering files, filtering strings, obfuscating resources, and merging duplicated resources.
     *
     * @see Command
     */
    @AutoValue.Builder
    public abstract static class Builder {

        /**
         * Sets the path of the Android App Bundle to be processed.
         *
         * @param bundlePath The path of the Android App Bundle.
         * @return This builder for method chaining.
         */
        public abstract Builder setBundlePath(Path bundlePath);

        /**
         * Sets the path where the output bundle file should be created.
         *
         * @param outputPath The path for the output bundle file.
         * @return This builder for method chaining.
         */
        public abstract Builder setOutputPath(Path outputPath);

        /**
         * Sets the path of the keystore file for signing the bundle (optional).
         *
         * @param storeFile The path of the keystore file.
         * @return This builder for method chaining.
         */
        public abstract Builder setStoreFile(Path storeFile);

        /**
         * Sets the keystore password (optional).
         *
         * @param storePassword The keystore password.
         * @return This builder for method chaining.
         */
        public abstract Builder setStorePassword(String storePassword);

        /**
         * Sets the key alias name (optional).
         *
         * @param keyAlias The key alias name.
         * @return This builder for method chaining.
         */
        public abstract Builder setKeyAlias(String keyAlias);

        /**
         * Sets the key password (optional).
         *
         * @param keyPassword The key password.
         * @return This builder for method chaining.
         */
        public abstract Builder setKeyPassword(String keyPassword);

        /**
         * Sets the builder for string filtering configuration.
         *
         * @param filterCommand The builder for string filtering configuration.
         * @return This builder for method chaining.
         */
        public abstract Builder setStringFilterBuilder(StringFilterCommand filterCommand);

        /**
         * Sets the builder for file filtering configuration (optional).
         *
         * @param fileFilterCommand The builder for file filtering configuration.
         * @return This builder for method chaining.
         */
        public abstract Builder setFileFilterBuilder(FileFilterCommand fileFilterCommand);

        /**
         * Sets the builder for obfuscating bundle resources configuration.
         *
         * @param obfuscateBundleCommand The builder for obfuscating bundle resources configuration.
         * @return This builder for method chaining.
         */
        public abstract Builder setObfuscateBundleBuilder(ObfuscateBundleCommand obfuscateBundleCommand);

        /**
         * Sets the builder for merging duplicated resources configuration.
         *
         * @param mergerCommand The builder for merging duplicated resources configuration.
         * @return This builder for method chaining.
         */
        public abstract Builder setDuplicateResMergeBuilder(DuplicateResMergerCommand mergerCommand);

        /**
         * Builds and returns a {@link Command} object with the specified properties.
         *
         * @return A {@link Command} object configured with the provided properties.
         */
        public abstract Command build();

        /**
         * Builds and validates a {@link Command} object based on the provided parameters and command type.
         * This method includes checks for file extensions, existence, and signing-related flags.
         *
         * @param command     The {@link Command} object to build.
         * @param commandType The type of command being executed.
         * @return A validated {@link Command} object.
         * @throws CommandExecutionException If validation checks fail.
         */
        public Command build(@NotNull Command command, TYPE commandType) {
            checkFileExistsAndReadable(command.getBundlePath());
            if (!command.getBundlePath().toFile().getName().endsWith(".aab"))
                throw CommandExecutionException.builder()
                        .withInternalMessage("Wrong properties: %s must end with '.aab'.", BUNDLE_LOCATION_FLAG)
                        .build();
            if (!command.getOutputPath().toFile().getName().endsWith(".aab"))
                throw CommandExecutionException.builder()
                        .withInternalMessage("Wrong properties: %s must end with '.aab'.", OUTPUT_FILE_FLAG)
                        .build();
            if (commandType == TYPE.DUPLICATE_RES_MERGE || commandType == TYPE.FILTER_FILE)
                checkFileDoesNotExist(command.getOutputPath());
            else if (commandType == TYPE.OBFUSCATE_BUNDLE) {
                checkFileExistsAndReadable(command.getBundlePath());
                //If a file exists, just delete it instead of throwing exception
                if (command.getOutputPath().toFile().exists())
                    command.getOutputPath().toFile().delete();
                if (command.getObfuscateBundleBuilder().getMappingPath().isPresent()) {
                    File file = command.getObfuscateBundleBuilder().getMappingPath().get().toFile();
                    checkFileExistsAndReadable(file.toPath());
                    if (!file.getName().endsWith(".txt"))
                        throw CommandExecutionException.builder()
                                .withInternalMessage("Wrong properties: %s must end with '.txt'.", MAPPING_FLAG)
                                .build();
                }
            } else if (commandType == TYPE.FILTER_STRING) {
                checkFileExistsAndReadable(command.getStringFilterBuilder().getConfigPath().get());
                checkFileDoesNotExist(command.getOutputPath());
                if (!command.getStringFilterBuilder().getConfigPath().get().toFile().getName().endsWith(".xml"))
                    throw CommandExecutionException.builder()
                            .withInternalMessage("Wrong properties: %s must end with '.xml'.", CONFIG_LOCATION_FLAG)
                            .build();
            }
            if (command.getStoreFile().isPresent()) {
                JarSigner.checkFlagPresent(command.getKeyAlias(), KEY_ALIAS_FLAG);
                JarSigner.checkFlagPresent(command.getKeyPassword(), KEY_PASSWORD_FLAG);
                JarSigner.checkFlagPresent(command.getStorePassword(), STORE_PASSWORD_FLAG);
            }
            return command;
        }
    }
}