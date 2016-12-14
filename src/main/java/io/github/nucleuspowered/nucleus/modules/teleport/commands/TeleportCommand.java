/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.teleport.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.argumentparsers.NicknameArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NoCostArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.NoWarmupArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.TwoPlayersArgument;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.teleport.config.TeleportConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.teleport.handlers.TeleportHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions(prefix = "teleport", mainOverride = "teleport", suggestedLevel = SuggestedLevel.MOD)
@RegisterCommand(value = "teleport", rootAliasRegister = "tp")
public class TeleportCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String playerFromKey = "playerFrom";
    private final String playerKey = "player";
    private final String quietKey = "quiet";

    @Inject private TeleportHandler handler;
    @Inject private TeleportConfigAdapter tca;
    @Inject private UserDataManager userDataManager;

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("others", new PermissionInformation("permission.teleport.others", SuggestedLevel.ADMIN));
        m.put("quiet", new PermissionInformation("permission.teleport.quiet", SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
       return new CommandElement[]{
                GenericArguments.flags().flag("f")
                    .valueFlag(GenericArguments.requiringPermission(GenericArguments.bool(Text.of(quietKey)), permissions.getPermissionWithSuffix("quiet")), "q")
                    .buildWith(GenericArguments.none()),

                    // Either we get two arguments, or we get one.
                    GenericArguments.firstParsing(
                        // <player> <player>
                        // TODO: Hook up with selectors
                        GenericArguments.requiringPermission(new NoCostArgument(new NoWarmupArgument(new TwoPlayersArgument(Text.of(playerFromKey), Text.of(playerKey)))),
                                permissions.getPermissionWithSuffix("others")),

                    // <player>
                    GenericArguments.onlyOne(new NicknameArgument(Text.of(playerKey), plugin.getUserDataManager(), NicknameArgument.UnderlyingType.PLAYER)))
       };
    }

    @Override
    public ContinueMode preProcessChecks(CommandSource source, CommandContext args) {
        // Do the /tptoggle check now, no need to go through a warmup then...
        if (source instanceof Player && !TeleportHandler.canBypassTpToggle(source)) {
            Player to = args.<Player>getOne(playerKey).get();
            if (!userDataManager.get(to).get().isTeleportToggled()) {
                source.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("teleport.fail.targettoggle", to.getName()));
                return ContinueMode.STOP;
            }
        }

        return ContinueMode.CONTINUE;
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        boolean beQuiet = args.<Boolean>getOne(quietKey).orElse(tca.getNodeOrDefault().isDefaultQuiet());
        Optional<Player> ofrom = args.getOne(playerFromKey);
        Player from;
        if (ofrom.isPresent()) {
            from = ofrom.get();
            if (from.equals(src)) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.teleport.player.noself"));
                return CommandResult.empty();
            }
        } else if (src instanceof Player) {
            from = (Player) src;
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.playeronly"));
            return CommandResult.empty();
        }

        Player pl = args.<Player>getOne(playerKey).get();
        if (handler.getBuilder().setSource(src).setFrom(from).setTo(pl).setSafe(!args.<Boolean>getOne("f").orElse(false))
                .setSilentTarget(beQuiet).startTeleport()) {
            return CommandResult.success();
        }

        return CommandResult.empty();
    }
}
