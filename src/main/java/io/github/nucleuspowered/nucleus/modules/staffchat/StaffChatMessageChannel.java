/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.staffchat;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.ChatUtil;
import io.github.nucleuspowered.nucleus.NucleusPlugin;
import io.github.nucleuspowered.nucleus.modules.staffchat.commands.StaffChatCommand;
import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfig;
import io.github.nucleuspowered.nucleus.modules.staffchat.config.StaffChatConfigAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.drnaylor.quickstart.exceptions.IncorrectAdapterTypeException;
import uk.co.drnaylor.quickstart.exceptions.NoModuleException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StaffChatMessageChannel implements MessageChannel {

    static StaffChatMessageChannel INSTANCE = null;

    public static StaffChatMessageChannel getInstance() {
        Preconditions.checkState(INSTANCE != null, "StaffChatMessageChannel#Instance");
        return INSTANCE;
    }

    private final NucleusPlugin plugin;
    private final ChatUtil chatUtil;
    private StaffChatConfigAdapter scca;
    private String basePerm;

    StaffChatMessageChannel(NucleusPlugin plugin) {
        this.plugin = plugin;
        chatUtil = plugin.getChatUtil();
    }

    // No injections, we have to do it the hard way!
    private StaffChatConfig getConfig() {
        if (scca == null) {
            try {
                scca = plugin.getModuleContainer().getConfigAdapterForModule(StaffChatModule.moduleID, StaffChatConfigAdapter.class);
            } catch (NoModuleException | IncorrectAdapterTypeException e) {
                e.printStackTrace();
                return new StaffChatConfig();
            }
        }

        return scca.getNodeOrDefault();
    }

    @Override
    @Nonnull
    public Optional<Text> transformMessage(@Nullable Object sender, MessageReceiver recipient, Text original, ChatType type) {
        if (!(sender instanceof Player)) {
            sender = Sponge.getServer().getConsole();
        }

        StaffChatConfig c = getConfig();
        Text prefix = chatUtil.getMessageFromTemplate(c.getMessageTemplate(), (CommandSource)sender, false);
        return Optional.of(Text.of(prefix, TextSerializers.FORMATTING_CODE.deserialize(String.format("&%s%s", c.getMessageColour(), original.toPlain()))));
    }

    @Override
    @Nonnull
    public Collection<MessageReceiver> getMembers() {
        List<MessageReceiver> c = Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.hasPermission(getPermission())).collect(Collectors.toList());
        c.add(Sponge.getServer().getConsole());
        return c;
    }

    private String getPermission() {
        if (basePerm == null) {
            basePerm = plugin.getPermissionRegistry().getService(StaffChatCommand.class).getBase();
        }

        return basePerm;
    }
}
