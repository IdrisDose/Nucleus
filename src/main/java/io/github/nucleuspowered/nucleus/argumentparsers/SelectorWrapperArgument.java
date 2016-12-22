/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.argumentparsers.selectors.AllPlayers;
import io.github.nucleuspowered.nucleus.argumentparsers.selectors.AllPlayersFromWorld;
import io.github.nucleuspowered.nucleus.argumentparsers.selectors.NearestPlayer;
import io.github.nucleuspowered.nucleus.argumentparsers.selectors.NearestPlayerFromSpecifiedLocation;
import io.github.nucleuspowered.nucleus.argumentparsers.selectors.RandomPlayer;
import io.github.nucleuspowered.nucleus.argumentparsers.selectors.RandomPlayerFromWorld;
import io.github.nucleuspowered.nucleus.internal.CommandPermissionHandler;
import io.github.nucleuspowered.nucleus.internal.interfaces.SelectorParser;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SelectorWrapperArgument extends CommandElement {

    public final static Collection<SelectorParser<?>> SINGLE_PLAYER_SELECTORS;

    public final static Collection<SelectorParser<?>> MULTIPLE_PLAYER_SELECTORS;

    public final static Collection<SelectorParser<?>> ALL_SELECTORS;

    static {
        SINGLE_PLAYER_SELECTORS = ImmutableList.of(
                NearestPlayer.INSTANCE, NearestPlayerFromSpecifiedLocation.INSTANCE, RandomPlayer.INSTANCE, RandomPlayerFromWorld.INSTANCE
        );

        MULTIPLE_PLAYER_SELECTORS = ImmutableList.of(
                AllPlayers.INSTANCE, AllPlayersFromWorld.INSTANCE
        );

        ALL_SELECTORS = ImmutableList.<SelectorParser<?>>builder()
                .addAll(SINGLE_PLAYER_SELECTORS)
                .addAll(MULTIPLE_PLAYER_SELECTORS)
                .build();
    }

    public static Object parseValue(CommandSource source, CommandArgs args, CommandPermissionHandler handler, Collection<SelectorParser<?>> selectors,
        String selectorRaw) throws ArgumentParseException {

        Preconditions.checkArgument(selectorRaw.startsWith("@"));

        if (!handler.testSelectors(source)) {
            throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.selector.nopermissions"));
        }

        String selector = selectorRaw.substring(1).toLowerCase();
        Optional<SelectorParser<?>> parserOptional = selectors.stream().filter(x -> x.selector().matcher(selector).matches()).findFirst();
        if (parserOptional.isPresent()) {
            return parserOptional.get().get(selector, source, args);
        }

        throw args.createError(Nucleus.getNucleus().getMessageProvider().getTextMessageWithFormat("args.selector.noexist", selectorRaw));
    }

    private final CommandElement wrappedElement;
    private final CommandPermissionHandler permissionHandler;
    private final Collection<SelectorParser<?>> selectors;

    public SelectorWrapperArgument(@Nonnull CommandElement wrappedElement, CommandPermissionHandler permissionHandler, SelectorParser<?>... selectors) {
        this(wrappedElement, permissionHandler, Lists.newArrayList(selectors));
    }

    public SelectorWrapperArgument(@Nonnull CommandElement wrappedElement, CommandPermissionHandler permissionHandler, Collection<SelectorParser<?>> selectors) {
        super(wrappedElement.getKey());

        Preconditions.checkArgument(selectors != null && !selectors.isEmpty());
        this.wrappedElement = wrappedElement;
        this.selectors = selectors;
        this.permissionHandler = permissionHandler;
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return parseValue(source, args, permissionHandler, this.selectors, args.next());
    }

    @Override
    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        if (args.peek().startsWith("@")) {
            // MC names cannot have "@" in them, so there is no need to send it to the wrapped argument anyway.
            super.parse(source, args, context);
        } else {
            wrappedElement.parse(source, args, context);
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return wrappedElement.complete(src, args, context);
    }

    @Override
    public Text getUsage(CommandSource src) {
        return wrappedElement.getUsage(src);
    }
}
