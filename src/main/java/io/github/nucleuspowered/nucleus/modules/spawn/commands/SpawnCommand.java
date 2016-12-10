/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.commands;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.spawn.events.SendToSpawnEvent;
import io.github.nucleuspowered.nucleus.modules.spawn.helpers.SpawnHelper;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand("spawn")
public class SpawnCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private WorldDataManager wcl;
    @Inject private SpawnConfigAdapter sca;

    private final String key = "world";

    @Override
    public Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> m = new HashMap<>();
        m.put("otherworlds", new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.spawn.otherworlds"), SuggestedLevel.ADMIN));
        return m;
    }

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
                GenericArguments.optional(GenericArguments.requiringPermission(GenericArguments.onlyOne(GenericArguments.world(Text.of(key))),
                        permissions.getPermissionWithSuffix("otherworlds")))};
    }

    @Override
    public CommandResult executeCommand(Player src, CommandContext args) throws Exception {
        GlobalSpawnConfig gsc = sca.getNodeOrDefault().getGlobalSpawn();
        WorldProperties wp = args.<WorldProperties>getOne(key)
            .orElseGet(() -> gsc.isOnSpawnCommand() ? gsc.getWorld().orElse(src.getWorld().getProperties()) : src.getWorld().getProperties());

        Optional<World> ow = Sponge.getServer().getWorld(wp.getUniqueId());

        if (!ow.isPresent()) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.spawn.noworld"));
            return CommandResult.empty();
        }

        Transform<World> worldTransform = SpawnHelper.getSpawn(wp, plugin, src);

        SendToSpawnEvent event = new SendToSpawnEvent(worldTransform, src, Cause.of(NamedCause.source(src), NamedCause.owner(plugin)));
        if (Sponge.getEventManager().post(event)) {
            if (event.getCancelReason().isPresent()) {
                throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.spawnother.self.failed.reason", event.getCancelReason().get()));
            }

            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.spawnother.self.failed.noreason"));
        }

        // If we don't have a rotation, then use the current rotation
        if (plugin.getTeleportHandler().teleportPlayer(src, new Location<>(ow.get(), wp.getSpawnPosition()),
                wcl.getWorld(wp.getUniqueId()).get().getSpawnRotation().orElse(src.getRotation()), sca.getNodeOrDefault().isSafeTeleport())) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.spawn.success", wp.getWorldName()));
            return CommandResult.success();
        }

        throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.spawn.fail", wp.getWorldName()));
    }
}
