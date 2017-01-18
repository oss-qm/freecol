/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.Comparator;
import java.util.List;

import net.sf.freecol.common.model.GoodsType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * Represents a certain amount of a GoodsType.  This does not
 * correspond to actual cargo present in a Location, but is intended
 * to represent things such as the amount of Lumber necessary to build
 * something, or the amount of cargo to load at a certain Location.
 */
public class AbstractGoods extends FreeColObject implements Named {

    /** Compare the amount of abstract goods. */
    public static final Comparator<AbstractGoods> ascendingAmountComparator
        = Comparator.comparingInt(AbstractGoods::getAmount)
            .thenComparing(AbstractGoods::getType,
                           GoodsType.goodsTypeComparator);

    /**
     * A comparator to sort by descending goods amount and then by a
     * predictable goods type order.
     */
    public static final Comparator<AbstractGoods> descendingAmountComparator
        = Comparator.comparingInt(AbstractGoods::getAmount).reversed()
            .thenComparing(AbstractGoods::getType,
                           GoodsType.goodsTypeComparator);

    /** The type of goods. */
    protected GoodsType type;

    /** The amount of goods. */
    protected int amount;


    /**
     * Empty constructor.
     */
    public AbstractGoods() {}

    /**
     * Creates a new {@code AbstractGoods} instance.
     *
     * @param type The {@code GoodsType} to create.
     * @param amount The amount of goods to create.
     */
    public AbstractGoods(GoodsType type, int amount) {
        setId(type.getId());
        this.type = type;
        this.amount = amount;
    }

    /**
     * Creates a new {@code AbstractGoods} instance.
     *
     * @param other Another {@code AbstractGoods} to copy.
     */
    public AbstractGoods(AbstractGoods other) {
        setId(other.type.getId());
        this.type = other.type;
        this.amount = other.amount;
    }


    /**
     * Get the goods type.
     *
     * @return The {@code GoodsType}.
     */
    public final GoodsType getType() {
        return type;
    }

    /**
     * Set the goods type.
     *
     * @param newType The new {@code GoodsType}.
     */
    public final void setType(final GoodsType newType) {
        this.type = newType;
    }

    /**
     * Is the goods type a food type?
     *
     * @return True if this is food.
     */
    public final boolean isFoodType() {
        return getType().isFoodType();
    }

    /**
     * Get the goods amount.
     *
     * @return The goods amount.
     */
    public final int getAmount() {
        return amount;
    }

    /**
     * Set the goods amount.
     *
     * @param newAmount The new goods amount.
     */
    public final void setAmount(final int newAmount) {
        this.amount = newAmount;
    }

    /**
     * Is the amount positive?
     *
     * @return True if the amount is greater than zero.
     */
    public final boolean isPositive() {
        return getAmount() > 0;
    }

    /**
     * Get a label for these goods.
     *
     * @return The label for these goods.
     */
    public StringTemplate getLabel() {
        return getLabel(getType(), getAmount());
    }

    /**
     * Are these goods storable.
     *
     * @return True if the goods are storable.
     */
    public boolean isStorable() {
        return getType().isStorable();
    }

    /**
     * Get a label for these goods.
     *
     * @param sellable Whether these goods can be sold.
     * @return A label for these goods.
     */
    public StringTemplate getLabel(boolean sellable) {
        return (sellable) ? getLabel()
            : StringTemplate.template("model.abstractGoods.boycotted")
                .addNamed("%goods%", getType())
                .addAmount("%amount%", getAmount());
    }

    /**
     * Get a label given a goods type and amount.
     *
     * @param type The {@code GoodsType} to display.
     * @param amount The amount of goods.
     * @return The goods label.
     */
    public static StringTemplate getLabel(GoodsType type, int amount) {
        return StringTemplate.template("model.abstractGoods.label")
            .addNamed("%goods%", type)
            .addAmount("%amount%", amount);
    }

    /**
     * Get a label given a key and amount.
     *
     * @param key A key for the goods to display.
     * @param amount The amount of goods.
     * @return The goods label.
     */
    public static StringTemplate getLabel(String key, int amount) {
        return StringTemplate.template("model.abstractGoods.label")
            .add("%goods%", key)
            .addAmount("%amount%", amount);
    }

    /**
     * Convenience lookup of the goods count in a collection of
     * abstract goods given a goods type.
     *
     * @param type The {@code GoodsType} to look for.
     * @param goods The collection of {@code AbstractGoods} to look in.
     * @return The goods count found, or zero if not found.
     */
    public static int getCount(GoodsType type,
        List<? extends AbstractGoods> goods) {
        AbstractGoods ag = AbstractGoods.findByType(goods, type);
        return (ag == null) ? 0 : ag.getAmount();
    }

    /**
     * Evaluate goods for trade purposes.
     *
     * @param player The {@code Player} to evaluate for.
     * @return A value for the goods.
     */
    public int evaluateFor(Player player) {
        final Market market = player.getMarket();
        return (market == null) ? getAmount() * 2 // FIXME: magic#
            : market.getSalePrice(getType(), getAmount());
    }

    /**
     * Check whether it is of the given {@link GoodsType}
     *
     * @param gt  The {@link GoodsType} to match against
     * @return    True if matching
     */
    public final boolean isType(GoodsType gt) {
        if ((gt == null) || (type == null))
            return false;

        return (type == gt || type.equals(gt));
    }

    /**
     * Check whether any in the list is of given {@link GoodsType}
     */
    public static boolean anyIsType(List<AbstractGoods> l, GoodsType gt) {
        if (l != null)
            for (AbstractGoods ag : l)
                if (ag.isType(gt))
                    return true;

        return false;
    }

    /**
     * find any in list with positive amount
     */
    public static AbstractGoods findPositive(List<AbstractGoods> l) {
        if (l != null)
            for (AbstractGoods ag : l)
                if (ag.amount > 0)
                    return ag;

        return null;
    }

    /**
     * find any in list by type
     */
    public static AbstractGoods findByType(List<? extends AbstractGoods> l, GoodsType gt) {
        if (l != null)
            for (AbstractGoods ag : l)
                if (ag.getType() == gt)
                    return ag;

        return null;
    }

    public static AbstractGoods findByType(List<AbstractGoods> l, AbstractGoods ag) {
        return findByType(l, ag.getType());
    }

    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return getType().getNameKey();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() {
        return null; // AbstractGoods are never serialized directly
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AbstractGoods) {
            AbstractGoods ag = (AbstractGoods)o;
            return type == ag.type && amount == ag.amount;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + Utils.hashCode(this.type);
        hash = 31 * hash + this.amount;
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return AbstractGoods.toString(this);
    }

    /**
     * Simple string version of some goods.
     *
     * @param ag The {@code AbstractGoods} to make a string from.
     * @return A string version of the goods.
     */
    public static String toString(AbstractGoods ag) {
        return toString(ag.getType(), ag.getAmount());
    }

    /**
     * Simple string version of the component parts of some goods.
     *
     * @param goodsType The {@code GoodsType} to use.
     * @param amount The amount of goods.
     * @return A string version of the goods.
     */
    public static String toString(GoodsType goodsType, int amount) {
        return amount + " "
            + ((goodsType == null) ? "(null)" : goodsType.getSuffix());
    }
}
