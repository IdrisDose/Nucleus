/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import com.google.common.base.Preconditions;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class TwoPlayersArgument extends CommandElement {

    private final Text key;
    private final Text key2;
    private final CommandPermissionHandler handler;

    public TwoPlayersArgument(@Nullable Text key, Text key2, CommandPermissionHandler handler) {
        super(key);
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(key2);
        this.key = key;
        this.key2 = key2;
        this.handler = handler;
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        String sp1 = args.next();
        Optional<String> osp2 = args.nextIfPresent();
        if (!osp2.isPresent()) {
            throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.twoplayer.notenough"));
        }

        String sp2 = osp2.get();

        if (sp1.startsWith("@")) {
            context.putArg(key.toPlain(),
                SelectorWrapperArgument.parseValue(source, args, handler, SelectorWrapperArgument.SINGLE_PLAYER_SELECTORS, sp1));
        } else {
            context.putArg(key.toPlain(),
                getPlayerFromPartialName(sp1).orElseThrow(() -> args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.twoplayer.noexist", sp1))));
        }

        if (sp2.startsWith("@")) {
            context.putArg(key2.toPlain(),
                SelectorWrapperArgument.parseValue(source, args, handler, SelectorWrapperArgument.SINGLE_PLAYER_SELECTORS, sp2));
        } else {
            context.putArg(key2.toPlain(),
                getPlayerFromPartialName(sp2).orElseThrow(() -> args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.twoplayer.noexist", sp1))));
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        try {
            String s = args.peek().toLowerCase();
            return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().startsWith(s.toLowerCase())).map(User::getName)
                    .collect(Collectors.toList());
        } catch (ArgumentParseException e) {
            return Sponge.getServer().getOnlinePlayers().stream().map(User::getName).collect(Collectors.toList());
        }
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of("(<player to teleport> <target>)");
    }

    private Optional<Player> getPlayerFromPartialName(String name) {
        return Sponge.getServer().getOnlinePlayers().stream().filter(x -> x.getName().startsWith(name.toLowerCase()))
                .sorted().findFirst();
    }
}
