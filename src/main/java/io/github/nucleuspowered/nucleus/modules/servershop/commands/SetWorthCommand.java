/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.servershop.commands;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.Nucleus;
import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.ItemAliasArgument;
import io.github.nucleuspowered.nucleus.configurate.datatypes.ItemDataNode;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCooldown;
import io.github.nucleuspowered.nucleus.internal.annotations.NoCost;
import io.github.nucleuspowered.nucleus.internal.annotations.NoWarmup;
import io.github.nucleuspowered.nucleus.internal.annotations.Permissions;
import io.github.nucleuspowered.nucleus.internal.annotations.RegisterCommand;
import io.github.nucleuspowered.nucleus.internal.annotations.RunAsync;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Sets or unsets an item's worth for either buying or selling.
 */
@RunAsync
@NoCost
@NoCooldown
@NoWarmup
@Permissions
@RegisterCommand({"setworth", "setitemworth"})
public class SetWorthCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<CommandSource> {

    private final String item = "item";
    private final String type = "type";
    private final String cost = "cost";
    @Inject private ItemDataService itemDataService;
    @Inject private EconHelper econHelper;

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.optionalWeak(new ItemAliasArgument(Text.of(item), itemDataService)),
            GenericArguments.choices(Text.of(type), ImmutableMap.of("buy", Type.BUY, "sell", Type.SELL)),
            GenericArguments.doubleNum(Text.of(cost))
        };
    }

    @Override
    public CommandResult executeCommand(CommandSource src, CommandContext args) throws Exception {
        String id = getCatalogTypeFromHandOrArgs(src, item, args).getId();
        Type transactionType = args.<Type>getOne(type).get();
        double newCost = args.<Double>getOne(cost).get();
        if (newCost < 0) {
            newCost = -1;
        }

        // Get the item from the system.
        ItemDataNode node = itemDataService.getDataForItem(id);

        // Get the current item worth.
        double currentWorth = transactionType.getter.apply(node);
        String worth;
        String newWorth;

        if (econHelper.economyServiceExists()) {
            worth = econHelper.getCurrencySymbol(currentWorth);
            newWorth = econHelper.getCurrencySymbol(newCost);
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.setworth.noeconservice"));
            worth = String.valueOf(currentWorth);
            newWorth = String.valueOf(newCost);
        }

        String name;
        Optional<CatalogType> type = Util.getCatalogTypeForItemFromId(id);
        name = type.map(Util::getTranslatableIfPresentOnCatalogType).orElse(id);

        if (currentWorth == newCost) {
            if (currentWorth < 0) {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.setworth.alreadyunavailable", name, transactionType.getTranslation()));
            } else {
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.setworth.samecost", transactionType.getTranslation(), name, worth));
            }

            return CommandResult.empty();
        }

        // Set the item worth.
        transactionType.setter.accept(node, newCost);
        itemDataService.setDataForItem(id, node);

        // Tell the user.
        if (currentWorth == -1) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.setworth.success.new", name, transactionType.getTranslation(), newWorth));
        } else if (newCost == -1) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.setworth.success.removed", name, transactionType.getTranslation(), worth));
        } else {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.setworth.success.changed", name, transactionType.getTranslation(), worth, newWorth));
        }

        return CommandResult.success();
    }

    private enum Type {
        BUY(ItemDataNode::getServerBuyPrice, ItemDataNode::setServerBuyPrice, "standard.buyfrom"),
        SELL(ItemDataNode::getServerSellPrice, ItemDataNode::setServerSellPrice, "standard.sellto");

        private final Function<ItemDataNode, Double> getter;
        private final BiConsumer<ItemDataNode, Double> setter;
        private final String transactionKey;

        Type(Function<ItemDataNode, Double> get, BiConsumer<ItemDataNode, Double> set, String transactionKey) {
            this.getter = get;
            this.setter = set;
            this.transactionKey = transactionKey;
        }

        private String getTranslation() {
            return Nucleus.getNucleus().getMessageProvider().getMessageWithFormat(transactionKey);
        }
    }
}
