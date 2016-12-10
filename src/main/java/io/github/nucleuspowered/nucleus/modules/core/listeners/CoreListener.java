/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.core.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.events.NucleusOnLoginEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CoreListener extends ListenerBase {

    @Inject private UserDataManager loader;
    @Inject private CoreConfigAdapter cca;
    private boolean runSync = false;

    /* (non-Javadoc)
     * We do this last to avoid interfering with other modules.
     */
    @Listener(order = Order.LATE)
    public void onPlayerLoginLast(final ClientConnectionEvent.Login event, @Getter("getProfile") GameProfile profile,
        @Getter("getTargetUser") User user) {

        loader.get(profile.getUniqueId()).ifPresent(qsu -> {
            if (event.getFromTransform().equals(event.getToTransform())) {
                NucleusOnLoginEvent onLoginEvent = new NucleusOnLoginEvent(Cause.of(NamedCause.source(profile)), user, qsu, event.getFromTransform());
                Sponge.getEventManager().post(onLoginEvent);
                if (onLoginEvent.getTo().isPresent()) {
                    event.setToTransform(onLoginEvent.getTo().get());
                    qsu.removeLocationOnLogin();
                    return;
                }

                // If we have a location to send them to in the config, send them there now!
                Optional<Location<World>> olw = qsu.getLocationOnLogin();
                if (olw.isPresent()) {
                    event.setToTransform(event.getFromTransform().setLocation(olw.get()));
                    qsu.removeLocationOnLogin();
                }
            }
        });
    }

    /* (non-Javadoc)
     * We do this first to try to get the first play status as quick as possible.
     */
    @Listener(order = Order.FIRST)
    public void onPlayerJoinFirst(final ClientConnectionEvent.Join event, @Getter("getTargetEntity") final Player player) {
        try {
            UserService qsu = loader.get(player).get();
            qsu.setLastLogin(Instant.now());
            qsu.setFirstPlay(Util.isFirstPlay(player));
            qsu.setLastIp(player.getConnection().getAddress().getAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Listener
    @SuppressWarnings("ConstantConditionalExpression")
    public void onPlayerQuit(final ClientConnectionEvent.Disconnect event, @Getter("getTargetEntity") final Player player) {
        // There is an issue in Sponge where the connection may not even exist, because they were disconnected before the connection was
        // completely established.
        //noinspection ConstantConditions
        if (player.getConnection() == null || player.getConnection().getAddress() == null) {
            return;
        }

        final Location<World> location = player.getLocation();
        final InetAddress address = player.getConnection().getAddress().getAddress();
        if (runSync) {
            // Work around things not existing, run quit events sync just as the server draws to a close.
            onPlayerQuitInner(player, location, address);
        } else {
            Sponge.getScheduler().createAsyncExecutor(plugin).schedule(() -> onPlayerQuitInner(player, location, address), 200, TimeUnit.MILLISECONDS);
        }
    }

    private void onPlayerQuitInner(final Player player, final Location<World> location, final InetAddress address) {
        try {
            this.plugin.getUserDataManager().get(player).ifPresent(x -> {
                x.setOnLogout(location);
                x.setLastIp(address);
            });
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }

    @Listener
    public void onServerAboutToStop(final GameStoppingServerEvent event) {
        runSync = true;
        if (cca.getNodeOrDefault().isKickOnStop()) {
            Iterator<Player> players = Sponge.getServer().getOnlinePlayers().iterator();
            while (players.hasNext()) {
                Text msg = TextSerializers.FORMATTING_CODE.deserialize(cca.getNodeOrDefault().getKickOnStopMessage());
                if (msg.isEmpty()) {
                    players.next().kick();
                } else {
                    players.next().kick(msg);
                }
            }
        }
    }

    @Listener
    public void onGameReload(final GameReloadEvent event) {
        plugin.reload();
        CommandSource requester = event.getCause().first(CommandSource.class).orElse(Sponge.getServer().getConsole());
        requester.sendMessage(Text.of(TextColors.YELLOW, "[NucleusPlugin] ", plugin.getMessageProvider().getTextMessageWithFormat("command.reload.one")));
        requester.sendMessage(Text.of(TextColors.YELLOW, "[NucleusPlugin] ", plugin.getMessageProvider().getTextMessageWithFormat("command.reload.two")));
    }
}
