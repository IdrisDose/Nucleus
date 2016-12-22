/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.servershop.commands;

import io.github.nucleuspowered.nucleus.Util;
import io.github.nucleuspowered.nucleus.argumentparsers.ItemAliasArgument;
import io.github.nucleuspowered.nucleus.argumentparsers.PositiveIntegerArgument;
import io.github.nucleuspowered.nucleus.configurate.datatypes.ItemDataNode;
import io.github.nucleuspowered.nucleus.dataservices.ItemDataService;
import io.github.nucleuspowered.nucleus.internal.EconHelper;
import io.github.nucleuspowered.nucleus.internal.annotations.*;
import io.github.nucleuspowered.nucleus.internal.permissions.SuggestedLevel;
import io.github.nucleuspowered.nucleus.modules.servershop.config.ServerShopConfigAdapter;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Consumer;

@RunAsync
@NoCost
@NoCooldown
@NoWarmup
@RequiresEconomy
@Permissions(suggestedLevel = SuggestedLevel.USER)
@RegisterCommand({"itembuy", "buy"})
public class BuyCommand extends io.github.nucleuspowered.nucleus.internal.command.AbstractCommand<Player> {

    @Inject private ServerShopConfigAdapter ssca;
    @Inject private ItemDataService itemDataService;
    @Inject private EconHelper econHelper;
    private final String itemKey = "item";
    private final String amountKey = "amount";

    @Override
    public CommandElement[] getArguments() {
        return new CommandElement[] {
            GenericArguments.flags().flag("y", "f", "a", "-yes", "-auto").buildWith(
                GenericArguments.seq(
                    GenericArguments.onlyOne(new ItemAliasArgument(Text.of(itemKey), itemDataService)),
                    GenericArguments.onlyOne(new PositiveIntegerArgument(Text.of(amountKey)))))
        };
    }

    @Override
    public CommandResult executeCommand(final Player src, CommandContext args) throws Exception {
        CatalogType ct = args.<CatalogType>getOne(itemKey).get();
        int amount = args.<Integer>getOne(amountKey).get();

        ItemDataNode node = itemDataService.getDataForItem(ct.getId());
        if (node.getServerBuyPrice() < 0) {
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.notforsale"));
            return CommandResult.empty();
        }

        int max = ssca.getNodeOrDefault().getMaxPurchasableAtOnce();
        if (amount > max) {
            amount = max;
            src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.maximum", String.valueOf(amount)));
        }

        final ItemStack created;
        if (ct instanceof ItemType) {
            created = ItemStack.of((ItemType) ct, amount);
        } else {
            created = ItemStack.builder().fromBlockState((BlockState)ct).quantity(amount).build();
        }

        // Get the cost.
        final double perUnitCost = node.getServerBuyPrice();
        final int unitCount = amount;
        final double overallCost = perUnitCost * unitCount;
        src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.summary", String.valueOf(amount), created.getTranslation().get(), econHelper.getCurrencySymbol(overallCost)));

        if (args.hasAny("y")) {
            new BuyCallback(src, overallCost, created, unitCount, perUnitCost).accept(src);
        } else {
            src.sendMessage(
                    plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.clickhere").toBuilder()
                            .onClick(TextActions.executeCallback(new BuyCallback(src, overallCost, created, unitCount, perUnitCost))).build()
            );
        }

        return CommandResult.success();
    }

    private class BuyCallback implements Consumer<CommandSource> {

        private final Player src;
        private final double overallCost;
        private final ItemStack created;
        private final int unitCount;
        private final double perUnitCost;

        private boolean hasRun = false;

        private BuyCallback(Player src, double overallCost, ItemStack created, int unitCount, double perUnitCost) {
            this.src = src;
            this.overallCost = overallCost;
            this.created = created;
            this.unitCount = unitCount;
            this.perUnitCost = perUnitCost;
        }

        @Override
        public void accept(CommandSource source) {
            if (hasRun) {
                return;
            }

            hasRun = true;

            // Get the money, transact, return the money on fail.
            Inventory target = Util.getStandardInventory(src);
            if (econHelper.withdrawFromPlayer(src, overallCost, false)) {
                InventoryTransactionResult itr = target.offer(created);
                if (itr.getRejectedItems().isEmpty()) {
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.transactionsuccess",
                            String.valueOf(unitCount), created.getTranslation().get(), econHelper.getCurrencySymbol(overallCost)));
                } else {
                    Collection<ItemStackSnapshot> iss = itr.getRejectedItems();
                    int rejected = iss.stream().mapToInt(ItemStackSnapshot::getCount).sum();
                    double refund = rejected * perUnitCost;
                    econHelper.depositInPlayer(src, refund, false);
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.transactionpartial", String.valueOf(rejected), created.getTranslation().get()));
                    src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.transactionsuccess",
                            String.valueOf(unitCount - rejected), created.getTranslation().get(), econHelper.getCurrencySymbol(overallCost - refund)));
                }
            } else {
                // No funds.
                src.sendMessage(plugin.getMessageProvider().getTextMessageWithFormat("command.itembuy.nofunds"));
            }
        }
    }
}
