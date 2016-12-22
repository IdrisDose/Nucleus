/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.commandspy.config;

import io.github.nucleuspowered.nucleus.configurate.annotations.ProcessSetting;
import io.github.nucleuspowered.nucleus.configurate.settingprocessor.RemoveFirstSlashIfExistsSettingProcessor;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class CommandSpyConfig {

    @Setting(comment = "loc:config.commandspy.template")
    private String prefix = "&7[CS: {{name}}]: ";

    @Setting(value = "use-whitelist", comment = "loc:config.commandspy.usewhitelist")
    private boolean useWhitelist = true;

    // Removes the first "/" if it exists.
    @ProcessSetting(RemoveFirstSlashIfExistsSettingProcessor.class)
    @Setting(value = "whitelisted-commands-to-spy-on")
    private List<String> commands = new ArrayList<>();

    public String getTemplate() {
        return prefix;
    }

    public List<String> getCommands() {
        return commands;
    }

    public boolean isUseWhitelist() {
        return useWhitelist;
    }
}
