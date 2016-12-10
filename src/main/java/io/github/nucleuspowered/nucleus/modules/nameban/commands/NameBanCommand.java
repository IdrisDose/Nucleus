/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.nameban.commands;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.RegexArgument;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.modules.nameban.config.NameBanConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.stream.Collectors;

@Permissions
@RunAsync
@NoWarmup
@NoCost
@NoCooldown
@RegisterCommand("nameban")
public class NameBanCommand extends AbstractCommand<CommandSource> {

    private final String nameKey = "name";
    private final String reasonKey = "reason";

    @Inject private NameBanConfigAdapter nameBanConfigAdapter;

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            new RegexArgument(Text.of(nameKey), Util.usernameRegexPattern, "command.nameban.notvalid", ((commandSource, commandArgs, commandContext) -> {
                try {
                    String arg = commandArgs.peek().toLowerCase();
                    return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().toLowerCase().startsWith(arg))
                        .map(User::getName)
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    return Lists.newArrayList();
                }
            })),
            GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of(reasonKey)))
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        String name = args.<String>getOne(nameKey).get().toLowerCase();
        String reason = args.<String>getOne(reasonKey).orElse(nameBanConfigAdapter.getNodeOrDefault().getDefaultReason());

        if (plugin.getNameBanService().setBan(name, reason)) {
            Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().equalsIgnoreCase(name)).findFirst()
                .ifPresent(x -> x.kick(TextSerializers.FORMATTING_CODE.deserialize(reason)));
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.nameban.success", name));
            return CommandResult.success();
        }

        throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.nameban.failed", name));
    }
}
