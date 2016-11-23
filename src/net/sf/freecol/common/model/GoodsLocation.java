/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


/**
 * A <code>GoodsLocation</code> is a place where {@link Unit}s and
 * {@link Goods} can be put.  It can not store any other Locatables,
 * such as {@link TileItem}s.
 *
 * @see Locatable
 */
public abstract class GoodsLocation extends UnitLocation {

    private static final Logger logger = Logger.getLogger(GoodsLocation.class.getName());

    /**
     * The container for the goods.
     */
    private GoodsContainer goodsContainer;


    protected GoodsLocation() {
        // empty constructor
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game The <code>Game</code> to create within.
     */
    public GoodsLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game The <code>Game</code> to create within.
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public GoodsLocation(Game game, XMLStreamReader in)
        throws XMLStreamException {
        super(game, in);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param game The <code>Game</code> to create within.
     * @param e An XML-element that will be used to initialize this object.
     */
    // Only Unit needs this
    public GoodsLocation(Game game, Element e) {
        super(game, e);
    }

    /**
     * Creates a new <code>GoodsLocation</code> instance.
     *
     * @param game The <code>Game</code> to create within.
     * @param id a <code>String</code> value
     */
    public GoodsLocation(Game game, String id) {
        super(game, id);
    }


    /**
     * Removes all references to this object.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        if (goodsContainer != null) {
            objects.addAll(goodsContainer.disposeList());
            goodsContainer = null;
        }
        objects.addAll(super.disposeList());

        return objects;
    }

    // getGoodsContainer() is part of the Location interface.

    /**
     * Set the <code>GoodsContainer</code> value.
     *
     * @param goodsContainer The new GoodsContainer value.
     */
    public final void setGoodsContainer(final GoodsContainer goodsContainer) {
        this.goodsContainer = goodsContainer;
    }

    /**
     * Adds some goods to this location.
     *
     * @param goods The <code>AbstractGoods</code> to add.
     * @return True if the goods were added.
     */
    public final boolean addGoods(AbstractGoods goods) {
        return addGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Removes the some goods from this location.
     *
     * @param goods The <code>AbstractGoods</code> to remove.
     * @return The goods that was removed, which may be less than that
     *     requested, or null if none.
     */
    public final Goods removeGoods(AbstractGoods goods) {
        return removeGoods(goods.getType(), goods.getAmount());
    }

    /**
     * Removes all Goods of the given type from this location.
     *
     * @param type The <code>GoodsType</code> to remove.
     * @return The goods that was removed, or null if none.
     */
    public final Goods removeGoods(GoodsType type) {
        return removeGoods(type, getGoodsCount(type));
    }

    /**
     * Gets the amount of one type of goods at this location.
     *
     * @param type The <code>GoodsType</code> to look for.
     * @return The amount of goods.
     */
    public final int getGoodsCount(GoodsType type) {
        return goodsContainer.getGoodsCount(type);
    }

    /**
     * Gets an iterator for every <code>Goods</code> in this location.
     * Each <code>Goods</code> have a maximum amount of CARGO_SIZE.
     *
     * @return The <code>Iterator</code>.
     */
    public final Iterator<Goods> getGoodsIterator() {
        return goodsContainer.getGoodsIterator();
    }

    /**
     * Gets a list of all the goods in this location.  Each list member is
     * limited to a maximum amount of CARGO_SIZE, thus there may be multiple
     * entries with the same goods type.
     *
     * @return A list of goods.
     */
    public final List<Goods> getGoods() {
        return goodsContainer.getGoods();
    }

    /**
     * Gets an list of all the goods in this location.  There is only
     * one <code>Goods</code> for each <code>GoodsType</code>, thus the
     * amount of goods may exceed CARGO_SIZE.
     *
     * @return A list of goods.
     */
    public final List<Goods> getCompactGoods() {
        return goodsContainer.getCompactGoods();
    }


    // Interface Location
    // Inheriting getId(), getTile(), getLocationName/For, canAdd,
    // getUnitCount, getUnitList, getSettlement, getColony from
    // UnitLocation.

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        return (locatable instanceof Goods)
            ? addGoods((Goods)locatable)
            : super.add(locatable);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Locatable locatable) {
        return (locatable instanceof Goods)
            ? removeGoods((Goods)locatable) != null
            : super.remove(locatable);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Locatable locatable) {
        return (locatable instanceof Goods)
            ? goodsContainer.contains((Goods)locatable)
            : super.contains(locatable);
    }

    /**
     * {@inheritDoc}
     *
     * Marked final, as this is where the goods container is.
     */
    public final GoodsContainer getGoodsContainer() {
        return goodsContainer;
    }

    // Interface UnitLocation routines
    // Inheriting getSpaceTaken, moveToFront, clearUnitList,
    // getUnitCapacity from UnitLocation.

    /**
     * {@inheritDoc}
     */
    public NoAddReason getNoAddReason(Locatable locatable) {
        if (locatable instanceof Goods) {
            Goods goods = (Goods)locatable;
            return (goods.getSpaceTaken() + goodsContainer.getSpaceTaken()
                > getGoodsCapacity())
                ? NoAddReason.CAPACITY_EXCEEDED
                : NoAddReason.NONE;
        }
        return super.getNoAddReason(locatable);
    }

    // GoodsLocation routines.

    /**
     * Gets the maximum number of <code>Goods</code> this Location
     * can hold.
     *
     * @return The capacity for goods
     */
    public abstract int getGoodsCapacity();

    /**
     * Adds a specified amount of a type of goods to this location.
     *
     * @param type The <code>GoodsType</code> to add.
     * @param amount The amount of goods to add.
     * @return True if the goods were added.
     */
    public boolean addGoods(GoodsType type, int amount) {
        return goodsContainer.addGoods(type, amount);
    }

    /**
     * Removes a specified amount of a type of Goods from this location.
     *
     * @param type The type of goods to remove.
     * @param amount The amount of goods to remove.
     * @return The goods that was removed, which may be less than that
     *     requested, or null if none.
     */
    public Goods removeGoods(GoodsType type, int amount) {
        return goodsContainer.removeGoods(type, amount);
    }

    // Serialization

    /**
     * {@inheritDoc}
     */
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        super.writeChildren(out, player, showAll, toSavedGame);
        if (goodsContainer != null) {
            goodsContainer.toXML(out, player, showAll, toSavedGame);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if (GoodsContainer.getXMLElementTagName().equals(in.getLocalName())) {
            goodsContainer = getGame()
                .getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE),
                                      GoodsContainer.class);
            if (goodsContainer == null) {
                goodsContainer = new GoodsContainer(getGame(), this, in);
            } else {
                goodsContainer.readFromXML(in);
            }
        } else {
            super.readChild(in);
        }
    }
}
