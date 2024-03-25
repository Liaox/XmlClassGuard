package com.overseas.reschiper.plugin.internal;

import com.android.build.gradle.api.ApplicationVariant;
import com.overseas.reschiper.plugin.model.KeyStore;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class SigningConfig {
    @Contract("_ -> new")
    public static @NotNull KeyStore getSigningConfig(@NotNull ApplicationVariant variant) {
        return new KeyStore(
                variant.getSigningConfig().getStoreFile(),
                variant.getSigningConfig().getStorePassword(),
                variant.getSigningConfig().getKeyAlias(),
                variant.getSigningConfig().getKeyPassword()
        );
    }
}
