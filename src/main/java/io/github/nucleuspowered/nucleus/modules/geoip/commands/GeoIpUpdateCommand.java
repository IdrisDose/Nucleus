/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.geoip.commands;

import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.modules.geoip.handlers.GeoIpDatabaseHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;

@RunAsync
@NoCooldown
@NoCost
@NoWarmup
@Permissions(prefix = "geoip")
@RegisterCommand(value = "update", subcommandOf = GeoIpCommand.class)
public class GeoIpUpdateCommand extends AbstractCommand<CommandSource> {

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.geoip.update.start"));
        try {
            plugin.getInternalServiceManager().getService(GeoIpDatabaseHandler.class).get().load(
                GeoIpDatabaseHandler.LoadType.DOWNLOAD, false);
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.geoip.update.complete"));
        } catch (IllegalStateException e) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.geoip.update.licence"));
        } catch (Exception e) {
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }

        return CommandResult.success();
    }
}
