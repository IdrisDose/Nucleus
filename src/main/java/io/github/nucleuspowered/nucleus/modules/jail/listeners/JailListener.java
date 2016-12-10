/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.api.data.JailData;
import io.github.nucleuspowered.nucleus.api.data.LocationData;
import io.github.nucleuspowered.nucleus.api.events.NucleusSendToSpawnEvent;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.InternalServiceManager;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.events.NucleusOnLoginEvent;
import io.github.nucleuspowered.nucleus.modules.jail.commands.JailCommand;
import io.github.nucleuspowered.nucleus.modules.jail.config.JailConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.jail.handlers.JailHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.format.TextColors;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JailListener extends ListenerBase {

    @Inject private UserDataManager loader;
    @Inject private InternalServiceManager ism;
    @Inject private JailConfigAdapter jailConfigAdapter;
    @Inject private JailHandler handler;

    @Listener
    public void onPlayerLogin(final NucleusOnLoginEvent event, @Getter("getTargetUser") User user, @Getter("getUserService") UserService qs) {
        JailHandler handler = ism.getService(JailHandler.class).get();

        // Jailing the player if we need to.
        if (qs.jailOnNextLogin() && qs.getJailData().isPresent()) {
            Optional<LocationData> owl = handler.getWarpLocation(user);
            if (!owl.isPresent()) {
                MessageChannel.permission(JailCommand.notifyPermission)
                    .send(Text.of(TextColors.RED, "WARNING: No jail is defined. Jailed players are going free!"));
                handler.unjailPlayer(user);
                return;
            }

            JailData jd = qs.getJailData().get();
            jd.setPreviousLocation(event.getFrom().getLocation());
            qs.setJailData(jd);
            event.setTo(owl.get().getTransform().get());
        }
    }

    /**
     * At the time the player joins, check to see if the player is muted.
     *
     * @param event The event.
     */
    @Listener(order = Order.LATE)
    public void onPlayerJoin(final ClientConnectionEvent.Join event) {
        final Player user = event.getTargetEntity();
        Optional<UserService> oqs = loader.get(user);
        if (!oqs.isPresent()) {
            return;
        }

        UserService qs = oqs.get();
        JailHandler handler = ism.getService(JailHandler.class).get();

        // Jailing the player if we need to.
        if (qs.jailOnNextLogin() && qs.getJailData().isPresent()) {
            // It exists.
            LocationData owl = handler.getWarpLocation(user).get();
            JailData jd = qs.getJailData().get();
            Optional<Duration> timeLeft = jd.getTimeLeft();
            Text message;
            message = timeLeft.map(duration -> plugin.getMessageProvider()
                .getTextMessageWithFormat("command.jail.jailed", owl.getName(), plugin.getNameUtil().getNameFromUUID(jd.getJailer()),
                    plugin.getMessageProvider().getMessageWithFormat("standard.for"), Util.getTimeStringFromSeconds(duration.getSeconds())))
                .orElseGet(() -> plugin.getMessageProvider()
                    .getTextMessageWithFormat("command.jail.jailed", owl.getName(), plugin.getNameUtil().getNameFromUUID(jd.getJailer()), "",
                        ""));

            qs.setFlying(false);
            user.sendMessage(message);
            user.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("standard.reason", jd.getReason()));
        }

        qs.setJailOnNextLogin(false);

        // Kick off a scheduled task.
        Sponge.getScheduler().createTaskBuilder().async().delay(500, TimeUnit.MILLISECONDS).execute(() -> {
            Optional<JailData> omd = qs.getJailData();
            if (omd.isPresent()) {
                JailData md = omd.get();
                md.nextLoginToTimestamp();

                omd = Util.testForEndTimestamp(qs.getJailData(), () -> handler.unjailPlayer(user));
                if (omd.isPresent()) {
                    md = omd.get();
                    onJail(md, event.getTargetEntity());
                }
            }
        }).submit(plugin);
    }

    @Listener
    public void onCommand(SendCommandEvent event, @Root Player player) {
        // Only if the command is not in the control list.
        if (checkJail(player, false) && !jailConfigAdapter.getNodeOrDefault().getAllowedCommands().stream().anyMatch(x -> event.getCommand().equalsIgnoreCase(x))) {
            event.setCancelled(true);

            // This is the easiest way to send the messages.
            checkJail(player, true);
        }
    }

    @Listener
    public void onBlockChange(ChangeBlockEvent event, @Root Player player) {
        event.setCancelled(checkJail(player, true));
    }

    @Listener
    public void onInteract(InteractEvent event, @Root Player player) {
        event.setCancelled(checkJail(player, true));
    }

    @Listener
    public void onSpawn(RespawnPlayerEvent event) {
        if (checkJail(event.getTargetEntity(), false)) {
            event.setToTransform(event.getToTransform().setLocation(handler.getWarpLocation(event.getTargetEntity()).get().getLocation().get()));
        }
    }

    @Listener(order = Order.FIRST)
    public void onChat(MessageChannelEvent.Chat event, @Root Player player) {
        if (checkJail(player, false) && jailConfigAdapter.getNodeOrDefault().isMuteOnJail()) {
            player.sendMessage(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("jail.muteonchat"));
            event.setCancelled(true);
        }
    }

    @Listener
    public void onSendToSpawn(NucleusSendToSpawnEvent event, @Getter("getTargetUser") User user) {
        if (checkJail(user, false)) {
            event.setCancelled(true);
            event.setCancelReason(plugin.getMessageProvider().getMessageWithFormat("jail.isjailed"));
        }
    }

    private boolean checkJail(final User player, boolean sendMessage) {
        Optional<UserService> oqs = loader.get(player);
        if (!oqs.isPresent()) {
            return false;
        }

        UserService qs = oqs.get();

        Optional<JailData> omd = Util.testForEndTimestamp(qs.getJailData(), () -> handler.unjailPlayer(player));
        if (omd.isPresent()) {
            if (sendMessage) {
                qs.setFlying(false);
                player.getPlayer().ifPresent(x -> onJail(omd.get(), x));
            }

            return true;
        }

        return false;
    }

    private void onJail(JailData md, Player user) {
        if (md.getEndTimestamp().isPresent()) {
            user.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("jail.playernotify.time",
                    Util.getTimeStringFromSeconds(Instant.now().until(md.getEndTimestamp().get(), ChronoUnit.SECONDS))));
        } else {
            user.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("jail.playernotify.standard"));
        }

        user.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("standard.reason", md.getReason()));
    }

}
