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

package net.sf.freecol.server.control;

import java.net.UnknownServiceException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DOMMessageHandler;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Element;


/**
 * Handles the network messages on the server side.
 *
 * @see Controller
 */
public abstract class ServerInputHandler extends FreeColServerHolder
    implements DOMMessageHandler {

    private static final Logger logger = Logger.getLogger(ServerInputHandler.class.getName());

    /**
     * The constructor to use.
     *
     * @param freeColServer The main server object.
     */
    public ServerInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
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
     * @return             Return element
     * @throws UnknownServiceException if tag isn't handled
     */
    public Element handleElement(Connection connection, Element element)
            throws UnknownServiceException {
        String tag = element.getTagName();
        switch (tag) {
            case ChatMessage.TAG:
                return handler(false, connection, new ChatMessage(getGame(), element));

            case TrivialMessage.DISCONNECT_TAG:
                return handler(false, connection, TrivialMessage.DISCONNECT_MESSAGE);

            case LogoutMessage.TAG:
                return handler(false, connection, new LogoutMessage(getGame(), element));

            default:
                throw new UnknownServiceException("ServerInputHandler: unhandled tag: "+tag);
        }
    }

    /**
     * Wrapper for new message handling.
     *
     * @param current If true, insist the message is from the current player
     *     in the game.
     * @param connection The {@code Connection} the message arrived on.
     * @param message The {@code DOMMessage} to handle.
     * @return The resulting reply {@code Message}.
     */
    private Message internalHandler(boolean current, Connection connection,
                                    DOMMessage message) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer serverPlayer = freeColServer.getPlayer(connection);
        final Game game = freeColServer.getGame();
        ChangeSet cs = (current && (game == null || serverPlayer == null
                || serverPlayer != game.getCurrentPlayer()))
            ? serverPlayer.clientError("Received: " + message.getType()
                + " out of turn from player: " + serverPlayer.getNation())
            : message.serverHandler(freeColServer, serverPlayer);
        return (cs == null) ? null : cs.build(serverPlayer);
    }

    /**
     * Wrapper for new message handling.
     *
     * @param current If true, insist the message is from the current player
     *     in the game.
     * @param connection The {@code Connection} the message arrived on.
     * @param message The {@code DOMMessage} to handle.
     * @return The resulting reply {@code Element}.
     */
    protected Element handler(boolean current, Connection connection,
                              DOMMessage message) {
        Message m = internalHandler(current, connection, message);
        return (m == null) ? null : ((DOMMessage)m).toXMLElement();
    }

    // Implement DOMMessageHandler

    /**
     * {@inheritDoc}
     */
    public final Element handle(Connection connection, Element element) {
        if (element == null) return null;
        try {
            Element ret = handleElement(connection, element);
            logger.log(Level.FINEST, "Handling " + element.getTagName() + " ok = "
                + ((ret == null) ? "null" : ret.getTagName()));
            return ret;
        } catch (UnknownServiceException e) {
            // Should we return an error here? The old handler returned null.
            logger.warning("No "
                + getFreeColServer().getServerState().toString().toLowerCase()
                + " handler for " + element.getTagName()+" "+e);
            return null;
        } catch (Exception e) {
            // FIXME: should we really catch Exception? The old code did.
            logger.log(Level.WARNING, "Handling " + element.getTagName() + " failed", e);
            connection.sendReconnect();
        }
        return null;
    }
}
