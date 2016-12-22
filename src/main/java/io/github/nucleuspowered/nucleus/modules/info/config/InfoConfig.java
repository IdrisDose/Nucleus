/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class InfoConfig {

    @Setting(value = "motd")
    private MotdConfig motdConfig = new MotdConfig();

    @Setting("info")
    private InfoFileConfig infoFileConfig = new InfoFileConfig();

    public boolean isShowMotdOnJoin() {
        return motdConfig.isShowMotdOnJoin();
    }

    public String getMotdTitle() {
        return motdConfig.getMotdTitle();
    }

    public boolean isMotdUsePagination() {
        return motdConfig.isUsePagination();
    }

    public boolean isUseDefaultFile() {
        return infoFileConfig.isUseDefaultFile();
    }

    public String getDefaultInfoSection() {
        return infoFileConfig.getDefaultInfoSection();
    }

    public float getMotdDelay() {
        return Math.max(0f, motdConfig.getDelay());
    }
}
