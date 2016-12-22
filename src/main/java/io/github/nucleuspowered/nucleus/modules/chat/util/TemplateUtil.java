/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.chat.util;

import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfig;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.chat.config.ChatTemplateConfig;
import io.github.nucleuspowered.nucleus.modules.chat.config.WeightedChatTemplateConfig;
import org.spongepowered.api.service.permission.Subject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contains the logic for caching templates and the template selection logic.
 */
public class TemplateUtil {

    private List<Map<String, WeightedChatTemplateConfig>> cachedTemplates = null;
    private final ChatConfigAdapter chatConfigAdapter;

    public TemplateUtil(NucleusPlugin plugin, ChatConfigAdapter chatConfigAdapter) {
        plugin.registerReloadable(() -> cachedTemplates = null);
        this.chatConfigAdapter = chatConfigAdapter;
    }

    public ChatTemplateConfig getTemplate(Subject subject) {
        ChatConfig cc = chatConfigAdapter.getNodeOrDefault();
        List<Subject> groups;
        try {
            groups = Util.getParentSubjects(subject);
        } catch (Exception e) {
            return cc.getDefaultTemplate();
        }

        if (groups == null || groups.isEmpty()) {
            return cc.getDefaultTemplate();
        }

        if (cachedTemplates == null) {
            cachedTemplates = cc.getGroupTemplates()
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(x -> x.getValue().getWeight(), Collectors.toSet()))
                .entrySet()
                .stream()
                // Reverse order.
                .sorted((first, second) -> second.getKey().compareTo(first.getKey()))
                .map(x -> x.getValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());
        }

        // For each weight...
        for (Map<String, WeightedChatTemplateConfig> templates : cachedTemplates) {
            // Iterate through all groups the player is in.
            for (Subject group : groups) {
                if (templates.containsKey(group.getIdentifier())) {
                    return templates.get(group.getIdentifier());
                }
            }
        }

        // Return the default.
        return cc.getDefaultTemplate();
    }

}
