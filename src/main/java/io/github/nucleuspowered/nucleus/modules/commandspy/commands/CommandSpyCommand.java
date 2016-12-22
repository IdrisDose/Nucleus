/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.messages.MessageProvider;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Map;

@Permissions
@RegisterCommand("commandspy")
public class CommandSpyCommand extends AbstractCommand<Player> {

    private final String truefalse = "true/false";

    @Inject private UserDataManager userDataManager;

    @Override protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = super.permissionSuffixesToRegister();
        mspi.put("exempt.target", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.commandspy.exempt.target"), SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optional(GenericArguments.bool(Text.of(truefalse)))
        };
    }

    @Override public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        UserService service = userDataManager.get(src).get();
        boolean to = args.<Boolean>getOne(truefalse).orElseGet(() -> !service.isCommandSpy());
        service.setCommandSpy(to);

        MessageProvider mp = plugin.getMessageProvider();
        src.sendMessage(mp.getTextMessageWithFormat("command.commandspy.success", mp.getMessageWithFormat(to ? "standard.enabled" : "standard.disabled")));
        return CommandResult.success();
    }
}
