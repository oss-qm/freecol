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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message that contains other messages.
 */
public class MultipleMessage extends DOMMessage {

    public static final String TAG = "multiple";

    /** The list of messages. */
    private List<Element> elements = new ArrayList<>();


    /**
     * Create a new {@code MultipleMessage}.
     */
    public MultipleMessage() {
        super(TAG);

        this.elements.clear();
    }

    /**
     * Create a new {@code MultipleMessage}.
     *
     * @param elements A list of sub-{@code Element}s.
     */
    public MultipleMessage(List<Element> elements) {
        this();

        if (elements != null) this.elements.addAll(elements);
    }

    /**
     * Create a new {@code MultipleMessage}.
     *
     * @param element An element containing the sub-{@code Element}s.
     */
    public MultipleMessage(Element element) {
        super(TAG);

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
            this.elements.add((Element)nodes.item(i));
    }

    /**
     * Create a new {@code MultipleMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public MultipleMessage(Game game, Element element) {
        this();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
            this.elements.add((Element)nodes.item(i));
    }


    // Public interface

    /**
     * Add another message.
     *
     * @param message The {@code DOMMessage} to add.
     */
    public void addMessage(DOMMessage message) {
        this.elements.add(message.toXMLElement());
    }

    /**
     * Apply a handler to this message.
     *
     * @param handler A {@code DOMMessageHandler} to apply.
     * @param connection The {@code Connection} message was received on.
     * @return A collapsed resolution of the submessages.
     */
    public Element applyHandler(DOMMessageHandler handler,
                                Connection connection) {
        return DOMUtils.handleList(handler, connection, this.elements);
    }


    /**
     * About to go away.
     */
    public Element handle(FreeColServer freeColServer, Connection connection) {
        return applyHandler(connection.getDOMMessageHandler(), connection);
    }


    /**
     * Convert this MultipleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        DOMMessage result = new DOMMessage(TAG);
        for (Element e : this.elements) result.add(e);
        return result.toXMLElement();
    }
}
