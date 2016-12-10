/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.jail.handlers;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.api.data.JailData;
import io.github.nucleuspowered.nucleus.api.data.LocationData;
import io.github.nucleuspowered.nucleus.api.service.NucleusJailService;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class JailHandler implements NucleusJailService {

    @Inject private GeneralService store;

    private final NucleusPlugin plugin;

    public JailHandler(NucleusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<LocationData> getJail(String warpName) {
        return store.getJailLocation(warpName);
    }

    @Override
    public boolean removeJail(String warpName) {
        return store.removeJail(warpName);
    }

    @Override
    public boolean setJail(String warpName, Location<World> location, Vector3d rotation) {
        return store.addJail(warpName, location, rotation);
    }

    @Override
    public Map<String, LocationData> getJails() {
        return store.getJails();
    }

    @Override
    public boolean isPlayerJailed(User user) {
        return getPlayerJailData(user).isPresent();
    }

    @Override
    public Optional<JailData> getPlayerJailData(User user) {
        try {
            return plugin.getUserDataManager().get(user).get().getJailData();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean jailPlayer(User user, JailData data) {
        UserService iqsu;
        try {
            iqsu = plugin.getUserDataManager().get(user).get();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (iqsu.getJailData().isPresent()) {
            return false;
        }

        // Get the jail.
        Optional<LocationData> owl = getJail(data.getJailName());
        LocationData wl = owl.orElseGet(() -> {
            if (!getJails().isEmpty()) {
                return null;
            }

            return getJails().entrySet().stream().findFirst().get().getValue();
        });

        if (wl == null) {
            return false;
        }

        iqsu.setJailData(data);
        if (user.isOnline()) {
            Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> {
                Player player = user.getPlayer().get();
                plugin.getTeleportHandler().teleportPlayer(player, owl.get().getLocation().get(), owl.get().getRotation(),
                    NucleusTeleportHandler.TeleportMode.NO_CHECK);
                iqsu.setFlying(false);
            });
        } else {
            iqsu.setJailOnNextLogin(true);
        }

        return true;
    }

    @Override
    public boolean unjailPlayer(User user) {
        final UserService iqsu;
        try {
            iqsu = plugin.getUserDataManager().get(user).get();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        Optional<JailData> ojd = iqsu.getJailData();
        if (!ojd.isPresent()) {
            return false;
        }

        Optional<Location<World>> ow = ojd.get().getPreviousLocation();
        if (user.isOnline()) {
            Player player = user.getPlayer().get();
            Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> {
                player.setLocation(ow.isPresent() ? ow.get() : player.getWorld().getSpawnLocation());
                player.sendMessage(NucleusPlugin.getNucleus().getMessageProvider().getTextMessageWithFormat("jail.elapsed"));

                // Remove after the teleport for the back data.
                iqsu.removeJailData();
            });
        } else {
            iqsu.sendToLocationOnLogin(ow.isPresent() ? ow.get()
                    : new Location<>(Sponge.getServer().getWorld(Sponge.getServer().getDefaultWorld().get().getUniqueId()).get(),
                            Sponge.getServer().getDefaultWorld().get().getSpawnPosition()));
            iqsu.removeJailData();
        }

        return true;
    }

    public Optional<LocationData> getWarpLocation(User user) {
        if (!isPlayerJailed(user)) {
            return Optional.empty();
        }

        Optional<LocationData> owl = getJail(getPlayerJailData(user).get().getJailName());
        if (!owl.isPresent()) {
            Collection<LocationData> wl = getJails().values();
            if (wl.isEmpty()) {
                return Optional.empty();
            }

            owl = wl.stream().findFirst();
        }

        return owl;
    }
}
