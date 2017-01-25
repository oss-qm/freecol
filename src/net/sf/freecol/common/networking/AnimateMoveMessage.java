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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

import org.w3c.dom.Element;


/**
 * The message sent to tell a client to show a movement animation.
 */
public class AnimateMoveMessage extends DOMMessage {

    public static final String TAG = "animateMove";
    private static final String NEW_TILE_TAG = "newTile";
    private static final String OLD_TILE_TAG = "oldTile";
    private static final String UNIT_TAG = "unit";

    /**
     * The unit to move *if* it is not currently visible, or its carrier
     * if on a carrier.
     */
    private Unit unit = null;


    /**
     * Create a new {@code AnimateMoveMessage} for the supplied unit and
     * direction.
     *
     * @param unit The {@code Unit} to move.
     * @param direction The {@code Direction} to move in.
     */
    public AnimateMoveMessage(Unit unit, Tile oldTile, Tile newTile,
                              boolean appears) {
        super(TAG, UNIT_TAG, unit.getId(),
              NEW_TILE_TAG, newTile.getId(), OLD_TILE_TAG, oldTile.getId());
        if (appears) {
            this.unit = (unit.isOnCarrier()) ? unit.getCarrier() : unit;
        }
    }

    /**
     * Create a new {@code AnimateMoveMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AnimateMoveMessage(Game game, Element element) {
        super(TAG, NEW_TILE_TAG, getStringAttribute(element, NEW_TILE_TAG),
              OLD_TILE_TAG, getStringAttribute(element, OLD_TILE_TAG),
              UNIT_TAG, getStringAttribute(element, UNIT_TAG));
        this.unit = getChild(game, element, 0, true, Unit.class);
    }


    // Public interface

    /**
     * Get the unit that is moving.
     *
     * @return The {@code Unit} that moves.
     */
    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(getStringAttribute(UNIT_TAG),
                                         Unit.class);
    }

    /**
     * Get the tile to move to.
     *
     * @return The new {@code Tile}.
     */
    public Tile getNewTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(NEW_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the tile to move from.
     *
     * @return The old {@code Tile}.
     */
    public Tile getOldTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(OLD_TILE_TAG),
                                         Tile.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return null; // Only sent to client
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            NEW_TILE_TAG, getStringAttribute(NEW_TILE_TAG),
            OLD_TILE_TAG, getStringAttribute(OLD_TILE_TAG),
            UNIT_TAG, getStringAttribute(UNIT_TAG))
            .add(this.unit).toXMLElement();
    }
}
