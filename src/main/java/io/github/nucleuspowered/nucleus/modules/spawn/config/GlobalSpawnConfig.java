/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.spawn.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Optional;

@ConfigSerializable
public class GlobalSpawnConfig {

    @Setting(value = "on-respawn", comment = "loc:config.spawn.global.onrespawn")
    private boolean onRespawn = false;

    @Setting(value = "on-spawn-command", comment = "loc:config.spawn.global.oncommand")
    private boolean onSpawnCommand = false;

    @Setting(value = "on-login", comment = "loc:config.spawn.global.onlogin")
    private boolean onLogin = false;

    @Setting(value = "target-spawn-world", comment = "loc:config.spawn.global.target")
    private String spawnWorld = "world";

    public boolean isOnRespawn() {
        return onRespawn;
    }

    public boolean isOnSpawnCommand() {
        return onSpawnCommand;
    }

    public boolean isOnLogin() {
        return onLogin;
    }

    public Optional<WorldProperties> getWorld() {
        Optional<WorldProperties> ow = Sponge.getServer().getWorldProperties(spawnWorld);
        if (!ow.isPresent()) {
            ow = Sponge.getServer().getWorldProperties(Sponge.getServer().getDefaultWorldName());
        }

        return ow;
    }
}
