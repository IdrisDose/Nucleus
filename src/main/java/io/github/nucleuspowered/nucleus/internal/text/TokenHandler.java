/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.text;

import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class TokenHandler {

    private final NucleusPlugin plugin;
    private final Map<String, Function<CommandSource, Optional<Text>>> standardMap = Tokens.getStandardTokens();

    private final Text SPACE = Text.of(" ");

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") private final Optional<Text> EMPTY = Optional.empty();
    private final Function<CommandSource, Optional<Text>> defaultEntry = cs -> EMPTY;

    public TokenHandler(NucleusPlugin plugin) {
        this.plugin = plugin;
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
                return getTextFromStandardToken(token, source, addSpace);
            }
        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }

            return EMPTY;
        }
    }

    private Optional<Text> getTextFromStandardToken(String token, CommandSource source, boolean addSpace) {
        Optional<Text> opt = standardMap.getOrDefault(token.toLowerCase(), defaultEntry).apply(source);
        if (opt.isPresent() && addSpace) {
            return Optional.of(opt.get().toBuilder().append(SPACE).build());
        }

        return opt;
    }

    private Optional<Text> getTextFromPluginToken(String pluginId, String token, CommandSource source, boolean addSpace) {
        Optional<NucleusTokenServiceImpl> optionalChatService = plugin.getInternalServiceManager().getService(NucleusTokenServiceImpl.class);
        if (optionalChatService.isPresent()) {
            Optional<Text> opt = optionalChatService.get().getTokenForCommandSource(pluginId, token, source);
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
