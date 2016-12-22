/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.misc.listeners;

import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.ListenerBase;
import io.github.nucleuspowered.nucleus.internal.annotations.ConditionalListener;
import io.github.nucleuspowered.nucleus.modules.misc.MiscModule;
import io.github.nucleuspowered.nucleus.modules.misc.commands.GodCommand;
import io.github.nucleuspowered.nucleus.modules.misc.config.MiscConfigAdapter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;

import java.util.function.Predicate;

@ConditionalListener(InvulnReloadableListener.Condition.class)
public class InvulnReloadableListener extends ListenerBase {

    private String basePerm = null;

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join joinEvent, @Getter("getTargetEntity") Player player) {
        if (basePerm == null) {
            basePerm = plugin.getPermissionRegistry().getService(GodCommand.class).getBase();
        }

        if (!player.hasPermission(basePerm)) {
            plugin.getUserDataManager().get(player).ifPresent(x -> x.setInvulnerable(false));
        }
    }

    public static class Condition implements Predicate<Nucleus> {

        @Override public boolean test(Nucleus nucleus) {
            try {
                return nucleus.getModuleContainer().getConfigAdapterForModule(MiscModule.ID, MiscConfigAdapter.class).getNodeOrDefault().isGodPermissionOnLogin();
            } catch (NoModuleException | IncorrectAdapterTypeException e) {
                if (nucleus.isDebugMode()) {
                    e.printStackTrace();
                }

                return false;
            }
        }
    }
}
