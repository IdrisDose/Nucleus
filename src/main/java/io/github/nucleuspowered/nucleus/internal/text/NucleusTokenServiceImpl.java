/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.text;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.PluginInfo;
import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class NucleusTokenServiceImpl implements NucleusMessageTokenService {

    private final Map<String, Map<String, Function<CommandSource, Optional<Text>>>> tokenStore = Maps.newHashMap();

    @Override public void register(PluginContainer pluginContainer, String tokenIdentifier, Function<CommandSource, Optional<Text>> textFunction) {
        Preconditions.checkNotNull(pluginContainer);
        Preconditions.checkNotNull(tokenIdentifier);
        Preconditions.checkNotNull(textFunction);

        Map<String, Function<CommandSource, Optional<Text>>> inner = tokenStore.computeIfAbsent(pluginContainer.getId().toLowerCase(), k -> Maps.newHashMap());

        tokenIdentifier = tokenIdentifier.toLowerCase();
        if (inner.containsKey(tokenIdentifier)) {
            // TODO: Different exception
            throw new IllegalArgumentException(tokenIdentifier);
        }

        inner.put(tokenIdentifier, textFunction);
    }

    @Override public boolean unregister(PluginContainer pluginContainer, String tokenIdentifier) {
        Preconditions.checkState(!pluginContainer.getId().equalsIgnoreCase(PluginInfo.ID), "Cannot remove Nucleus tokens");

        Map<String, Function<CommandSource, Optional<Text>>> inner = tokenStore.get(pluginContainer.getId().toLowerCase());
        if (inner != null && inner.containsKey(tokenIdentifier.toLowerCase())) {
            inner.remove(tokenIdentifier.toLowerCase());
            return true;
        }

        return false;
    }

    @Override public boolean unregisterAll(PluginContainer pluginContainer) {
        Preconditions.checkState(!pluginContainer.getId().equalsIgnoreCase(PluginInfo.ID), "Cannot remove Nucleus tokens");

        if (tokenStore.containsKey(pluginContainer.getId().toLowerCase())) {
            tokenStore.remove(pluginContainer.getId().toLowerCase());
            return true;
        }

        return false;
    }

    @Override public Optional<Function<CommandSource, Optional<Text>>> getToken(String plugin, String token) {
        Map<String, Function<CommandSource, Optional<Text>>> inner = tokenStore.computeIfAbsent(plugin.toLowerCase(), k -> Maps.newHashMap());
        return Optional.ofNullable(inner.get(token.toLowerCase()));
    }

    @Override public Text formatAmpersandEncodedStringWithTokens(String input, CommandSource source) {
        return Nucleus.getNucleus().getChatUtil().getMessageFromTemplate(input, source, true);
    }
}
