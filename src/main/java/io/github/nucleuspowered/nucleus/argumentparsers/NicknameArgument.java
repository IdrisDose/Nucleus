/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.argumentparsers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.dataservices.UserService;
import io.github.nucleuspowered.nucleus.dataservices.loaders.UserDataManager;
import io.github.nucleuspowered.nucleus.util.ThrownBiFunction;
import io.github.nucleuspowered.nucleus.util.TriFunction;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class NicknameArgument extends CommandElement {

    private final UserDataManager userDataManager;
    private final ThrownBiFunction<String, CommandArgs, List<?>, ArgumentParseException> parser;
    private final TriFunction<String, CommandArgs, CommandContext, List<String>> completer;
    private final boolean onlyOne;
    private final UnderlyingType type;

    public NicknameArgument(@Nullable Text key, @Nonnull UserDataManager userDataManager, UnderlyingType type) {
        this(key, userDataManager, type, true);
    }

    public NicknameArgument(@Nullable Text key, @Nonnull UserDataManager userDataManager, UnderlyingType type, boolean onlyOne) {
        super(key);

        Preconditions.checkNotNull(userDataManager);
        this.onlyOne = onlyOne;
        this.userDataManager = userDataManager;
        this.type = type;

        if (type == UnderlyingType.USER) {
            parser = (s, a) -> {
                try {
                    UserStorageService uss = Sponge.getServiceManager().provideUnchecked(UserStorageService.class);
                    List<User> users = uss.match(s).stream().map(uss::get).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
                    if (!users.isEmpty()) {
                        List<User> exactUser = users.stream().filter(x -> x.getName().equalsIgnoreCase(s)).collect(Collectors.toList());
                        if (exactUser.size() == 1) {
                            return exactUser;
                        }

                        if (users.size() > 1 && this.onlyOne) {
                            throw a.createError(Util.getTextMessageWithFormat("args.user.toomany", s));
                        }

                        return users;
                    }
                } catch (Exception e) {
                    // We want to rethrow this!
                    if (e instanceof ArgumentParseException) {
                        throw e;
                    }
                }

                return Lists.newArrayList();
            };

            completer = (s, a, c) -> Sponge.getServiceManager().provideUnchecked(UserStorageService.class).match(s).stream().filter(x -> x.getName().isPresent())
                    .map(x -> x.getName().get()).collect(Collectors.toList());
        } else {
            PlayerConsoleArgument pca = new PlayerConsoleArgument(key, type == UnderlyingType.PLAYER_CONSOLE);
            parser = pca::parseInternal;
            completer = pca::completeInternal;
        }
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String name = args.next().toLowerCase();
        return parseInternal(name, args);
    }

    Object parseInternal(String name, CommandArgs args) throws ArgumentParseException {
        boolean playerOnly = name.startsWith("p:");

        final String fName;
        if (playerOnly) {
            fName = name.split(":", 2)[1];
        } else {
            fName = name;
        }

        List<?> obj = parser.accept(fName, args);
        if (!obj.isEmpty()) {
            return obj;
        } else if (playerOnly) {
            // Rethrow;
            throw args.createError(Util.getTextMessageWithFormat("args.user.nouser", fName));
        }

        // Now check user names
        Map<String, UserService> allPlayers = userDataManager.getOnlineUsersInternal().stream()
                .filter(x -> x.getUser().isOnline() && x.getNicknameAsString().isPresent())
                .collect(Collectors.toMap(s -> TextSerializers.FORMATTING_CODE.stripCodes(s.getNicknameAsString().get().toLowerCase()), s -> s));
        if (allPlayers.containsKey(fName.toLowerCase())) {
            return allPlayers.get(fName.toLowerCase()).getUser();
        }

        List<User> players = allPlayers.entrySet().stream().filter(x -> x.getKey().toLowerCase().startsWith(fName))
                .sorted((x, y) -> x.getKey().compareTo(y.getKey()))
                .map(x -> x.getValue().getUser())
                .collect(Collectors.toList());

        if (players.isEmpty()) {
            throw args.createError(Util.getTextMessageWithFormat(type == UnderlyingType.PLAYER_CONSOLE ? "args.playerconsole.nouser" : "args.user.nouser", fName));
        } else if (players.size() > 1 && this.onlyOne) {
            throw args.createError(Util.getTextMessageWithFormat("args.user.toomany", fName));
        }

        // We know they are online.
        return players;
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        String name;
        try {
            name = args.peek().toLowerCase();
        } catch (ArgumentParseException e) {
            name = "";
        }

        boolean playerOnly = name.startsWith("p:");
        final String fName;
        if (playerOnly) {
            fName = name.split(":", 2)[1];
        } else {
            fName = name;
        }

        List<String> original = completer.accept(fName, args, context);
        if (playerOnly) {
            return original.stream().map(x -> "p:" + x).collect(Collectors.toList());
        } else {
            original.addAll(userDataManager.getOnlineUsersInternal().stream()
                    .filter(x -> x.getUser().isOnline() && x.getNicknameAsString().isPresent() &&
                            TextSerializers.FORMATTING_CODE.stripCodes(x.getNicknameAsString().get()).toLowerCase().startsWith(fName))
                    .map(x -> TextSerializers.FORMATTING_CODE.stripCodes(x.getNicknameAsString().get())).collect(Collectors.toList()));
            return original;
        }
    }

    public enum UnderlyingType {
        PLAYER,
        PLAYER_CONSOLE,
        USER
    }
}