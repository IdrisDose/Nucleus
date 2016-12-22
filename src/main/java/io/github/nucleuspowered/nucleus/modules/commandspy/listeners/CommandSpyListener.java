/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy.listeners;

import com.google.common.collect.Sets;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.annotations.ConditionalListener;
import io.github.nucleuspowered.nucleus.modules.commandspy.CommandSpyModule;
import io.github.nucleuspowered.nucleus.modules.commandspy.commands.CommandSpyCommand;
import io.github.nucleuspowered.nucleus.modules.commandspy.config.CommandSpyConfig;
import io.github.nucleuspowered.nucleus.modules.commandspy.config.CommandSpyConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.text.Text;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

@ConditionalListener(CommandSpyListener.Test.class)
public class CommandSpyListener extends ListenerBase.Reloadable {

    private CommandPermissionHandler permissionHandler = null;

    private CommandSpyConfig config;
    @Inject private UserDataManager dataManager;
    @Inject private ChatUtil chatUtil;

    @Listener(order = Order.LAST)
    public void onCommand(SendCommandEvent event, @Root Player player) {
        if (permissionHandler == null) {
            permissionHandler = plugin.getPermissionRegistry().getService(CommandSpyCommand.class);
        }

        if (!permissionHandler.testSuffix(player, "exempt.target")) {
            boolean cont = !config.isUseWhitelist();
            if (!cont) {
                String command = event.getCommand().toLowerCase();
                Optional<? extends CommandMapping> oc = Sponge.getCommandManager().get(command, player);
                Set<String> cmd;

                // If the command exists, then get all aliases.
                cmd = oc.map(commandMapping -> commandMapping.getAllAliases().stream().map(String::toLowerCase).collect(Collectors.toSet()))
                    .orElseGet(() -> Sets.newHashSet(command));
                cont = config.getCommands().stream().map(String::toLowerCase).anyMatch(cmd::contains);
            }

            // If the command is in the list, report it.
            if (cont) {
                List<Player> playerList = Sponge.getServer().getOnlinePlayers()
                    .stream()
                    .filter(x -> !x.getUniqueId().equals(player.getUniqueId()))
                    .filter(x -> permissionHandler.testBase(x))
                    .filter(x -> dataManager.get(x).get().isCommandSpy())
                    .collect(Collectors.toList());

                if (!playerList.isEmpty()) {
                    Text prefix = chatUtil.getMessageFromTemplate(config.getTemplate(), player, true);
                    ChatUtil.StyleTuple st = chatUtil.getLastColourAndStyle(prefix, null);
                    Text messageToSend = prefix
                        .toBuilder().append(Text.of(st.colour, st.style, "/" + event.getCommand() + " " + event.getArguments())).build();
                    playerList.forEach(x -> x.sendMessage(messageToSend));
                }
            }
        }
    }

    @Override public void onReload() throws Exception {
        config = plugin.getModuleContainer().getConfigAdapterForModule(CommandSpyModule.ID, CommandSpyConfigAdapter.class)
            .getNodeOrDefault();
    }

    public static class Test implements Predicate<Nucleus> {

        @Override public boolean test(Nucleus nucleus) {
            try {
                CommandSpyConfig csc = nucleus.getModuleContainer().getConfigAdapterForModule(CommandSpyModule.ID, CommandSpyConfigAdapter.class)
                    .getNodeOrDefault();
                return !csc.isUseWhitelist() || !csc.getCommands().isEmpty();
            } catch (NoModuleException | IncorrectAdapterTypeException e) {
                if (nucleus.isDebugMode()) {
                    e.printStackTrace();
                }

                return false;
            }
        }
    }
}
