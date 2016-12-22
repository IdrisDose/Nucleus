/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.info.config;

import io.github.nucleuspowered.nucleus.Nucleus;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MotdConfig {

    @Setting(value = "show-motd-on-join", comment = "loc:config.motd.onjoin")
    private boolean showMotdOnJoin = true;

    @Setting(value = "motd-login-delay", comment = "loc:config.motd.delay")
    private float delay = 0.5f;

    @Setting(value = "motd-title", comment = "loc:config.motd.title")
    private String motdTitle = Nucleus.getNucleus().getMessageProvider().getMessageWithFormat("motd.title");

    @Setting(value = "use-pagination", comment = "loc:config.motd.pagination")
    private boolean usePagination = true;

    public boolean isShowMotdOnJoin() {
        return showMotdOnJoin;
    }

    public String getMotdTitle() {
        return motdTitle;
    }

    public boolean isUsePagination() {
        return usePagination;
    }

    public float getDelay() {
        return delay;
    }
}
