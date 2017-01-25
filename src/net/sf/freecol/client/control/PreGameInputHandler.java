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

import java.awt.Color;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.ReadyMessage;
import net.sf.freecol.common.networking.SetAvailableMessage;
import net.sf.freecol.common.networking.SetColorMessage;
import net.sf.freecol.common.networking.SetNationMessage;
import net.sf.freecol.common.networking.SetNationTypeMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
import net.sf.freecol.server.FreeColServer.ServerState;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives before the game starts.
 */
public final class PreGameInputHandler extends ClientInputHandler {

    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public PreGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(AddPlayerMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    addPlayer(e);
            }});

        register(ChatMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    chat(e);
            }});

        register(ErrorMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    error(e);
            }});

        register(LoginMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    login(e);
            }});

        register(LogoutMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    logout(e);
            }});

        register(MultipleMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    multiple(c, e);
            }});

        register(ReadyMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    ready(e);
            }});

        register(SetAvailableMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    setAvailable(e);
            }});

        register(SetColorMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    setColor(e);
            }});

        register(SetNationMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    setNation(e);
            }});

        register(TrivialMessage.START_GAME_TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    startGame(e);
            }});

        register(UpdateMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    update(e);
            }});

        register(UpdateGameOptionsMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    updateGameOptions(e);
            }});

        register(UpdateMapGeneratorOptionsMessage.TAG,
            new ClientNetworkRequestHandler() {
                public void handle(Connection c, Element e) {
                    updateMapGeneratorOptions(e);
            }});
    }


    // Individual handlers

    /**
     * Handles an "addPlayer"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void addPlayer(Element element) {
        final Game game = getGame();

        // The message constructor interns the new players directly.
        new AddPlayerMessage(game, element);
        getGUI().refreshPlayersTable();
    }

    /**
     * Handles a "chat"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void chat(Element element)  {
        final Game game = getGame();
        final ChatMessage chatMessage = new ChatMessage(game, element);

        getGUI().displayChatMessage(chatMessage.getPlayer(game),
                                    chatMessage.getMessage(),
                                    chatMessage.isPrivate());
    }

    /**
     * Handles an "error"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void error(Element element)  {
        final ErrorMessage errorMessage = new ErrorMessage(getGame(), element);

        getGUI().showErrorMessage(errorMessage.getTemplate(),
                                  errorMessage.getMessage());
    }

    /**
     * Handle a "login"-request.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void login(Element element) {
        final LoginMessage message = new LoginMessage(new Game(), element);
        final String user = message.getUserName();
        final boolean single = message.getSinglePlayer();
        final boolean current = message.getCurrentPlayer();
        final ServerState state = message.getState();
        final Game game = message.getGame(); // This is the real game!

        this.freeColClient.getConnectController()
            .login(state, game, user, single, current);
    }

    /**
     * Handles an "logout"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void logout(Element element) {
        final Game game = getGame();
        final LogoutMessage message = new LogoutMessage(game, element);
        final Player player = message.getPlayer(game);
        final LogoutReason reason = message.getReason();

        game.removePlayer(player);
        getGUI().refreshPlayersTable();
        if (player == getMyPlayer()) {
            this.freeColClient.getConnectController()
                .logout(reason);
        }
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The {@code Connection} the element arrived on.
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    public void multiple(Connection connection, Element element) {
        Element result = new MultipleMessage(element)
            .applyHandler(this, connection);
        assert result == null;
    }

    /**
     * Handles a "ready"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void ready(Element element) {
        final Game game = getGame();
        final ReadyMessage message = new ReadyMessage(game, element);

        Player player = message.getPlayer(game);
        boolean ready = message.getValue();
        if (player != null) {
            player.setReady(ready);
            getGUI().refreshPlayersTable();
        }
    }

    /**
     * Handles a "setAvailable"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void setAvailable(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final SetAvailableMessage message
            = new SetAvailableMessage(game, element);

        game.getNationOptions().setNationState(message.getNation(spec),
                                               message.getNationState());
        getGUI().refreshPlayersTable();
    }

    /**
     * Handles an "setColor"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void setColor(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final SetColorMessage message = new SetColorMessage(game, element);

        Nation nation = message.getNation(spec);
        if (nation != null) {
            Color color = message.getColor();
            if (color != null) {
                nation.setColor(color);
                getGUI().refreshPlayersTable();
            } else {
                logger.warning("Invalid color: " + message.toString());
            }
        } else {
            logger.warning("Invalid nation: " + message.toString());
        }
    }

    /**
     * Handles a "setNation"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void setNation(Element element) {
        final Game game = getGame();
        final SetNationMessage message = new SetNationMessage(game, element);

        Player player = message.getPlayer(game);
        Nation nation = message.getValue(getSpecification());
        if (player != null && nation != null) {
            player.setNation(nation);
            getGUI().refreshPlayersTable();
        }
    }

    /**
     * Handles a "setNationType"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void setNationType(Element element) {
        final Game game = getGame();
        final SetNationTypeMessage message
            = new SetNationTypeMessage(game, element);

        Player player = message.getPlayer(game);
        NationType nationType = message.getValue(getSpecification());
        if (player != null && nationType != null) {
            player.changeNationType(nationType);
            getGUI().refreshPlayersTable();
        }
    }

    /**
     * Handles an "startGame"-message.
     *
     * Wait until map is received from server, sometimes this
     * message arrives when map is still null. Wait in other
     * thread in order not to block and it can receive the map.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void startGame(@SuppressWarnings("unused") Element element) {
        new Thread(FreeCol.CLIENT_THREAD + "Starting game") {
                @Override
                public void run() {
                    Game game;
                    for (;;) {
                        game = getGame();
                        if (game != null && game.getMap() != null) break;
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            logger.log(Level.SEVERE, "Thread used for starting a game has been interupted.", ie);
                        }
                    }

                    SwingUtilities.invokeLater(() -> {
                            pgc().startGame();
                        });
                }
            }.start();
    }

    /**
     * Handles an "update"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void update(Element element) {
        final Game game = getGame();
        final UpdateMessage message = new UpdateMessage(game, element);

        for (FreeColGameObject fcgo : message.getObjects()) {
            if (fcgo instanceof Game) {
                this.freeColClient
                    .addSpecificationActions(((Game)fcgo).getSpecification());
            } else {
                logger.warning("Game node expected: " + fcgo.getId());
            }
        }
    }

    /**
     * Handles an "updateGameOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void updateGameOptions(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final UpdateGameOptionsMessage message
            = new UpdateGameOptionsMessage(game, element);

        if (!spec.mergeGameOptions(message.getGameOptions(), "client")) {
            logger.warning("Game option update failed");
        }
    }

    /**
     * Handles an "updateMapGeneratorOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    private void updateMapGeneratorOptions(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final UpdateMapGeneratorOptionsMessage message
            = new UpdateMapGeneratorOptionsMessage(game, element);

        if (!spec.mergeMapGeneratorOptions(message.getMapGeneratorOptions(),
                                           "client")) {
            logger.warning("Map generator option update failed");
        }
    }
}
