/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.text;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

final class Tokens {

    private Tokens() {}

    static Map<String, Function<CommandSource, Optional<Text>>> getStandardTokens() {
        Map<String, Function<CommandSource, Optional<Text>>> t = new HashMap<>();

        t.put("name", p -> Optional.of(Nucleus.getNucleus().getChatUtil().addCommandToName(p)));
        t.put("prefix", p -> Optional.of(getTextFromOption(p, "prefix")));
        t.put("suffix", p -> Optional.of(getTextFromOption(p, "suffix")));
        t.put("displayname", p -> Optional.of(Nucleus.getNucleus().getChatUtil().addCommandToDisplayName(p)));

        t.put("maxplayers", p -> Optional.of(Text.of(Sponge.getServer().getMaxPlayers())));
        t.put("onlineplayers", p -> Optional.of(Text.of(Sponge.getServer().getOnlinePlayers().size())));
        t.put("currentworld", p -> Optional.of(Text.of(getWorld(p).getName())));
        t.put("time", p -> Optional.of(Text.of(String.valueOf(Util.getTimeFromTicks(getWorld(p).getProperties().getWorldTime())))));

        return t;
    }

    private static World getWorld(CommandSource p) {
        World world;
        if (p instanceof Locatable) {
            world = ((Locatable) p).getWorld();
        } else {
            world = Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorldName()).get();
        }

        return world;
    }

    private static Text getTextFromOption(CommandSource cs, String option) {
        if (cs instanceof Player) {
            Optional<String> os = Util.getOptionFromSubject(cs, option);
            if (os.isPresent() && !os.get().isEmpty()) {
                return TextSerializers.FORMATTING_CODE.deserialize(os.get());
            }
        }

        return Text.EMPTY;
    }
}
