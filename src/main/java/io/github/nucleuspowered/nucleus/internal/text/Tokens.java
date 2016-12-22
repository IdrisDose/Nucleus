/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.internal.text;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.api.world.World;

import java.util.Optional;

final class Tokens {

    private Tokens() {}

    static void registerNucleusTokens(NucleusPlugin plugin, NucleusTokenServiceImpl service) {
        PluginContainer container = plugin.getPluginContainer();
        service.register(container, "name", p -> Optional.of(Nucleus.getNucleus().getChatUtil().addCommandToName(p)));
        service.register(container, "prefix", p -> Optional.of(getTextFromOption(p, "prefix")));
        service.register(container, "suffix", p -> Optional.of(getTextFromOption(p, "suffix")));
        service.register(container, "displayname", p -> Optional.of(Nucleus.getNucleus().getChatUtil().addCommandToDisplayName(p)));

        service.register(container, "maxplayers", p -> Optional.of(Text.of(Sponge.getServer().getMaxPlayers())));
        service.register(container, "onlineplayers", p -> Optional.of(Text.of(Sponge.getServer().getOnlinePlayers().size())));
        service.register(container, "currentworld", p -> Optional.of(Text.of(getWorld(p).getName())));
        service.register(container, "time", p -> Optional.of(Text.of(String.valueOf(Util.getTimeFromTicks(getWorld(p).getProperties().getWorldTime())))));
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
