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
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.metaserver.ServerInfo;
import net.sf.freecol.server.FreeColServer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when to get a list of servers.
 */
public class ServerListMessage extends DOMMessage {

    public static final String TAG = "serverList";

    /** The list of information about the available servers. */
    private List<ServerInfo> servers = new ArrayList<>();


    /**
     * Create a new {@code ServerListMessage}.  Used to generate
     * a request for servers.
     */
    public ServerListMessage() {
        super(TAG);

        this.servers.clear();
    }

    /**
     * Create a new {@code ServerListMessage} from a
     * supplied element.  Used to read the reply.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ServerListMessage(Game game, Element element) {
        this();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element)nodes.item(i);
            if (e == null) continue;
            ServerInfo si = new RegisterServerMessage(null, element).getServerInfo();
            if (si != null) this.servers.add(si);
        }
    }


    // Public interface

    /**
     * Get the server information.
     *
     * @return The list of {@code ServerInfo}.
     */
    public List<ServerInfo> getServers() {
        return this.servers;
    }

    /**
     * Add information about a server.
     *
     * @param si The {@code ServerInfo} to add.
     */
    public void addServer(ServerInfo si) {
        this.servers.add(si);
    }


    /**
     * Convert this ServerListMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        List<DOMMessage> ml = new ArrayList<>();
        for (ServerInfo srv : this.servers)
            ml.add(new RegisterServerMessage(srv));

        return new DOMMessage(TAG)
            .<DOMMessage>addMessages(ml)
            .toXMLElement();
    }
}
