/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.config;

import com.google.common.collect.ImmutableMap;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Map;

@ConfigSerializable
public class ChatConfig {

    @Setting(value = "modify-chat", comment = "loc:config.chat.modify")
    private boolean modifychat = true;

    @Setting(value = "use-group-templates", comment = "loc:config.chat.useGroupTemplates")
    private boolean useGroupTemplates = true;

    @Setting(value = "templates")
    private TemplateConfig templates = new TemplateConfig();

    @Setting(value = "modify-main-message", comment = "loc:config.chat.main")
    private boolean modifyMainMessage = true;

    @Setting(value = "overwrite-early-prefixes", comment = "loc:config.chat.includeprefix")
    private boolean overwriteEarlyPrefixes = false;

    @Setting(value = "overwrite-early-suffixes", comment = "loc:config.chat.includesuffix")
    private boolean overwriteEarlySuffixes = false;

    public boolean isModifychat() {
        return modifychat;
    }

    public boolean isUseGroupTemplates() {
        return useGroupTemplates;
    }

    public ChatTemplateConfig getDefaultTemplate() {
        return templates.getDefaultTemplate();
    }

    public Map<String, WeightedChatTemplateConfig> getGroupTemplates() {
        return ImmutableMap.copyOf(templates.getGroupTemplates());
    }

    public boolean isModifyMainMessage() {
        return modifyMainMessage;
    }

    public boolean isOverwriteEarlyPrefixes() {
        return overwriteEarlyPrefixes;
    }

    public boolean isOverwriteEarlySuffixes() {
        return overwriteEarlySuffixes;
    }
}
