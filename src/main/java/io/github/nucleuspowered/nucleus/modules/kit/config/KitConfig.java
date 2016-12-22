/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.kit.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class KitConfig {

    @Setting(value = "must-get-all-items", comment = "loc:config.kits.mustgetall")
    private boolean mustGetAll = false;

    @Setting(value = "drop-items-if-inventory-full", comment = "loc:config.kits.full")
    private boolean dropKitIfFull = false;

    @Setting(value = "separate-permissions", comment = "loc:config.kits.separate")
    private boolean separatePermissions = false;

    public boolean isMustGetAll() {
        return mustGetAll;
    }

    public boolean isSeparatePermissions() {
        return separatePermissions;
    }

    public boolean isDropKitIfFull() {
        return dropKitIfFull;
    }
}
