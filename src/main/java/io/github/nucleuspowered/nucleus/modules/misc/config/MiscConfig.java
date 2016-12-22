/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MiscConfig {

    @Setting(value = "max-speed", comment = "loc:config.misc.speed.max")
    private int maxSpeed = 5;

    @Setting(value = "require-god-permission-on-login", comment = "loc:config.god.permissiononlogin")
    private boolean godPermissionOnLogin = false;

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public boolean isGodPermissionOnLogin() {
        return godPermissionOnLogin;
    }
}
