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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Contains goods and can be used by a {@link Location} to make certain
 * tasks easier.
 */
public class GoodsContainer extends FreeColGameObject implements Ownable {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GoodsContainer.class.getName());

    public static final String TAG = "goodsContainer";

    /** The size of a standard `hold' of data. */
    public static final int CARGO_SIZE = 100;

    /**
     * Value to use for apparent unlimited quantities of goods
     * (e.g. warehouse contents in Europe, amount of food a colony can
     * import).  Has to be signficantly bigger than any one unit could
     * expect to carry, but not so huge as to look silly in user
     * messages.
     */
    public static final int HUGE_CARGO_SIZE = 100 * CARGO_SIZE;

    /**
     * The list of Goods stored in this {@code GoodsContainer}.
     *
     * Always accessed synchronized (except I/O).
     */
    private final Map<GoodsType, Integer> storedGoods = new HashMap<>();

    /**
     * The previous list of Goods stored in this
     * {@code GoodsContainer}.
     *
     * Always accessed synchronized and *after synchronized(storedGoods)*.
     * This is only touched rarely so the extra lock is tolerable.
     * (Not synchronized during I/O)
     */
    private final Map<GoodsType, Integer> oldStoredGoods = new HashMap<>();

    /** The location for this {@code GoodsContainer}. */
    private Location parent = null;


    /**
     * Creates an empty {@code GoodsContainer}.
     *
     * @param game The enclosing {@code Game}.
     * @param parent The {@code Location} this
     *     {@code GoodsContainer} contains goods for.
     */
    public GoodsContainer(Game game, Location parent) {
        super(game);

        this.parent = parent;
    }

    /**
     * Create a new {@code GoodsContainer}.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public GoodsContainer(Game game, String id) {
        super(game, id);
    }


    /**
     * Set the goods location.
     *
     * @param location The {@code Location} to set.
     */
    public void setLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Null GoodsContainer Location.");
        }
        this.parent = location;
    }

    /**
     * Checks if the specified {@code Goods} is in this container.
     *
     * @param <T> The base type of the goods.
     * @param g The goods to test the presence of.
     * @return True if there is enough of the specified goods present that it
     *     can be removed without error.
     */
    public <T extends AbstractGoods> boolean contains(T g) {
        return getGoodsCount(g.getType()) >= g.getAmount();
    }

    /**
     * Gets the amount of one type of goods in this container.
     *
     * @param type The {@code GoodsType} being looked for.
     * @return The amount of this type of goods in this container.
     */
    public int getGoodsCount(GoodsType type) {
        synchronized (this.storedGoods) {
            return (this.storedGoods.containsKey(type))
                ? this.storedGoods.get(type)
                : 0;
        }
    }

    /**
     * Gets the amount of one type of goods at the beginning of the turn.
     *
     * @param type The {@code GoodsType} being looked for.
     * @return The amount of this type of goods in this container at
     *     the beginning of the turn
     */
    public int getOldGoodsCount(GoodsType type) {
        synchronized (this.oldStoredGoods) {
            return (this.oldStoredGoods.containsKey(type))
                ? this.oldStoredGoods.get(type)
                : 0;
        }
    }

    /**
     * Adds goods to this goods container.
     *
     * @param <T> The base type of the goods.
     * @param goods The goods to add.
     * @return True if the addition succeeds.
     */
    public <T extends AbstractGoods> boolean addGoods(T goods) {
        return addGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Adds goods by type and amount to this goods container.
     *
     * Note: negative amounts are allowed.
     *
     * @param type The {@code GoodsType} to add.
     * @param amount The amount of goods to add.
     * @return True if the addition succeeds.
     */
    public boolean addGoods(GoodsType type, int amount) {
        int oldAmount = getGoodsCount(type);
        int newAmount = oldAmount + amount;

        if (newAmount < 0) {
            throw new IllegalStateException("Operation would leave "
                + newAmount + " goods of type " + type
                + " in Location " + parent);
        } else if (newAmount == 0) {
            synchronized (this.storedGoods) {
                this.storedGoods.remove(type);
            }
        } else {
            synchronized (this.storedGoods) {
                this.storedGoods.put(type, newAmount);
            }
        }
        return true;
    }

    /**
     * Removes goods from this goods container.
     *
     * @param <T> The base type of the goods.
     * @param goods The goods to remove from this container.
     * @return The {@code Goods} actually removed.
     */
    public <T extends AbstractGoods> Goods removeGoods(T goods) {
        return removeGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Removes all goods of a given type from this goods container.
     *
     * @param type The {@code GoodsType} to remove.
     * @return The {@code Goods} actually removed.
     */
    public Goods removeGoods(GoodsType type) {
        return removeGoods(type, INFINITY);
    }

    /**
     * Removes goods by type and amount from this goods container.
     *
     * @param type The {@code GoodsType} to remove.
     * @param amount The amount of goods to remove.
     * @return The {@code Goods} actually removed, which may have a
     *     lower actual amount, or null if nothing removed.
     */
    public Goods removeGoods(GoodsType type, int amount) {
        int oldAmount = getGoodsCount(type);
        if (oldAmount <= 0) return null;

        int newAmount = oldAmount - amount;
        Goods removedGoods;
        if (newAmount > 0) {
            removedGoods = new Goods(getGame(), null, type, amount);
            synchronized (this.storedGoods) {
                this.storedGoods.put(type, newAmount);
            }
        } else {
            removedGoods = new Goods(getGame(), null, type, oldAmount);
            synchronized (this.storedGoods) {
                this.storedGoods.remove(type);
            }
        }
        return removedGoods;
    }

    /**
     * Set the amount of goods in this container.
     *
     * @param goodsType The {@code GoodsType} to set the amount of.
     * @param newAmount The new amount.
     */
    public void setAmount(GoodsType goodsType, int newAmount) {
        if (newAmount == 0) {
            synchronized (this.storedGoods) {
                this.storedGoods.remove(goodsType);
            }
        } else {
            synchronized (this.storedGoods) {
                this.storedGoods.put(goodsType, newAmount);
            }
        }
    }

    /**
     * Remove all goods.
     */
    public void removeAll() {
        synchronized (this.storedGoods) {
            this.storedGoods.clear();
        }
    }

    /**
     * Clear both containers.
     */
    private void clearContainers() {
        synchronized (this.storedGoods) {
            this.storedGoods.clear();
            synchronized (this.oldStoredGoods) {
                this.oldStoredGoods.clear();
            }
        }
    }

    /**
     * Removes all goods above given amount, provided that the goods
     * are storable and do not ignore warehouse limits.
     *
     * @param newAmount The threshold.
     */
    public void removeAbove(final int newAmount) {
        synchronized (this.storedGoods) {
            if (newAmount <= 0) {
                this.storedGoods.clear();
                return;
            }

            for (Map.Entry<GoodsType, Integer> e : this.storedGoods.entrySet()) {
                final GoodsType gt = e.getKey();
                if (gt.isStorable() && !gt.limitIgnored() && e.getValue() > newAmount)
                    this.storedGoods.put(e.getKey(), newAmount);
            }
        }
    }

    /**
     * Checks if any storable type of goods has reached the given amount.
     *
     * @param amount The amount to check.
     * @return True if any storable, capacity limited goods has reached the
     *     given amount.
     */
    public boolean hasReachedCapacity(int amount) {
        synchronized (this.storedGoods) {
            for (Map.Entry<GoodsType, Integer> e : this.storedGoods.entrySet())
                if (e.getKey().isStorable()
                           && !e.getKey().limitIgnored()
                           && e.getValue() > amount) return true;
            return false;
        }
    }

    /**
     * Gets the amount of space that the goods in this container will consume.
     * Each occupied cargo slot contains an amount in [1, CARGO_SIZE].
     *
     * @return The amount of space taken by this containers goods.
     */
    public int getSpaceTaken() {
        synchronized (this.storedGoods) {
            int result = 0;
            for (int amount : this.storedGoods.values())
                result += (((amount % CARGO_SIZE == 0)
                           ? amount/CARGO_SIZE
                           : amount/CARGO_SIZE + 1));
            return result;
        }
    }

    /**
     * Gets a list containing all holds of goods in this goods container.
     * Each {@code Goods} returned has a maximum amount of CARGO_SIZE.
     *
     * @return A list of {@code Goods}.
     */
    public List<Goods> getGoods() {
        final Game game = getGame();
        List<Goods> result = new ArrayList<>();
        synchronized (this.storedGoods) {
            for (Map.Entry<GoodsType, Integer> e : this.storedGoods.entrySet()) {
                    int amount = e.getValue();
                    while (amount > 0) {
                        result.add(new Goods(game, parent, e.getKey(),
                                ((amount >= CARGO_SIZE) ? CARGO_SIZE : amount)));
                        amount -= CARGO_SIZE;
                    }
            }
        }
        return result;
    }

    /**
     * Gets a list of all goods in this goods container.
     * There is only one {@code Goods} for each distinct
     * {@code GoodsType}.
     *
     * @return A list of {@code Goods}.
     */
    public List<Goods> getCompactGoods() {
        final Game game = getGame();
        synchronized (this.storedGoods) {
            List<Goods> result = new ArrayList<>();
            for (Map.Entry<GoodsType,Integer> e : this.storedGoods.entrySet())
                if (e.getValue() > 0)
                    result.add(new Goods(game, parent, e.getKey(), e.getValue()));
            return result;
        }
    }

    /**
     * Save the current stored goods of this goods container in the old
     * stored goods.
     */
    public void saveState() {
        synchronized (this.storedGoods) {
            synchronized (this.oldStoredGoods) {
                this.oldStoredGoods.clear();
                this.oldStoredGoods.putAll(this.storedGoods);
            }
        }
    }

    /**
     * Restore the current stored goods of this goods container to the
     * old state.
     */
    public void restoreState() {
        synchronized (this.storedGoods) {
            synchronized (this.oldStoredGoods) {
                this.storedGoods.clear();
                this.storedGoods.putAll(this.oldStoredGoods);
            }
        }
    }

    /**
     * Has this goods containers contents changed from what was recorded
     * last time the state was saved?
     *
     * @return True if the contents have changed.
     */
    public boolean hasChanged() {
        for (GoodsType gt : getSpecification().getGoodsTypeList())
            if (getOldGoodsCount(gt) != getGoodsCount(gt))
                return true;
        return false;
    }

    /**
     * Fire property changes for all goods that have seen level changes
     * since the last saveState().
     *
     * @return True if something changed.
     */
    public boolean fireChanges() {
        boolean ret = false;
        for (GoodsType type : getSpecification().getGoodsTypeList()) {
            int oldCount = getOldGoodsCount(type);
            int newCount = getGoodsCount(type);
            if (oldCount != newCount) {
                firePropertyChange(type.getId(), oldCount, newCount);
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Utility to move goods from one location to another.
     *
     * @param src The source {@code GoodsContainer}.
     * @param goodsType The {@code GoodsType} to move.
     * @param amount The amount of goods to move.
     * @param dst The new {@code GoodsContainer}.
     */
    public static void moveGoods(GoodsContainer src,
                                 GoodsType goodsType, int amount,
                                 GoodsContainer dst) {
        if (src != null) {
            src.saveState();
            src.removeGoods(goodsType, amount);
        }
        if (dst != null) {
            dst.saveState();
            dst.addGoods(goodsType, amount);
        }
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getOwner() {
        return (parent instanceof Ownable) ? ((Ownable)parent).getOwner()
            : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOwner(Player p) {
        throw new UnsupportedOperationException("Can not set GoodsContainer owner");
    }

    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public void disposeResources() {
        clearContainers();
        super.disposeResources();
    }


    // Serialization

    public static final String AMOUNT_TAG = "amount";
    public static final String OLD_STORED_GOODS_TAG = "oldStoredGoods";
    public static final String STORED_GOODS_TAG = "storedGoods";
    public static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (xw.validFor(getOwner())) {

            synchronized (this.storedGoods) {
                writeStorage(xw, STORED_GOODS_TAG, this.storedGoods);
                synchronized (this.oldStoredGoods) {
                    writeStorage(xw, OLD_STORED_GOODS_TAG, this.oldStoredGoods);
                }
            }
        }
    }

    /**
     * Write a storage container to a stream.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @param tag The element tag.
     * @param storage The storage container.
     * @exception XMLStreamException if there is a problem writing to
     *     the stream.
     */
    private void writeStorage(FreeColXMLWriter xw, String tag,
                              Map<GoodsType, Integer> storage) throws XMLStreamException {
        if (storage.isEmpty()) return;

        xw.writeStartElement(tag);

        for (GoodsType goodsType : sort(storage.keySet())) {

            xw.writeStartElement(Goods.TAG);

            xw.writeAttribute(TYPE_TAG, goodsType);

            xw.writeAttribute(AMOUNT_TAG, storage.get(goodsType));

            xw.writeEndElement();
        }

        xw.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearContainers();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (OLD_STORED_GOODS_TAG.equals(tag)) {
            synchronized (this.oldStoredGoods) {
                readStorage(xr, this.oldStoredGoods);
            }

        } else if (STORED_GOODS_TAG.equals(tag)) {
            synchronized (this.storedGoods) {
                readStorage(xr, this.storedGoods);
            }

        } else {
            super.readChild(xr);
        }
    }

    /**
     * Read a storage container from a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param storage The storage container.
     * @exception XMLStreamException if there is a problem reading from
     *     the stream.
     */
    private void readStorage(FreeColXMLReader xr,
        Map<GoodsType, Integer> storage) throws XMLStreamException {
        final Specification spec = getGame().getSpecification();

        while (xr.moreTags()) {
            String tag = xr.getLocalName();

            if (Goods.TAG.equals(tag)) {
                GoodsType goodsType = xr.getType(spec, TYPE_TAG,
                    GoodsType.class, (GoodsType)null);

                int amount = xr.getAttribute(AMOUNT_TAG, 0);

                storage.put(goodsType, amount);

            } else {
                throw new XMLStreamException("Bogus GoodsContainer tag: "
                    + tag);
            }
            xr.closeTag(tag);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String sep = ", ";
        StringBuilder sb = new StringBuilder(128);
        sb.append('[').append(getId()).append(" [");
        // Do not bother to synchronize containers for display
        for (Map.Entry<GoodsType, Integer> e : storedGoods.entrySet())
            sb.append(e.getKey()).append('=').append(e.getValue()).append(sep);
        sb.setLength(sb.length() - sep.length());
        sb.append("][");
        for (Map.Entry<GoodsType, Integer> e : oldStoredGoods.entrySet())
            sb.append(e.getKey()).append('=').append(e.getValue()).append(sep);
        sb.setLength(sb.length() - sep.length());
        sb.append("]]");
        return sb.toString();
    }
}
