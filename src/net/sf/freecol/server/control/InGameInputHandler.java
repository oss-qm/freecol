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

import net.sf.freecol.common.networking.AbandonColonyMessage;
import net.sf.freecol.common.networking.AskSkillMessage;
import net.sf.freecol.common.networking.AssignTeacherMessage;
import net.sf.freecol.common.networking.AssignTradeRouteMessage;
import net.sf.freecol.common.networking.AttackMessage;
import net.sf.freecol.common.networking.BuildColonyMessage;
import net.sf.freecol.common.networking.CashInTreasureTrainMessage;
import net.sf.freecol.common.networking.ChangeStateMessage;
import net.sf.freecol.common.networking.ChangeWorkImprovementTypeMessage;
import net.sf.freecol.common.networking.ChangeWorkTypeMessage;
import net.sf.freecol.common.networking.ChooseFoundingFatherMessage;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.ClearSpecialityMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DeclareIndependenceMessage;
import net.sf.freecol.common.networking.DeclineMoundsMessage;
import net.sf.freecol.common.networking.DeliverGiftMessage;
import net.sf.freecol.common.networking.DeleteTradeRouteMessage;
import net.sf.freecol.common.networking.DemandTributeMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.DisbandUnitMessage;
import net.sf.freecol.common.networking.DisembarkMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.EmbarkMessage;
import net.sf.freecol.common.networking.EmigrateUnitMessage;
import net.sf.freecol.common.networking.EquipForRoleMessage;
import net.sf.freecol.common.networking.FirstContactMessage;
import net.sf.freecol.common.networking.HighScoreMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.JoinColonyMessage;
import net.sf.freecol.common.networking.LearnSkillMessage;
import net.sf.freecol.common.networking.LoadGoodsMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MissionaryMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.MoveMessage;
import net.sf.freecol.common.networking.MoveToMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NativeGiftMessage;
import net.sf.freecol.common.networking.NativeTradeMessage;
import net.sf.freecol.common.networking.NewLandNameMessage;
import net.sf.freecol.common.networking.NewRegionNameMessage;
import net.sf.freecol.common.networking.NewTradeRouteMessage;
import net.sf.freecol.common.networking.PayArrearsMessage;
import net.sf.freecol.common.networking.PayForBuildingMessage;
import net.sf.freecol.common.networking.PutOutsideColonyMessage;
import net.sf.freecol.common.networking.RearrangeColonyMessage;
import net.sf.freecol.common.networking.RenameMessage;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.ScoutIndianSettlementMessage;
import net.sf.freecol.common.networking.SetBuildQueueMessage;
import net.sf.freecol.common.networking.SetCurrentStopMessage;
import net.sf.freecol.common.networking.SetDestinationMessage;
import net.sf.freecol.common.networking.SetGoodsLevelsMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import net.sf.freecol.common.networking.TrainUnitInEuropeMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UnloadGoodsMessage;
import net.sf.freecol.common.networking.UpdateTradeRouteMessage;
import net.sf.freecol.common.networking.WorkMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives while
 * {@link net.sf.freecol.server.FreeColServer.ServerState#IN_GAME in game}.
 */
public final class InGameInputHandler extends ServerInputHandler {


    /**
     * Create a new server in-game input handler.
     *
     * Note: all the handler lamdbas call getGame() because the game
     * is not necessarily available when the constructor is called.
     *
     * @param freeColServer The main server object.
     */
    public InGameInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
    }

    @Override
    public Element handleElement(Connection c, Element e)
            throws UnknownServiceException {
        String tag = e.getTagName().intern();
        System.out.println("InGameInputHandler: "+tag);
        switch (tag) {
            case AbandonColonyMessage.TAG:
                return handler(true, c, new AbandonColonyMessage(getGame(), e));

            case AskSkillMessage.TAG:
                return handler(true, c, new AskSkillMessage(getGame(), e));

            case AssignTeacherMessage.TAG:
                return handler(true, c, new AssignTeacherMessage(getGame(), e));

            case AssignTradeRouteMessage.TAG:
                return handler(true, c, new AssignTradeRouteMessage(getGame(), e));

            case AttackMessage.TAG:
                return handler(true, c, new AttackMessage(getGame(), e));

            case BuildColonyMessage.TAG:
                return handler(true, c, new BuildColonyMessage(getGame(), e));

            case CashInTreasureTrainMessage.TAG:
                return handler(true, c, new CashInTreasureTrainMessage(getGame(), e));

            case ChangeStateMessage.TAG:
                return handler(true, c, new ChangeStateMessage(getGame(), e));

            case ChangeWorkImprovementTypeMessage.TAG:
                return handler(true, c, new ChangeWorkImprovementTypeMessage(getGame(), e));

            case ChangeWorkTypeMessage.TAG:
                return handler(true, c, new ChangeWorkTypeMessage(getGame(), e));

            case ChooseFoundingFatherMessage.TAG:
                return handler(true, c, new ChooseFoundingFatherMessage(getGame(), e));

            case ClaimLandMessage.TAG:
                return handler(true, c, new ClaimLandMessage(getGame(), e));

            case ClearSpecialityMessage.TAG:
                return handler(true, c, new ClearSpecialityMessage(getGame(), e));

            case TrivialMessage.CONTINUE_TAG:
                return handler(false, c, TrivialMessage.CONTINUE_MESSAGE);

            case DeclareIndependenceMessage.TAG:
                return handler(true, c, new DeclareIndependenceMessage(getGame(), e));

            case DeclineMoundsMessage.TAG:
                return handler(true, c, new DeclineMoundsMessage(getGame(), e));

            case DeliverGiftMessage.TAG:
                return handler(true, c, new DeliverGiftMessage(getGame(), e));

            case DeleteTradeRouteMessage.TAG:
                return handler(true, c, new DeleteTradeRouteMessage(getGame(), e));

            case DemandTributeMessage.TAG:
                return handler(true, c, new DemandTributeMessage(getGame(), e));

            case DiplomacyMessage.TAG:
                return handler(false, c, new DiplomacyMessage(getGame(), e));

            case DisbandUnitMessage.TAG:
                return handler(true, c, new DisbandUnitMessage(getGame(), e));

            case DisembarkMessage.TAG:
                return handler(true, c, new DisembarkMessage(getGame(), e));

            case EmbarkMessage.TAG:
                return handler(true, c, new EmbarkMessage(getGame(), e));

            case EmigrateUnitMessage.TAG:
                return handler(true, c, new EmigrateUnitMessage(getGame(), e));

            case TrivialMessage.END_TURN_TAG:
                return handler(true, c, TrivialMessage.END_TURN_MESSAGE);

            case TrivialMessage.ENTER_REVENGE_MODE_TAG:
                return handler(false, c, TrivialMessage.ENTER_REVENGE_MODE_MESSAGE);

            case EquipForRoleMessage.TAG:
                return handler(true, c, new EquipForRoleMessage(getGame(), e));

            case FirstContactMessage.TAG:
                return handler(false, c, new FirstContactMessage(getGame(), e));

            case InciteMessage.TAG:
                return handler(true, c, new InciteMessage(getGame(), e));

            case IndianDemandMessage.TAG:
                return handler(false, c, new IndianDemandMessage(getGame(), e));

            case HighScoreMessage.TAG:
                return handler(false, c, new HighScoreMessage(getGame(), e));

            case JoinColonyMessage.TAG:
                return handler(true, c, new JoinColonyMessage(getGame(), e));

            case LearnSkillMessage.TAG:
                return handler(true, c, new LearnSkillMessage(getGame(), e));

            case LoadGoodsMessage.TAG:
                return handler(true, c, new LoadGoodsMessage(getGame(), e));

            case LootCargoMessage.TAG:
                return handler(false, c, new LootCargoMessage(getGame(), e));

            case MissionaryMessage.TAG:
                return handler(true, c, new MissionaryMessage(getGame(), e));

            case MonarchActionMessage.TAG:
                return handler(true, c, new MonarchActionMessage(getGame(), e));

            case MoveMessage.TAG:
                return handler(true, c, new MoveMessage(getGame(), e));

            case MoveToMessage.TAG:
                return handler(true, c, new MoveToMessage(getGame(), e));

            case NationSummaryMessage.TAG:
                return handler(false, c, new NationSummaryMessage(getGame(), e));

            case NativeGiftMessage.TAG:
                return handler(true, c, new NativeGiftMessage(getGame(), e));

            case NativeTradeMessage.TAG:
                return handler(false, c, new NativeTradeMessage(getGame(), e));

            case NewLandNameMessage.TAG:
                return handler(false, c, new NewLandNameMessage(getGame(), e));

            case NewRegionNameMessage.TAG:
                return handler(false, c, new NewRegionNameMessage(getGame(), e));

            case NewTradeRouteMessage.TAG:
                return handler(true, c, new NewTradeRouteMessage(getGame(), e));

            case PayArrearsMessage.TAG:
                return handler(true, c, new PayArrearsMessage(getGame(), e));

            case PayForBuildingMessage.TAG:
                return handler(true, c, new PayForBuildingMessage(getGame(), e));

            case PutOutsideColonyMessage.TAG:
                return handler(true, c, new PutOutsideColonyMessage(getGame(), e));

            case RearrangeColonyMessage.TAG:
                return handler(true, c, new RearrangeColonyMessage(getGame(), e));

            case RenameMessage.TAG:
                return handler(true, c, new RenameMessage(getGame(), e));

            case TrivialMessage.RETIRE_TAG:
                return handler(false, c, TrivialMessage.RETIRE_MESSAGE);

            case ScoutIndianSettlementMessage.TAG:
                return handler(true, c, new ScoutIndianSettlementMessage(getGame(), e));

            case ScoutSpeakToChiefMessage.TAG:
                return handler(true, c, new ScoutSpeakToChiefMessage(getGame(), e));

            case SetBuildQueueMessage.TAG:
                return handler(true, c, new SetBuildQueueMessage(getGame(), e));

            case SetCurrentStopMessage.TAG:
                return handler(false, c, new SetCurrentStopMessage(getGame(), e));

            case SetDestinationMessage.TAG:
                return handler(false, c, new SetDestinationMessage(getGame(), e));

            case SetGoodsLevelsMessage.TAG:
                return handler(true, c, new SetGoodsLevelsMessage(getGame(), e));

            case SpySettlementMessage.TAG:
                return handler(false, c, new SpySettlementMessage(getGame(), e));

            case TrainUnitInEuropeMessage.TAG:
                return handler(true, c, new TrainUnitInEuropeMessage(getGame(), e));

            case UnloadGoodsMessage.TAG:
                return handler(true, c, new UnloadGoodsMessage(getGame(), e));

            case UpdateTradeRouteMessage.TAG:
                return handler(false, c, new UpdateTradeRouteMessage(getGame(), e));

            case WorkMessage.TAG:
                return handler(true, c, new WorkMessage(getGame(), e));

            case MultipleMessage.TAG:
                return (new MultipleMessage(getGame(), e).handle(getFreeColServer(), c));

            default:
                return super.handleElement(c, e);
        }
    }
}
