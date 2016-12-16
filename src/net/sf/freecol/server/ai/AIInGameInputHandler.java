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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeleteTradeRouteMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.FountainOfYouthMessage;
import net.sf.freecol.common.networking.GameEndedMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.NativeGiftMessage;
import net.sf.freecol.common.networking.NativeTradeMessage;
import net.sf.freecol.common.networking.NewTradeRouteMessage;
import net.sf.freecol.common.networking.NewTurnMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.SetAIMessage;
import net.sf.freecol.common.networking.SetCurrentPlayerMessage;
import net.sf.freecol.common.networking.SetDeadMessage;
import net.sf.freecol.common.networking.SetStanceMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import static net.sf.freecol.common.util.CollectionUtils.*;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives while in the game.
 */
public final class AIInGameInputHandler implements MessageHandler {

    private static final Logger logger = Logger.getLogger(AIInGameInputHandler.class.getName());

    /** The server. */
    private final FreeColServer freeColServer;

    /** The player for whom I work. */
    private final ServerPlayer serverPlayer;

    /** The main AI object. */
    private final AIMain aiMain;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main server.
     * @param serverPlayer The {@code ServerPlayer} that is being
     *     managed by this AIInGameInputHandler.
     * @param aiMain The main AI-object.
     */
    public AIInGameInputHandler(FreeColServer freeColServer,
                                ServerPlayer serverPlayer,
                                AIMain aiMain) {
        if (freeColServer == null) {
            throw new NullPointerException("freeColServer == null");
        } else if (serverPlayer == null) {
            throw new NullPointerException("serverPlayer == null");
        } else if (!serverPlayer.isAI()) {
            throw new RuntimeException("Applying AIInGameInputHandler to a non-AI player!");
        } else if (aiMain == null) {
            throw new NullPointerException("aiMain == null");
        }

        this.freeColServer = freeColServer;
        this.serverPlayer = serverPlayer;
        this.aiMain = aiMain;
    }


    /**
     * Get the AI player using this {@code AIInGameInputHandler}.
     *
     * @return The {@code AIPlayer}.
     */
    private AIPlayer getAIPlayer() {
        return this.aiMain.getAIPlayer(this.serverPlayer);
    }

    /**
     * Gets the AI unit corresponding to a given unit, if any.
     *
     * @param unit The {@code Unit} to look up.
     * @return The corresponding AI unit or null if not found.
     */
    private AIUnit getAIUnit(Unit unit) {
        return this.aiMain.getAIUnit(unit);
    }

    /**
     * Get the game.
     *
     * @return The {@code Game} in the server.
     */
    private Game getGame() {
        return this.freeColServer.getGame();
    }

    /**
     * Get the enclosed player.
     *
     * @return This {@code ServerPlayer}.
     */
    private ServerPlayer getMyPlayer() {
        return this.serverPlayer;
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Element handle(Connection connection, Element element) {
        if (element == null) return null;

        final Game game = getGame();
        final String tag = element.getTagName();
        Element reply = null;
        try {
            switch (tag) {
            case ChooseFoundingFatherMessage.TAG:
                reply = chooseFoundingFather(new ChooseFoundingFatherMessage(game, element));
                break;
            case "diplomacy":
                reply = diplomacy(new DiplomacyMessage(game, element));
                break;
            case FirstContactMessage.TAG:
                reply = firstContact(new FirstContactMessage(game, element));
                break;
            case FountainOfYouthMessage.TAG:
                reply = fountainOfYouth(new FountainOfYouthMessage(game, element));
                break;
            case IndianDemandMessage.TAG:
                reply = indianDemand(new IndianDemandMessage(game, element));
                break;
            case "lootCargo":
                reply = lootCargo(new LootCargoMessage(game, element));
                break;
            case "monarchAction":
                reply = monarchAction(new MonarchActionMessage(game, element));
                break;
            case MultipleMessage.TAG:
                reply = multiple(connection, element);
                break;
            case NationSummaryMessage.TAG:
                reply = nationSummary(new NationSummaryMessage(game, element));
                break;
            case NativeTradeMessage.TAG:
                reply = nativeTrade(new NativeTradeMessage(game, element));
                break;
            case NewLandNameMessage.TAG:
                reply = newLandName(new NewLandNameMessage(game, element));
                break;
            case NewRegionNameMessage.TAG:
                reply = newRegionName(new NewRegionNameMessage(game, element));
                break;
            case TrivialMessage.RECONNECT_TAG:
                logger.warning("Reconnect on illegal operation,"
                    + " refer to any previous error message.");
                break;
            case SetAIMessage.TAG:
                reply = setAI(new SetAIMessage(game, element));
                break;
            case SetCurrentPlayerMessage.TAG:
                reply = setCurrentPlayer(new SetCurrentPlayerMessage(game, element));
                break;

            // Since we're the server, we can see everything.
            // Therefore most of these messages are useless.
            // This may change one day.
            case AddPlayerMessage.TAG:
            case "animateMove":
            case "animateAttack":
            case AssignTradeRouteMessage.TAG:
            case ChatMessage.TAG:
            case TrivialMessage.CLOSE_MENUS_TAG:
            case DeleteTradeRouteMessage.TAG:
            case DisconnectMessage.TAG:
            case ErrorMessage.TAG:
            case "featureChange":
            case GameEndedMessage.TAG:
            case LogoutMessage.TAG:
            case NativeGiftMessage.TAG:
            case NewTurnMessage.TAG:
            case NewTradeRouteMessage.TAG:
            case "remove":
            case ScoutSpeakToChiefMessage.TAG:
            case SetDeadMessage.TAG:
            case SetStanceMessage.TAG:
            case TrivialMessage.START_GAME_TAG:
            case UpdateMessage.TAG:
                break;
            default:
                logger.warning("Unknown message type: " + tag);
                break;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "AI input handler for " + getMyPlayer()
                + " caught error handling " + tag, e);
        }
        return reply;
    }

    // Individual message handlers

    /**
     * Handles a "chooseFoundingFather"-message.
     * Only meaningful for AIPlayer types that implement selectFoundingFather.
     *
     * @param message The {@code ChooseFoundingFatherMessage} to process.
     * @return An {@code Element} containing the response/s.
     */
    private Element chooseFoundingFather(ChooseFoundingFatherMessage message) {
        final AIPlayer aiPlayer = getAIPlayer();
        final List<FoundingFather> fathers = message.getFathers(getGame());

        FoundingFather ff = aiPlayer.selectFoundingFather(fathers);
        if (ff != null) message.setFather(ff);
        logger.finest(aiPlayer.getId() + " chose founding father: " + ff);
        return message.toXMLElement();
    }

    /**
     * Handles an "diplomacy"-message.
     *
     * @param message The {@code DiplomacyMessage} to process.
     * @return An {@code Element} containing the response/s.
     */
    private Element diplomacy(DiplomacyMessage message) {
        final Game game = getGame();
        final DiplomaticTrade agreement = message.getAgreement();

        // Shortcut if no negotiation is required
        if (agreement.getStatus() != DiplomaticTrade.TradeStatus.PROPOSE_TRADE)
            return null;
        StringBuilder sb = new StringBuilder(256);
        sb.append("AI Diplomacy: ").append(agreement);
        TradeStatus status = getAIPlayer().acceptDiplomaticTrade(agreement);
        agreement.setStatus(status);
        sb.append(" -> ").append(agreement);
        logger.fine(sb.toString());

        // Note: transposing {our,other} here, the message is in sender sense
        return new DiplomacyMessage(message.getOtherFCGO(game),
                                    message.getOurFCGO(game), agreement)
            .toXMLElement();
    }

    /**
     * Replies to a first contact offer.
     *
     * @param message The {@code FirstContactMessage} to process.
     * @return An {@code Element} containing the response/s.
     */
    private Element firstContact(FirstContactMessage message) {
        return message.setResult(true).toXMLElement();
    }

    /**
     * Replies to fountain of youth offer.
     *
     * @param message The {@code FountainOfYouthMessage} to process.
     * @param element The {@code Element} to process.
     * @return Null.
     */
    private Element fountainOfYouth(FountainOfYouthMessage message) {
        final AIPlayer aiPlayer = getAIPlayer();
        final int n = message.getMigrants();

        for (int i = 0; i < n; i++) AIMessage.askEmigrate(aiPlayer, 0);
        return null;
    }

    /**
     * Handles an "indianDemand"-message.
     *
     * @param message The {@code IndianDemandMessage} to process.
     * @return The original message with the acceptance state set if querying
     *     the colony player (result == null), or null if reporting the final
     *     result to the native player (result != null).
     */
    private Element indianDemand(IndianDemandMessage message) {
        final Game game = getGame();
        final AIPlayer aiPlayer = getAIPlayer();
        final Unit unit = message.getUnit(game);
        final Colony colony = message.getColony(game);
        final GoodsType type = message.getType(game);
        final int amount = message.getAmount();

        Boolean result = aiPlayer.indianDemand(unit, colony, type, amount,
                                               message.getResult());
        if (result == null) return null;
        message.setResult(result);
        logger.finest("AI handling native demand by " + unit
            + " at " + colony.getName() + " result: " + result);
        return message.toXMLElement();
    }

    /**
     * Replies to loot cargo offer.
     *
     * @param message The {@code LootCargoMessage} to process.
     * @return Null.
     */
    private Element lootCargo(LootCargoMessage message) {
        final Game game = getGame();
        final Market market = getMyPlayer().getMarket();
        final Unit unit = message.getUnit(game);

        List<Goods> goods = sort(message.getGoods(),
                                 market.getSalePriceComparator());
        List<Goods> loot = new ArrayList<>();
        int space = unit.getSpaceLeft();
        while (!goods.isEmpty()) {
            Goods g = goods.remove(0);
            if (g.getSpaceTaken() > space) continue; // Approximate
            loot.add(g);
            space -= g.getSpaceTaken();
        }
        AIMessage.askLoot(getAIUnit(unit), message.getDefenderId(), loot);
        return null;
    }

    /**
     * Handles a "monarchAction"-message.
     *
     * @param message The {@code MonarchActionMessage} to process.
     * @return An {@code Element} containing the response/s.
     */
    private Element monarchAction(MonarchActionMessage message) {
        final MonarchAction action = message.getAction();

        boolean accept;
        switch (action) {
        case RAISE_TAX_WAR: case RAISE_TAX_ACT:
            accept = getAIPlayer().acceptTax(message.getTax());
            message.setResult(accept);
            logger.finest("AI player monarch action " + action
                          + " = " + accept);
            break;

        case MONARCH_MERCENARIES: case HESSIAN_MERCENARIES:
            accept = getAIPlayer().acceptMercenaries();
            message.setResult(accept);
            logger.finest("AI player monarch action " + action
                          + " = " + accept);
            break;

        default:
            logger.finest("AI player ignoring monarch action " + action);
            return null;
        }
        return message.toXMLElement();
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The {@code Connection} the element arrived on.
     * @param element The {@code Element} to process.
     * @return An {@code Element} containing the response/s.
     */
    public Element multiple(Connection connection, Element element) {
        return new MultipleMessage(element).applyHandler(this, connection);
    }

    /**
     * Handle an incoming nation summary.
     *
     * @param message The {@code NationSummaryMessage} to process.
     * @return Null.
     */
    private Element nationSummary(NationSummaryMessage message) {
        final AIPlayer aiPlayer = getAIPlayer();
        final Player player = aiPlayer.getPlayer();
        final Player other = message.getPlayer(getGame());
        final NationSummary ns = message.getNationSummary();

        player.putNationSummary(other, ns);
        logger.info("Updated nation summary of " + other.getSuffix()
            + " for AI " + player.getSuffix());
        return null;
    }

    /**
     * Handle a native trade message.
     *
     * @param message The {@code NativeTradeMessage} to process.
     * @return An {@code Element} containing the response/s.
     */
    private Element nativeTrade(NativeTradeMessage message) {
        final NativeTrade nt = message.getNativeTrade();
        final NativeTrade.NativeTradeAction action = message.getAction();

        return new NativeTradeMessage(getAIPlayer().handleTrade(action, nt),
                                      nt).toXMLElement();
    }

    /**
     * Replies to offer to name the new land.
     *
     * @param message The {@code NewLandNameMessage} to process.
     * @return An {@code Element} containing the response/s.
     */
    private Element newLandName(NewLandNameMessage message) {
        return message.toXMLElement();
    }

    /**
     * Replies to offer to name a new region name.
     *
     * @param message The {@code NewRegionNameMessage} to process.
     * @return An {@code Element} containing the response/s.
     */
    private Element newRegionName(NewRegionNameMessage message) {
        return message.toXMLElement();
    }

    /**
     * Handle a "setAI"-message.
     *
     * @param message The {@code SetAIMessage} to process.
     * @return Null.
     */
    private Element setAI(SetAIMessage message) {
        final Player p = message.getPlayer(getGame());
        final boolean ai = message.getAI();

        if (p != null) p.setAI(ai);
        return null;
    }

    /**
     * Handles a "setCurrentPlayer"-message.
     *
     * @param message The {@code SetCurrentPlayerMessage} to process.
     * @return Null.
     */
    private Element setCurrentPlayer(SetCurrentPlayerMessage message) {
        final Player currentPlayer = message.getPlayer(getGame());

        if (currentPlayer != null
            && getMyPlayer().getId().equals(currentPlayer.getId())) {
            String name = getMyPlayer().getName();
            logger.finest("Starting new Thread for " + name);
            name = FreeCol.SERVER_THREAD + "AIPlayer (" + name + ")";
            new Thread(name) {
                @Override
                public void run() {
                    try {
                        getAIPlayer().startWorking();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "AI player failed while working!", e);
                    }
                    AIMessage.askEndTurn(getAIPlayer());
                }
            }.start();
        }
        return null;
    }
}
