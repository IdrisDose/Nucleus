/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.Kit;
import io.github.nucleuspowered.nucleus.argumentparsers.KitArgument;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.command.AbstractCommand;
import io.github.nucleuspowered.nucleus.internal.command.ReturnMessageException;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.kit.config.KitConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.kit.handlers.KitHandler;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gives a kit to a player.
 */
@Permissions(prefix = "kit")
@RegisterCommand(value = "give", subcommandOf = KitCommand.class)
public class KitGiveCommand extends AbstractCommand<CommandSource> {

    @Inject private KitHandler handler;
    @Inject private KitConfigAdapter kitConfigAdapter;
    @Inject private UserDataManager userDataManager;

    private final String playerKey = "player";
    private final String kitKey = "kit";

    @Override protected Map<String, PermissionInformation> permissionSuffixesToRegister() {
        Map<String, PermissionInformation> mspi = Maps.newHashMap();
        mspi.put("overridecheck", new PermissionInformation("permissions.kit.give.override", SuggestedLevel.ADMIN));
        return mspi;
    }

    @Override public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().permissionFlag(permissions.getPermissionWithSuffix("overridecheck"), "i", "-ignore").buildWith(
                GenericArguments.none()
            ),
            GenericArguments.onlyOne(GenericArguments.player(Text.of(playerKey))),
            GenericArguments.onlyOne(new KitArgument(Text.of(kitKey), kitConfigAdapter, handler, false))
        };
    }

    @Override public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        KitArgument.KitInfo kitInfo = args.<KitArgument.KitInfo>getOne(kitKey).get();
        Player player = args.<Player>getOne(playerKey).get();
        boolean skip = args.hasAny("i");

        if (src instanceof Player && player.getUniqueId().equals(((Player) src).getUniqueId())) {
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.give.self"));
        }

        UserService user = userDataManager.get(player.getUniqueId()).get();
        Kit kit = kitInfo.kit;
        String kitName = kitInfo.name;
        Instant now = Instant.now();

        // If the kit was used before...
        if (!skip) {
            Optional<Instant> oi = Util.getValueIgnoreCase(user.getKitLastUsedTime(), kitName);
            if (oi.isPresent()) {

                // if it's one time only and the user does not have an exemption...
                if (kit.isOneTime() && !permissions.testSuffix(player, "exempt.onetime")) {
                    // tell the user.
                    throw new ReturnMessageException(
                        plugin.getMessageProvider().getTextMessageWithFormat("command.kit.give.onetime.alreadyredeemed",
                            plugin.getNameUtil().getSerialisedName(player), kitName));
                }

                // If we have a cooldown for the kit, and we don't have permission to
                // bypass it...
                if (!permissions.testCooldownExempt(player) && kit.getInterval().getSeconds() > 0) {

                    // ...and we haven't reached the cooldown point yet...
                    Instant timeForNextUse = oi.get().plus(kit.getInterval());
                    if (timeForNextUse.isAfter(now)) {
                        Duration d = Duration.between(now, timeForNextUse);

                        // tell the user.
                        throw new ReturnMessageException(plugin.getMessageProvider()
                            .getTextMessageWithFormat("command.kit.give.cooldown",
                                plugin.getNameUtil().getSerialisedName(player), Util.getTimeStringFromSeconds(d.getSeconds()), kitName));
                    }
                }
            }
        }

        List<Optional<ItemStackSnapshot>> slotList = Lists.newArrayList();
        Util.getStandardInventory(player).slots().forEach(x -> slotList.add(x.peek().map(ItemStack::createSnapshot)));

        boolean mustConsumeAll = kitConfigAdapter.getNodeOrDefault().isMustGetAll();
        boolean dropItems = kitConfigAdapter.getNodeOrDefault().isDropKitIfFull();
        Tristate tristate = Util.addToStandardInventory(player, kit.getStacks(), !mustConsumeAll && dropItems);
        if (tristate != Tristate.TRUE) {
            if (mustConsumeAll) {
                Inventory inventory = Util.getStandardInventory(player);
                inventory.clear();

                // Slots
                Iterator<Inventory> slot = inventory.slots().iterator();

                // Slots to restore
                slotList.forEach(x -> {
                    Inventory i = slot.next();
                    x.ifPresent(y -> i.offer(y.createStack()));
                });

                throw new ReturnMessageException(plugin.getMessageProvider()
                    .getTextMessageWithFormat("command.kit.give.fullinventorynosave", plugin.getNameUtil().getSerialisedName(player)));
            } else if (dropItems) {
                src.sendMessage(plugin.getMessageProvider()
                    .getTextMessageWithFormat("command.kit.give.itemsdropped", plugin.getNameUtil().getSerialisedName(player)));
            } else {
                src.sendMessage(plugin.getMessageProvider()
                    .getTextMessageWithFormat("command.kit.give.fullinventory", plugin.getNameUtil().getSerialisedName(player)));
            }
        }

        // If something was consumed, consider a success.
        if (dropItems || tristate != Tristate.FALSE) {

            // Register the last used time. Do it for everyone, in case permissions or cooldowns change later
            if (!skip) {
                user.addKitLastUsedTime(kitName, now);
            }

            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.give.spawned", plugin.getNameUtil().getSerialisedName(player), kitName));
            player.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.spawned", kitName));
            return CommandResult.success();
        } else {
            // Failed.
            throw new ReturnMessageException(plugin.getMessageProvider().getTextMessageWithFormat("command.kit.give.fail", plugin.getNameUtil().getSerialisedName(player), kitName));
        }
    }
}
