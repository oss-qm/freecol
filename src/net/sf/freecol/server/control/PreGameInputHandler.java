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

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.ReadyMessage;
import net.sf.freecol.common.networking.SetAvailableMessage;
import net.sf.freecol.common.networking.SetColorMessage;
import net.sf.freecol.common.networking.SetNationMessage;
import net.sf.freecol.common.networking.SetNationTypeMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Handle messages that arrive before the game starts.
 *
 * @see PreGameController
 */
public final class PreGameInputHandler extends ServerInputHandler {

    /**
     * Create the a new pre-game controller.
     *
     * @param freeColServer The main server object.
     */
    public PreGameInputHandler(FreeColServer freeColServer) {
        super(freeColServer);

        register(ReadyMessage.TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, new ReadyMessage(getGame(), e));
                }});

        register(TrivialMessage.REQUEST_LAUNCH_TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, TrivialMessage.REQUEST_LAUNCH_MESSAGE);
                }});

        register(SetAvailableMessage.TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, new SetAvailableMessage(getGame(), e));
                }});

        register(SetColorMessage.TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, new SetColorMessage(getGame(), e));
                }});

        register(SetNationMessage.TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, new SetNationMessage(getGame(), e));
                }});

        register(SetNationTypeMessage.TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, new SetNationTypeMessage(getGame(), e));
                }});

        register(UpdateGameOptionsMessage.TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, new UpdateGameOptionsMessage(getGame(), e));
                }});

        register(UpdateMapGeneratorOptionsMessage.TAG,
            new ServerInputHandler.NetworkRequestHandler() {
                public Element handle(final Connection conn, final Element e) {
                    return handler(false, conn, new UpdateMapGeneratorOptionsMessage(getGame(), e));
                }});
    }
}
