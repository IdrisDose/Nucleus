/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.service;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.function.Function;

/**
 * Allows plugins to register their own tokens for use in templated messages.
 */
public interface NucleusMessageTokenService {

    /**
     * Registers a token for the given {@link PluginContainer}. The token will have the format <code>{{pl:pluginid:identifier(:s)}}</code>, the <code>:s</code>
     * is optional.
     *
     * @param pluginContainer The {@link PluginContainer} of the plugin.
     * @param tokenIdentifier The identifier for the token.
     * @param textFunction A {@link Function} that returns a {@link Text} to put in place of the token. The {@link CommandSource} generating the message
     *                     is provided. If {@link Optional#empty()} is returned, then {@link Text#EMPTY} is used in the token's place.
     * @throws IllegalArgumentException Thrown if the token has been registered previously.
     */
    void register(PluginContainer pluginContainer, String tokenIdentifier, Function<CommandSource, Optional<Text>> textFunction);

    /**
     * Unregisters a token.
     *
     * @param pluginContainer The {@link PluginContainer} of the plugin.
     * @param tokenIdentifier The identifier for the token.
     * @return <code>true</code> if successful.
     */
    boolean unregister(PluginContainer pluginContainer, String tokenIdentifier);

    /**
     * Unregisters all tokens for a plugin.
     *
     * @param pluginContainer The {@link PluginContainer} of the plugin.
     * @return <code>true</code> if successful.
     */
    boolean unregisterAll(PluginContainer pluginContainer);
}
