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
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when unloading goods.
 */
public class UnloadGoodsMessage extends AttributeMessage {

    public static final String TAG = "unloadGoods";
    private static final String AMOUNT_TAG = "amount";
    private static final String CARRIER_TAG = "carrier";
    private static final String TYPE_TAG = "type";


    /**
     * Create a new {@code UnloadGoodsMessage}.
     *
     * @param goodsType The {@code GoodsType} to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The {@code Unit} carrying the goods.
     */
    public UnloadGoodsMessage(GoodsType goodsType, int amount, Unit carrier) {
        super(TAG, TYPE_TAG, goodsType.getId(),
              AMOUNT_TAG, String.valueOf(amount),
              CARRIER_TAG, carrier.getId());
    }

    /**
     * Create a new {@code UnloadGoodsMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UnloadGoodsMessage(Game game, Element element) {
        super(TAG, TYPE_TAG, getStringAttribute(element, TYPE_TAG),
              AMOUNT_TAG, getStringAttribute(element, AMOUNT_TAG),
              CARRIER_TAG, getStringAttribute(element, CARRIER_TAG));
    }


    /**
     * {@inheritDoc}
     */
    public static MessagePriority getMessagePriority() {
        return Message.MessagePriority.NORMAL;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Specification spec = freeColServer.getSpecification();
        final String typeId = getStringAttribute(TYPE_TAG);
        final int amount = getIntegerAttribute(AMOUNT_TAG, -1);
        final String carrierId = getStringAttribute(CARRIER_TAG);

        Unit carrier;
        try {
            carrier = serverPlayer.getOurFreeColGameObject(carrierId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!carrier.canCarryGoods()) {
            return serverPlayer.clientError("Not a goods carrier: "
                + carrierId);
        }
        // Do not check location, carriers can dump goods anywhere

        GoodsType type = spec.getGoodsType(typeId);
        if (type == null) {
            return serverPlayer.clientError("Not a goods type: " + typeId);
        }

        if (amount <= 0) {
            return serverPlayer.clientError("Invalid amount: " + amount);
        }
        int present = carrier.getGoodsCount(type);
        if (present < amount) {
            return serverPlayer.clientError("Attempt to unload " + amount
                + " " + type.getId() + " but only " + present + " present");
        }

        // Try to unload.
        return freeColServer.getInGameController()
            .unloadGoods(serverPlayer, type, amount, carrier);
    }
}
