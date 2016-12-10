/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.listeners;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.dataservices.GeneralService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.dataservices.loaders.WorldDataManager;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.internal.teleport.NucleusTeleportHandler;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.spawn.config.GlobalSpawnConfig;
import io.github.nucleuspowered.nucleus.modules.spawn.config.SpawnConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SpawnListener extends ListenerBase {

    @Inject private GeneralService store;
    @Inject private UserDataManager loader;
    @Inject private WorldDataManager wcl;
    @Inject private CoreConfigAdapter cca;
    @Inject private SpawnConfigAdapter sca;

    private final String spawnExempt = PermissionRegistry.PERMISSIONS_PREFIX + "spawn.exempt.login";

    @Override
    public Map<String, PermissionInformation> getPermissions() {
        Map<String, PermissionInformation> mpi = Maps.newHashMap();
        mpi.put(spawnExempt, new PermissionInformation(plugin.getMessageProvider().getMessageWithFormat("permission.spawn.exempt.login"), SuggestedLevel.ADMIN));
        return mpi;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Login loginEvent) {
        UUID pl = loginEvent.getProfile().getUniqueId();
        boolean first = !loader.getUser(pl).isPresent() || !loader.getUser(pl).get().getLastLogin().isPresent();

        try {
            if (first) {
                // first spawn.
                Optional<Transform<World>> ofs = store.getFirstSpawn();

                // Bit of an odd line, but what what is going on here is checking for first spawn, and if it exists, then
                // setting the location the player safely. If this cannot be done in either case, send them to world spawn.
                if (ofs.isPresent()) {
                    NucleusTeleportHandler.TeleportMode mode = sca.getNodeOrDefault().isSafeTeleport() ? NucleusTeleportHandler.TeleportMode.SAFE_TELEPORT : NucleusTeleportHandler.TeleportMode.WALL_CHECK;
                    Optional<Location<World>> location = plugin.getTeleportHandler().getSafeLocation(null, ofs.get().getLocation(), mode);

                    if (location.isPresent()) {
                        loginEvent.setToTransform(new Transform<>(location.get().getExtent(), location.get().getPosition().add(0.5, 0, 0.5), ofs.get().getRotation()));
                        return;
                    }

                    WorldProperties w = Sponge.getServer().getDefaultWorld().get();
                    loginEvent.setToTransform(new Transform<>(new Location<>(Sponge.getServer().getWorld(w.getUniqueId()).get(), w.getSpawnPosition().add(0.5, 0, 0.5))));

                    // We don't want to boot them elsewhere.
                    return;
                }
            }
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }

        // Throw them to the default world spawn if the config suggests so.
        User user = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).getOrCreate(loginEvent.getProfile());
        if (sca.getNodeOrDefault().isSpawnOnLogin() && !user.hasPermission(spawnExempt)) {

            GlobalSpawnConfig sc = sca.getNodeOrDefault().getGlobalSpawn();
            World world = loginEvent.getFromTransform().getExtent();
            if (sc.isOnLogin() && sc.getWorld().isPresent()) {
                world = Sponge.getServer().getWorld(sc.getWorld().get().getUniqueId()).orElse(world);
            }

            Location<World> lw = world.getSpawnLocation().add(0.5, 0, 0.5);
            Optional<Location<World>> safe = plugin.getTeleportHandler().getSafeLocation(null, lw,
                    sca.getNodeOrDefault().isSafeTeleport() ? NucleusTeleportHandler.TeleportMode.SAFE_TELEPORT_ASCENDING : NucleusTeleportHandler.TeleportMode.NO_CHECK);
            if (safe.isPresent()) {
                try {
                    Optional<Vector3d> ov = wcl.getWorld(world.getUniqueId()).get().getSpawnRotation();
                    if (ov.isPresent()) {
                        loginEvent.setToTransform(new Transform<>(safe.get().getExtent(),
                                safe.get().getPosition().add(0.5, 0, 0.5),
                                ov.get()));
                        return;
                    }
                } catch (Exception e) {
                    //
                }

                loginEvent.setToTransform(new Transform<>(safe.get().add(0.5, 0, 0.5)));
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onRespawn(RespawnPlayerEvent event) {
        if (event.isBedSpawn()) {
            // Nope, we don't care.
            return;
        }

        GlobalSpawnConfig sc = sca.getNodeOrDefault().getGlobalSpawn();
        World world = event.getToTransform().getExtent();

        // Get the world.
        if (sc.isOnRespawn()) {
            Optional<WorldProperties> oworld = sc.getWorld();
            if (oworld.isPresent()) {
                world = Sponge.getServer().getWorld(oworld.get().getUniqueId()).orElse(world);
            }
        }

        try {
            Location<World> spawn = world.getSpawnLocation().add(0.5, 0, 0.5);
            Transform<World> to = new Transform<>(spawn);

            // Compare current transform to spawn.
            wcl.getWorld(world).ifPresent(x -> x.getSpawnRotation().ifPresent(to::setRotation));

            event.setToTransform(to);
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }
    }
}
