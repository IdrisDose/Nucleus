/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import io.github.nucleuspowered.nucleus.PluginInfo;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@Permissions
@RegisterCommand({"blockzap", "zapblock"})
public class BlockZapCommand extends AbstractCommand<CommandSource> {

    private final String locationKey = "location";

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.onlyOne(GenericArguments.location(Text.of(locationKey)))
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        Location<World> location = args.<Location<World>>getOne(locationKey).get();
        if (location.getBlockType() == BlockTypes.AIR) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.blockzap.alreadyair", location.getPosition().toString(), location.getExtent().getName()));
        }

        if (location.setBlock(BlockTypes.AIR.getDefaultState(), BlockChangeFlag.ALL,
                Cause.of(NamedCause.owner(Sponge.getPluginManager().getPlugin(PluginInfo.ID).get()), NamedCause.source(src)))) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.blockzap.success", location.getPosition().toString(), location.getExtent().getName()));
            return CommandResult.success();
        }

        throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.blockzap.fail", location.getPosition().toString(), location.getExtent().getName()));
    }
}
