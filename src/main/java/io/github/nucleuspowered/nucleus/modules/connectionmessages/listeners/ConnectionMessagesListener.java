/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.connectionmessages.listeners;

import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.PermissionRegistry;
import io.github.nucleuspowered.nucleus.internal.permissions.PermissionInformation;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfig;
import io.github.nucleuspowered.nucleus.modules.connectionmessages.config.ConnectionMessagesConfigAdapter;
import io.github.nucleuspowered.nucleus.modules.core.config.CoreConfigAdapter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.HashMap;
import java.util.Map;

public class ConnectionMessagesListener extends ListenerBase {

    @Inject private ChatUtil chatUtil;
    @Inject private CoreConfigAdapter cca;
    @Inject private ConnectionMessagesConfigAdapter cma;
    @Inject private UserDataManager loader;
    private final String disablePermission = PermissionRegistry.PERMISSIONS_PREFIX + "connectionmessages.disable";

    @Override public Map<String, PermissionInformation> getPermissions() {
        return new HashMap<String, PermissionInformation>() {{
            put(disablePermission, new PermissionInformation(
                plugin.getMessageProvider().getMessageWithFormat("permission.connectionmesssages.disable"),
                SuggestedLevel.NONE));
        }};
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join joinEvent, @Getter("getTargetEntity") Player pl) {
        ConnectionMessagesConfig cmc = cma.getNodeOrDefault();
        if (cmc.isDisableWithPermission() && pl.hasPermission(this.disablePermission)) {
            joinEvent.setMessageCancelled(true);
            return;
        }

        try {
            if (loader.getUser(pl).get().isFirstPlay()) {
                // First time player.
                if (cmc.isShowFirstTimeMessage() && !cmc.getFirstTimeMessage().isEmpty()) {
                    MessageChannel.TO_ALL.send(plugin, chatUtil.getMessageFromTemplate(cmc.getFirstTimeMessage(), pl, true));
                }
            }
        } catch (Exception e) {
            if (cca.getNodeOrDefault().isDebugmode()) {
                e.printStackTrace();
            }
        }

        if (cmc.isModifyLoginMessage()) {
            if (cmc.getLoginMessage().isEmpty()) {
                joinEvent.setMessageCancelled(true);
            } else {
                joinEvent.setMessage(chatUtil.getMessageFromTemplate(cma.getNodeOrDefault().getLoginMessage(), pl, true));
            }
        }
    }

    @Listener
    public void onPlayerQuit(ClientConnectionEvent.Disconnect leaveEvent, @Getter("getTargetEntity") Player pl) {
        ConnectionMessagesConfig cmc = cma.getNodeOrDefault();
        if (cmc.isDisableWithPermission() && pl.hasPermission(this.disablePermission)) {
            leaveEvent.setMessageCancelled(true);
            return;
        }

        if (cmc.isModifyLogoutMessage()) {
            if (cmc.getLogoutMessage().isEmpty()) {
                leaveEvent.setMessageCancelled(true);
            } else {
                leaveEvent.setMessage(chatUtil.getMessageFromTemplate(cma.getNodeOrDefault().getLogoutMessage(), pl, true));
            }
        }
    }
}
