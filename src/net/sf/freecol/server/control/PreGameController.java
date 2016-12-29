/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The control object that is responsible for setting parameters
 * and starting a new game. {@link PreGameInputHandler} is used
 * to receive and handle network messages from the clients.
 *
 * The game enters the state
 * {@link net.sf.freecol.server.FreeColServer.ServerState#IN_GAME}, when the
 * {@link #startGame} has successfully been invoked.
 *
 * @see InGameInputHandler
 */
public final class PreGameController extends Controller {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());


    /** Is the game launching yet. */
    private boolean launching = false;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main {@code FreeColServer} object.
     */
    public PreGameController(FreeColServer freeColServer) {
        super(freeColServer);
    }


    /**
     * Set the launching state.
     *
     * @param launching The new launching state.
     * @return The former launching state.
     */
    private synchronized boolean setLaunching(boolean launching) {
        boolean old = this.launching;
        this.launching = launching;
        return old;
    }

    /**
     * Launch the game if possible.
     *
     * @param serverPlayer The {@code ServerPlayer} that requested launching.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet requestLaunch(ServerPlayer serverPlayer) {
        if (setLaunching(true)) return null;

        final FreeColServer freeColServer = getFreeColServer();
        final Game game = getGame();
        final Specification spec = game.getSpecification();

        // Check if launching player is an admin.
        if (!serverPlayer.isAdmin()) {
            setLaunching(false);
            return serverPlayer.clientError(StringTemplate
                .template("server.onlyAdminCanLaunch"));
        }
        serverPlayer.setReady(true);

        // Check that no two players have the same nation
        List<Nation> nations = new ArrayList<>();
        for (Player p : game.getLivePlayerList()) {
            final Nation nation = spec.getNation(p.getNationId());
            if (nations.contains(nation)) {
                setLaunching(false);
                return serverPlayer.clientError(StringTemplate
                    .template("server.invalidPlayerNations"));
            }
            nations.add(nation);
        }

        // Check if all players are ready.
        if (!game.allPlayersReadyToLaunch()) {
            setLaunching(false);
            return serverPlayer.clientError(StringTemplate
                .template("server.notAllReady"));
        }
        try {
            freeColServer.startGame();
        } catch (FreeColException fce) {
            return serverPlayer.clientError(fce.getMessage());
        }

        return null;
    }
}
