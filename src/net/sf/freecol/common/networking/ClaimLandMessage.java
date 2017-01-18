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

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.NoClaimReason;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests claiming land.
 */
public class ClaimLandMessage extends AttributeMessage {

    public static final String TAG = "claimLand";
    private static final String CLAIMANT_TAG = "claimant";
    private static final String PRICE_TAG = "price";
    private static final String TILE_TAG = "tile";


    /**
     * Create a new {@code ClaimLandMessage}.
     *
     * @param tile The {@code Tile} to claim.
     * @param claimant The {@code Unit} or {@code Settlement}
     *     claiming the tile.
     * @param price The price to pay for the tile, negative if stealing.
     */
    public ClaimLandMessage(Tile tile, FreeColGameObject claimant, int price) {
        super(TAG, TILE_TAG, tile.getId(), CLAIMANT_TAG, claimant.getId(),
              PRICE_TAG, String.valueOf(price));
    }

    /**
     * Create a new {@code ClaimLandMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ClaimLandMessage(Game game, Element element) {
        super(TAG, TILE_TAG, getStringAttribute(element, TILE_TAG),
              CLAIMANT_TAG, getStringAttribute(element, CLAIMANT_TAG),
              PRICE_TAG, getStringAttribute(element, PRICE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final String tileId = getStringAttribute(TILE_TAG);
        final String claimantId = getStringAttribute(CLAIMANT_TAG);
        final String priceString = getStringAttribute(PRICE_TAG);

        Tile tile = game.getFreeColGameObject(tileId, Tile.class);
        if (tile == null) {
            return serverPlayer.clientError("Not a file: " + tileId);
        }

        Unit unit = null;
        try {
            unit = serverPlayer.getOurFreeColGameObject(claimantId, Unit.class);
        } catch (IllegalStateException e) {} // Expected to fail sometimes...
        Settlement settlement = null;
        try {
            settlement = serverPlayer.getOurFreeColGameObject(claimantId,
                                                        Settlement.class);
        } catch (IllegalStateException e) {} // ...as is this one...
        if (unit != null) {
            if (unit.getTile() != tile) {
                return serverPlayer.clientError("Unit not at tile: " + tileId);
            }
        } else if (settlement != null) {
            if (settlement.getOwner().isEuropean()
                && !settlement.getTile().isAdjacent(tile)) {
                return serverPlayer.clientError("Settlement can not claim tile: "
                    + tileId);
            }
        } else { // ...but not both of them.
            return serverPlayer.clientError("Not a unit or settlement: "
                + claimantId);
        }

        int price;
        try {
            price = Integer.parseInt(priceString);
        } catch (NumberFormatException e) {
            return serverPlayer.clientError("Bad price: " + priceString);
        }

        // Request is well formed, but there are more possibilities...
        int value = serverPlayer.getLandPrice(tile);
        Player owner = tile.getOwner();
        Settlement ownerSettlement = tile.getOwningSettlement();
        if (owner == null) { // unclaimed, always free
            price = 0;
        } else if (owner == serverPlayer) { // capture vacant colony tiles only
            if (settlement != null && ownerSettlement != null
                && tile.isInUse()) {
                return serverPlayer.clientError("Can not claim tile "
                    + tile.getId() + ": already owned.");
            }
            price = 0;
        } else if (owner.isEuropean()) {
            if (tile.getOwningSettlement() == null  // its not "nailed down"
                || tile.getOwningSettlement() == settlement) { // pre-attached
                price = 0;
            } else { // must fail
                return serverPlayer.clientError("Can not claim tile "
                    + tile.getId() + ": European owners will not sell.");
            }
        } else { // natives
            NoClaimReason why = serverPlayer.canClaimForSettlementReason(tile);
            switch (why) {
            case NONE:
                break; // Succeed.
            case NATIVES:
                if (price >= 0) {
                    if (price < value) {
                        return serverPlayer.clientError("Can not claim tile "
                            + tile.getId() + ": insufficient offer.");
                    }
                    if (!serverPlayer.checkGold(price)) {
                        return serverPlayer.clientError("Can not pay for tile: "
                            + tile.getId() + ": insufficient funds.");
                    }
                    // Succeed, sufficient offer
                } // else succeed, stealing
                break;
            default: // Fail
                return serverPlayer.clientError("Can not claim tile "
                    + tile.getId() + ": " + why);
            }
        }

        // Proceed to claim.  Note, does not require unit, it is only
        // required for permission checking above.  Settlement is required
        // to set owning settlement.
        return freeColServer.getInGameController()
            .claimLand(serverPlayer, tile, settlement, price);
    }
}
