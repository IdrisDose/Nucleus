/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.admin.commands;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import io.github.nucleuspowered.nucleus.modules.admin.config.AdminConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.admin.config.BroadcastConfig;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.List;

@RunAsync
@NoCooldown
@NoCost
@NoWarmup
@Permissions
@RegisterCommand({ "broadcast", "bcast", "bc" })
public class BroadcastCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {
    private final String message = "message";
    @Inject private AdminConfigAdapter adminConfigAdapter;
    @Inject private ChatUtil chatUtil;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] { GenericArguments.onlyOne(GenericArguments.remainingJoinedStrings(Text.of(message))) };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        String m = args.<String>getOne(message).get();
        BroadcastConfig bc = adminConfigAdapter.getNodeOrDefault().getBroadcastMessage();
        List<Text> messages = Lists.newArrayList();

        ChatUtil.StyleTuple cst = ChatUtil.EMPTY;
        String p = bc.getPrefix();
        if (!p.trim().isEmpty()) {
            messages.add(chatUtil.getMessageFromTemplate(p, src, true));
            cst = chatUtil.getLastColourAndStyle(messages.get(0), ChatUtil.EMPTY);
        }

        messages.add(Text.of(cst.colour, cst.style, chatUtil.addUrlsToAmpersandFormattedString(m)));

        String s = bc.getSuffix();
        if (!s.trim().isEmpty()) {
            messages.add(Text.of(cst.colour, cst.style, chatUtil.getMessageFromTemplate(s, src, true)));
        }

        MessageChannel.TO_ALL.send(src, Text.joinWith(Text.of(" "), messages));
        return CommandResult.success();
    }
}
