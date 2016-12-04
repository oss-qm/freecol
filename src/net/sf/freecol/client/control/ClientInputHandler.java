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

package net.sf.freecol.client.control;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.DOMMessageHandler;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.VacantPlayersMessage;

import org.w3c.dom.Element;


/**
 * Provides common methods for input handlers on the client side.
 */
public abstract class ClientInputHandler extends FreeColClientHolder
    implements DOMMessageHandler {

    private static final Logger logger = Logger.getLogger(ClientInputHandler.class.getName());

    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ClientInputHandler(FreeColClient freeColClient) {
        super(freeColClient);
    }

    /**
     * Do the actual message handling.
     *
     * Common messages (applying to all game states) are handled here.
     * Subclasses for individual game states will override it, but still call this
     * one (super), in order to get the common messages handled.
     *
     * @param connection   The connection, where the messages coming in
     * @param element      The message Element
     * @return             True if the message was handled
     */
    protected boolean handleElement(Connection connection, Element element) {
        String tag = element.getTagName();

        switch (tag) {
            case TrivialMessage.DISCONNECT_TAG:
                disconnect(element);
            break;

            case GameStateMessage.TAG:
                gameState(element);
            break;

            case VacantPlayersMessage.TAG:
                vacantPlayers(element);
            break;

            default:
                return false;
        }
        return true;
    }

    // Useful handlers

    /**
     * Handle a "disconnect"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    protected void disconnect(Element element) {
        ; // Do nothing
    }

    /**
     * Handle a "gameState"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void gameState(Element element) {
        final FreeColClient fcc = getFreeColClient();
        final GameStateMessage message
            = new GameStateMessage(fcc.getGame(), element);

        fcc.setServerState(message.getState());
    }

    /**
     * Handle a "vacantPlayers"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void vacantPlayers(Element element) {
        final FreeColClient fcc = getFreeColClient();
        final VacantPlayersMessage message
            = new VacantPlayersMessage(fcc.getGame(), element);

        fcc.setVacantPlayerNames(message.getVacantPlayers());
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Element handle(Connection connection, Element element) {
        if (element == null) return null;
        final String tag = element.getTagName();
        try {
            if (handleElement(connection, element))
                logger.log(Level.FINEST, "Client handled: " + tag);
            else
                logger.warning("unhandled tag: "+tag);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Client failed: " + tag, ex);
        }
        return null;
    }
}
