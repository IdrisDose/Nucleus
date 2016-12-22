/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.text;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.PluginInfo;
import io.github.nucleuspowered.nucleus.Util;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

public class TokenHandler {

    private final NucleusPlugin plugin;
    private boolean registered = false;
    private NucleusTokenServiceImpl service = null;

    private final Text SPACE = Text.of(" ");

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") private final Optional<Text> EMPTY = Optional.empty();

    public TokenHandler(NucleusPlugin plugin) {
        this.plugin = plugin;
    }

    public final void register(NucleusTokenServiceImpl service) {
        Preconditions.checkState(!registered);
        Tokens.registerNucleusTokens(plugin, service);
        this.service = service;
        registered = true;
    }

    public final Optional<Text> getTextFromToken(String token, CommandSource source) {
        token = token.toLowerCase();
        if (token.startsWith("{{") && token.endsWith("}}")) {
            token = token.substring(2, token.length() - 2);
        }

        boolean addSpace = token.endsWith(":s");
        if (addSpace) {
            token = token.substring(0, token.length() - 2);
        }

        try {
            if (token.startsWith("pl:")) {
                // Plugin identifers are of the form pl:<pluginid>:<identifier>
                String[] tokSplit = token.split(":", 3);
                if (tokSplit.length < 3) {
                    return EMPTY;
                }

                return getTextFromPluginToken(tokSplit[1], tokSplit[2], source, addSpace);
            } else if (token.startsWith("o:")) { // Option identifier.
                return getTextFromOption(source, token.substring(2), addSpace);
            } else {
                // Standard.
                return getTextFromPluginToken(PluginInfo.ID, token, source, addSpace);
            }
        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }

            return EMPTY;
        }
    }

    private Optional<Text> getTextFromPluginToken(String pluginId, String token, CommandSource source, boolean addSpace) {
        if (service != null) {
            Optional<Text> opt = service.applyToken(pluginId, token, source);
            if (opt.isPresent() && addSpace) {
                return Optional.of(opt.get().toBuilder().append(SPACE).build());
            }

            return opt;
        }

        return EMPTY;
    }

    private Optional<Text> getTextFromOption(CommandSource cs, String option, boolean addSpace) {
        if (cs instanceof Player) {
            Optional<String> os = Util.getOptionFromSubject(cs, option);
            if (os.isPresent() && !os.get().isEmpty()) {
                String s = os.get();
                if (addSpace) {
                    s += " ";
                }

                return Optional.of(TextSerializers.FORMATTING_CODE.deserialize(s));
            }
        }

        return Optional.of(Text.EMPTY);
    }
}
