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

package net.sf.freecol.common.model;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * The {@code Ability} class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public final class Ability extends Feature {

    public static final String TAG = "ability";

    /**
     * The ability to add the current tax as a bonus to the production
     * of bells.  Provided by the {@link FoundingFather} Thomas Paine.
     */
    public static final String ADD_TAX_TO_BELLS
        = "model.ability.addTaxToBells";

    /** The ability to always receive a peace offer (Franklin). */
    public static final String ALWAYS_OFFERED_PEACE
        = "model.ability.alwaysOfferedPeace";

    /** The ability to ambush other units. */
    public static final String AMBUSH_BONUS
        = "model.ability.ambushBonus";

    /** The susceptibility to ambush from other units. */
    public static final String AMBUSH_PENALTY
        = "model.ability.ambushPenalty";

    /** Terrain in which an ambush may occur. */
    public static final String AMBUSH_TERRAIN
        = "model.ability.ambushTerrain";

    /** Equipment type ability denoting the holder is armed. */
    public static final String ARMED
        = "model.ability.armed";

    /** The ability of a colony to automatocally arm defenders (Revere). */
    public static final String AUTOMATIC_EQUIPMENT
        = "model.ability.automaticEquipment";

    /** The ability to automatically promote combat winners (Washington). */
    public static final String AUTOMATIC_PROMOTION
        = "model.ability.automaticPromotion";

    /**
     * The ability of certain buildings (e.g. the stables) to produce
     * goods even if no units are present.
     */
    public static final String AUTO_PRODUCTION
        = "model.ability.autoProduction";

    /**
     * The ability of certain buildings (e.g. the stables) to avoid
     * producing more goods than the colony can store, which would
     * normally go to waste.
     */
    public static final String AVOID_EXCESS_PRODUCTION
        = "model.ability.avoidExcessProduction";

    /** The ability for better foreign affairs reporting (deWitt). */
    public static final String BETTER_FOREIGN_AFFAIRS_REPORT
        = "model.ability.betterForeignAffairsReport";

    /** The ability of a unit to bombard other units. */
    public static final String BOMBARD
        = "model.ability.bombard";

    /** The ability of a colony to bombard ships. */
    public static final String BOMBARD_SHIPS
        = "model.ability.bombardShips";

    /**
     * The ability to be born in a Colony.  Only Units with this
     * ability can be produced by a Colony.
     */
    public static final String BORN_IN_COLONY
        = "model.ability.bornInColony";

    /**
     * The ability to be born in an IndianSettlement.  Only Units with
     * this ability can be produced by an IndianSettlement.
     */
    public static final String BORN_IN_INDIAN_SETTLEMENT
        = "model.ability.bornInIndianSettlement";

    /**
     * The ability to build {@link BuildableType}s, such as units.  The
     * shipyard provides the ability to build ships, for example.
     */
    public static final String BUILD
        = "model.ability.build";

    /**
     * The ability to build a customs house.  Yes this is misspelled.
     */
    public static final String BUILD_CUSTOM_HOUSE
        = "model.ability.buildCustomHouse";

    /** The ability to build a factories. */
    public static final String BUILD_FACTORY
        = "model.ability.buildFactory";

    /**
     * The ability of certain unarmed units to be captured by another
     * player's units. Units lacking this ability (e.g. braves) will
     * be destroyed instead.
     */
    public static final String CAN_BE_CAPTURED
        = "model.ability.canBeCaptured";

    /** The ability of certain units to be equipped with tools, muskets, etc. */
    public static final String CAN_BE_EQUIPPED
        = "model.ability.canBeEquipped";

    /** The ability of a player to recruit units. */
    public static final String CAN_RECRUIT_UNIT
        = "model.ability.canRecruitUnit";

    /** The ability of certain armed units to capture equipment.*/
    public static final String CAPTURE_EQUIPMENT
        = "model.ability.captureEquipment";

    /**
     * The ability of certain units (e.g. privateers) to capture goods
     * carried by another player's units.
     */
    public static final String CAPTURE_GOODS
        = "model.ability.captureGoods";

    /** The ability of certain armed units to capture another player's units.*/
    public static final String CAPTURE_UNITS
        = "model.ability.captureUnits";

    /** The ability of certain units (e.g. wagon trains) to carry goods. */
    public static final String CARRY_GOODS
        = "model.ability.carryGoods";

    /**
     * The ability of certain units (e.g. treasure trains) to carry
     * treasure.
     */
    public static final String CARRY_TREASURE
        = "model.ability.carryTreasure";

    /** The ability of certain units (e.g. ships) to carry other units. */
    public static final String CARRY_UNITS
        = "model.ability.carryUnits";

    /** Restrict some buildings to only be buildable on the coast. */
    public static final String COASTAL_ONLY
        = "model.ability.coastalOnly";

    /**
     * The ability of certain consumers (e.g. BuildQueues) to consume
     * a large amount of goods at once instead of turn by turn.
     */
    public static final String CONSUME_ALL_OR_NOTHING
        = "model.ability.consumeAllOrNothing";

    /** The ability of customs houses to trade with other players. */
    public static final String CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES
        = "model.ability.customHouseTradesWithForeignCountries";

    /** The ability to demand tribute even when unarmed. */
    public static final String DEMAND_TRIBUTE
        = "model.ability.demandTribute";

    /** Units with this ability are demoted on losing all equipment. */
    public static final String DEMOTE_ON_ALL_EQUIPMENT_LOST
        = "model.ability.demoteOnAllEquipLost";

    /** The ability to denounce heresy. */
    public static final String DENOUNCE_HERESY
        = "model.ability.denounceHeresy";

    /** Units with this ability die on losing all equipment. */
    public static final String DISPOSE_ON_ALL_EQUIPMENT_LOST
        = "model.ability.disposeOnAllEquipLost";

    /** Units with this ability die on losing a combat. */
    public static final String DISPOSE_ON_COMBAT_LOSS
        = "model.ability.disposeOnCombatLoss";

    /** The ability to bless a missionary. */
    public static final String DRESS_MISSIONARY
        = "model.ability.dressMissionary";

    /** The ability to elect founding fathers. */
    public static final String ELECT_FOUNDING_FATHER
        = "model.ability.electFoundingFather";

    /** The ability to establish a mission. */
    public static final String ESTABLISH_MISSION
        = "model.ability.establishMission";

    /** The ability to evade naval attack. */
    public static final String EVADE_ATTACK
        = "model.ability.evadeAttack";

    /**
     * The ability of certain units to work as missionaries more
     * effectively.
     */
    public static final String EXPERT_MISSIONARY
        = "model.ability.expertMissionary";

    /** The ability of certain units to build TileImprovements faster. */
    public static final String EXPERT_PIONEER
        = "model.ability.expertPioneer";

    /** The ability of certain units to work as scouts more effectively. */
    public static final String EXPERT_SCOUT
        = "model.ability.expertScout";

    /** The ability of certain units to work as soldiers more effectively. */
    public static final String EXPERT_SOLDIER
        = "model.ability.expertSoldier";

    /**
     * The somewhat controversial ability of expert units in factory
     * level buildings to produce a certain amount of goods even when
     * no raw materials are available.  Allegedly, this is a feature of
     * the original game.
     */
    public static final String EXPERTS_USE_CONNECTIONS
        = "model.ability.expertsUseConnections";

    /** The ability to export goods to Europe directly. */
    public static final String EXPORT
        = "model.ability.export";

    /** The ability of a unit to found a colony. */
    public static final String FOUND_COLONY
        = "model.ability.foundColony";

    /** The ability of a unit to be found in a lost city. */
    public static final String FOUND_IN_LOST_CITY
        = "model.ability.foundInLostCity";

    /** The ability of a player to found colonies. */
    public static final String FOUNDS_COLONIES
        = "model.ability.foundsColonies";

    /** The ability of a colony which is a port. */
    public static final String HAS_PORT
        = "model.ability.hasPort";

    /** The ability to ignore the monarchs wars. */
    public static final String IGNORE_EUROPEAN_WARS
        = "model.ability.ignoreEuropeanWars";

    /** The ability of a unit to make terrain improvements. */
    public static final String IMPROVE_TERRAIN
        = "model.ability.improveTerrain";

    /** The ability to incite the natives. */
    public static final String INCITE_NATIVES
        = "model.ability.inciteNatives";

    /**
     * The ability denoting that a declaration of independence has
     * been made.
     */
    public static final String INDEPENDENCE_DECLARED
        = "model.ability.independenceDeclared";

    /**
     * The ability denoting that this is an independent nation.
     * Note: this differs from INDEPENDENCE_DECLARED in that
     * the REF is also (representing) an independent nation.
     */
    public static final String INDEPENDENT_NATION
        = "model.ability.independentNation";

    /** Units with this ability can be chosen as mercenaries support units. */
    public static final String MERCENARY_UNIT
        = "model.ability.mercenaryUnit";

    /** Equipment type ability denoting the holder is mounted. */
    public static final String MOUNTED
        = "model.ability.mounted";

    /** The ability to move to Europe from a tile. */
    public static final String MOVE_TO_EUROPE
        = "model.ability.moveToEurope";

    /** The ability to attack multiple times. */
    public static final String MULTIPLE_ATTACKS
        = "model.ability.multipleAttacks";

    /** The ability of being a native unit. */
    public static final String NATIVE
        = "model.ability.native";

    /** The ability of ships to move across water tiles. */
    public static final String NAVAL_UNIT
        = "model.ability.navalUnit";

    /** The ability to engage in diplomatic negotiation. */
    public static final String NEGOTIATE
        = "model.ability.negotiate";

    /** Units with this property are persons, not a ship or wagon etc. */
    public static final String PERSON
        = "model.ability.person";

    /** The ability to pillage unprotected colonies. */
    public static final String PILLAGE_UNPROTECTED_COLONY
        = "model.ability.pillageUnprotectedColony";

    /**
     * The ability of certain units (e.g. privateers) to attack and
     * plunder another player's units without causing war.
     */
    public static final String PIRACY
        = "model.ability.piracy";

    /**
     * An ability that enhances the treasure plundered from native
     * settlements.
     */
    public static final String PLUNDER_NATIVES
        = "model.ability.plunderNatives";

    /** The ability to produce goods (e.g. fish) on water tiles. */
    public static final String PRODUCE_IN_WATER
        = "model.ability.produceInWater";

    /** Units with this ability can be added to the REF. */
    public static final String REF_UNIT
        = "model.ability.refUnit";

    /** The ability to repair certain units. */
    public static final String REPAIR_UNITS
        = "model.ability.repairUnits";

    /** A national ability required to generate a REF. */
    public static final String ROYAL_EXPEDITIONARY_FORCE
        = "model.ability.royalExpeditionaryForce";

    /** LCRs always yield positive results (deSoto). */
    public static final String RUMOURS_ALWAYS_POSITIVE
        = "model.ability.rumoursAlwaysPositive";

    /** The ability to see all colonies (Coronado). */
    public static final String SEE_ALL_COLONIES
        = "model.ability.seeAllColonies";

    /** The ability to select recruits (Brewster). */
    public static final String SELECT_RECRUIT
        = "model.ability.selectRecruit";

    /** The ability to speak to a native settlement chief. */
    public static final String SPEAK_WITH_CHIEF
        = "model.ability.speakWithChief";

    /** The ability to spy on a colony. */
    public static final String SPY_ON_COLONY
        = "model.ability.spyOnColony";

    /**
     * Units with this ability can be chosen as support units from
     * the crown.
     */
    public static final String SUPPORT_UNIT
        = "model.ability.supportUnit";

    /** Buildings with this ability can be used to teach. */
    public static final String TEACH
        = "model.ability.teach";

    /** The ability to trade with foreign colonies (deWitt). */
    public static final String TRADE_WITH_FOREIGN_COLONIES
        = "model.ability.tradeWithForeignColonies";

    /** Undead units have this ability. */
    public static final String UNDEAD
        = "model.ability.undead";

    /** Upgrade converts to free colonist with Casas. */
    public static final String UPGRADE_CONVERT
        = "model.ability.upgradeConvert";

    public static final List<Ability> EMPTY_LIST = new ArrayList<>();

    /** The ability value. */
    private boolean value = true;

    public enum ID {
        ALWAYS_OFFERED_PEACE,
        ADD_TAX_TO_BELLS,
        AMBUSH_BONUS,
        AMBUSH_PENALTY,
        AMBUSH_TERRAIN,
        ARMED,
        AUTOMATIC_EQUIPMENT,
        AUTOMATIC_PROMOTION,
        AUTO_PRODUCTION,
        AVOID_EXCESS_PRODUCTION,
        BETTER_FOREIGN_AFFAIRS_REPORT,
        BOMBARD,
        BOMBARD_SHIPS,
        BORN_IN_COLONY,
        BORN_IN_INDIAN_SETTLEMENT,
        BUILD,
        BUILD_CUSTOM_HOUSE,
        BUILD_FACTORY,
        CAN_BE_CAPTURED,
        CAN_BE_EQUIPPED,
        CAN_RECRUIT_UNIT,
        CAPTURE_EQUIPMENT,
        CAPTURE_GOODS,
        CAPTURE_UNITS,
        CARRY_GOODS,
        CARRY_TREASURE,
        CARRY_UNITS,
        COASTAL_ONLY,
        CONSUME_ALL_OR_NOTHING,
        CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES,
        DEMAND_TRIBUTE,
        DEMOTE_ON_ALL_EQUIPMENT_LOST,
        DENOUNCE_HERESY,
        DISPOSE_ON_ALL_EQUIPMENT_LOST,
        DISPOSE_ON_COMBAT_LOSS,
        DRESS_MISSIONARY,
        ELECT_FOUNDING_FATHER,
        ESTABLISH_MISSION,
        EVADE_ATTACK,
        EXPERT_MISSIONARY,
        EXPERT_PIONEER,
        EXPERT_SCOUT,
        EXPERT_SOLDIER,
        EXPERTS_USE_CONNECTIONS,
        EXPORT,
        FOUND_COLONY,
        FOUND_IN_LOST_CITY,
        FOUNDS_COLONIES,
        HAS_PORT,
        IGNORE_EUROPEAN_WARS,
        IMPROVE_TERRAIN,
        INCITE_NATIVES,
        INDEPENDENCE_DECLARED,
        INDEPENDENT_NATION,
        MERCENARY_UNIT,
        MOUNTED,
        MOVE_TO_EUROPE,
        MULTIPLE_ATTACKS,
        NATIVE,
        NAVAL_UNIT,
        NEGOTIATE,
        PERSON,
        PILLAGE_UNPROTECTED_COLONY,
        PIRACY,
        PLUNDER_NATIVES,
        PRODUCE_IN_WATER,
        REF_UNIT,
        REPAIR_UNITS,
        ROYAL_EXPEDITIONARY_FORCE,
        RUMOURS_ALWAYS_POSITIVE,
        SEE_ALL_COLONIES,
        SELECT_RECRUIT,
        SPEAK_WITH_CHIEF,
        SPY_ON_COLONY,
        SUPPORT_UNIT,
        TEACH,
        TRADE_WITH_FOREIGN_COLONIES,
        UNDEAD,
        UPGRADE_CONVERT,
    };

    public static java.util.Map<ID,String> mapIdName = new HashMap<>();
    public static java.util.Map<String,ID> mapNameId = new HashMap<>();

    static {
        mapIdName.put(ID.ALWAYS_OFFERED_PEACE,				ALWAYS_OFFERED_PEACE);
        mapIdName.put(ID.ADD_TAX_TO_BELLS,				ADD_TAX_TO_BELLS);
        mapIdName.put(ID.AMBUSH_BONUS,					AMBUSH_BONUS);
        mapIdName.put(ID.AMBUSH_PENALTY,				AMBUSH_PENALTY);
        mapIdName.put(ID.AMBUSH_TERRAIN,				AMBUSH_TERRAIN);
        mapIdName.put(ID.ARMED,						ARMED);
        mapIdName.put(ID.AUTOMATIC_EQUIPMENT,				AUTOMATIC_EQUIPMENT);
        mapIdName.put(ID.AUTOMATIC_PROMOTION,				AUTOMATIC_PROMOTION);
        mapIdName.put(ID.AUTO_PRODUCTION,				AUTO_PRODUCTION);
        mapIdName.put(ID.AVOID_EXCESS_PRODUCTION,			AVOID_EXCESS_PRODUCTION);
        mapIdName.put(ID.BETTER_FOREIGN_AFFAIRS_REPORT,			BETTER_FOREIGN_AFFAIRS_REPORT);
        mapIdName.put(ID.BOMBARD,					BOMBARD);
        mapIdName.put(ID.BOMBARD_SHIPS,					BOMBARD_SHIPS);
        mapIdName.put(ID.BORN_IN_COLONY,				BORN_IN_COLONY);
        mapIdName.put(ID.BORN_IN_INDIAN_SETTLEMENT,			BORN_IN_INDIAN_SETTLEMENT);
        mapIdName.put(ID.BUILD,						BUILD);
        mapIdName.put(ID.BUILD_CUSTOM_HOUSE,				BUILD_CUSTOM_HOUSE);
        mapIdName.put(ID.BUILD_FACTORY,					BUILD_FACTORY);
        mapIdName.put(ID.CAN_BE_CAPTURED,				CAN_BE_CAPTURED);
        mapIdName.put(ID.CAN_BE_EQUIPPED,				CAN_BE_EQUIPPED);
        mapIdName.put(ID.CAN_RECRUIT_UNIT,				CAN_RECRUIT_UNIT);
        mapIdName.put(ID.CAPTURE_EQUIPMENT,				CAPTURE_EQUIPMENT);
        mapIdName.put(ID.CAPTURE_GOODS,					CAPTURE_GOODS);
        mapIdName.put(ID.CAPTURE_UNITS,					CAPTURE_UNITS);
        mapIdName.put(ID.CARRY_GOODS,					CARRY_GOODS);
        mapIdName.put(ID.CARRY_TREASURE,				CARRY_TREASURE);
        mapIdName.put(ID.CARRY_UNITS,					CARRY_UNITS);
        mapIdName.put(ID.COASTAL_ONLY,					COASTAL_ONLY);
        mapIdName.put(ID.CONSUME_ALL_OR_NOTHING,			CONSUME_ALL_OR_NOTHING);
        mapIdName.put(ID.CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES,	CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES);
        mapIdName.put(ID.DEMAND_TRIBUTE,				DEMAND_TRIBUTE);
        mapIdName.put(ID.DEMOTE_ON_ALL_EQUIPMENT_LOST,			DEMOTE_ON_ALL_EQUIPMENT_LOST);
        mapIdName.put(ID.DENOUNCE_HERESY,				DENOUNCE_HERESY);
        mapIdName.put(ID.DISPOSE_ON_ALL_EQUIPMENT_LOST,			DISPOSE_ON_ALL_EQUIPMENT_LOST);
        mapIdName.put(ID.DISPOSE_ON_COMBAT_LOSS,			DISPOSE_ON_COMBAT_LOSS);
        mapIdName.put(ID.DRESS_MISSIONARY,				DRESS_MISSIONARY);
        mapIdName.put(ID.ELECT_FOUNDING_FATHER,				ELECT_FOUNDING_FATHER);
        mapIdName.put(ID.ESTABLISH_MISSION,				ESTABLISH_MISSION);
        mapIdName.put(ID.EVADE_ATTACK,					EVADE_ATTACK);
        mapIdName.put(ID.EXPERT_MISSIONARY,				EXPERT_MISSIONARY);
        mapIdName.put(ID.EXPERT_PIONEER,				EXPERT_PIONEER);
        mapIdName.put(ID.EXPERT_SCOUT,					EXPERT_SCOUT);
        mapIdName.put(ID.EXPERT_SOLDIER,				EXPERT_SOLDIER);
        mapIdName.put(ID.EXPERTS_USE_CONNECTIONS,			EXPERTS_USE_CONNECTIONS);
        mapIdName.put(ID.EXPORT,					EXPORT);
        mapIdName.put(ID.FOUND_COLONY,					FOUND_COLONY);
        mapIdName.put(ID.FOUND_IN_LOST_CITY,				FOUND_IN_LOST_CITY);
        mapIdName.put(ID.FOUNDS_COLONIES,				FOUNDS_COLONIES);
        mapIdName.put(ID.HAS_PORT,					HAS_PORT);
        mapIdName.put(ID.IGNORE_EUROPEAN_WARS,				IGNORE_EUROPEAN_WARS);
        mapIdName.put(ID.IMPROVE_TERRAIN,				IMPROVE_TERRAIN);
        mapIdName.put(ID.INCITE_NATIVES,				INCITE_NATIVES);
        mapIdName.put(ID.INDEPENDENCE_DECLARED,				INDEPENDENCE_DECLARED);
        mapIdName.put(ID.INDEPENDENT_NATION,				INDEPENDENT_NATION);
        mapIdName.put(ID.MERCENARY_UNIT,				MERCENARY_UNIT);
        mapIdName.put(ID.MOUNTED,					MOUNTED);
        mapIdName.put(ID.MOVE_TO_EUROPE,				MOVE_TO_EUROPE);
        mapIdName.put(ID.MULTIPLE_ATTACKS,				MULTIPLE_ATTACKS);
        mapIdName.put(ID.NATIVE,					NATIVE);
        mapIdName.put(ID.NAVAL_UNIT,					NAVAL_UNIT);
        mapIdName.put(ID.NEGOTIATE,					NEGOTIATE);
        mapIdName.put(ID.PERSON,					PERSON);
        mapIdName.put(ID.PILLAGE_UNPROTECTED_COLONY,			PILLAGE_UNPROTECTED_COLONY);
        mapIdName.put(ID.PIRACY,					PIRACY);
        mapIdName.put(ID.PLUNDER_NATIVES,				PLUNDER_NATIVES);
        mapIdName.put(ID.PRODUCE_IN_WATER,				PRODUCE_IN_WATER);
        mapIdName.put(ID.REF_UNIT,					REF_UNIT);
        mapIdName.put(ID.REPAIR_UNITS,					REPAIR_UNITS);
        mapIdName.put(ID.ROYAL_EXPEDITIONARY_FORCE,			ROYAL_EXPEDITIONARY_FORCE);
        mapIdName.put(ID.RUMOURS_ALWAYS_POSITIVE,			RUMOURS_ALWAYS_POSITIVE);
        mapIdName.put(ID.SEE_ALL_COLONIES,				SEE_ALL_COLONIES);
        mapIdName.put(ID.SELECT_RECRUIT,				SELECT_RECRUIT);
        mapIdName.put(ID.SPEAK_WITH_CHIEF,				SPEAK_WITH_CHIEF);
        mapIdName.put(ID.SPY_ON_COLONY,					SPY_ON_COLONY);
        mapIdName.put(ID.SUPPORT_UNIT,					SUPPORT_UNIT);
        mapIdName.put(ID.TEACH,						TEACH);
        mapIdName.put(ID.TRADE_WITH_FOREIGN_COLONIES,			TRADE_WITH_FOREIGN_COLONIES);
        mapIdName.put(ID.UNDEAD,					UNDEAD);
        mapIdName.put(ID.UPGRADE_CONVERT,				UPGRADE_CONVERT);

        mapNameId.put(ALWAYS_OFFERED_PEACE,				ID.ALWAYS_OFFERED_PEACE);
        mapNameId.put(ADD_TAX_TO_BELLS,					ID.ADD_TAX_TO_BELLS);
        mapNameId.put(AMBUSH_BONUS,					ID.AMBUSH_BONUS);
        mapNameId.put(AMBUSH_PENALTY,					ID.AMBUSH_PENALTY);
        mapNameId.put(AMBUSH_TERRAIN,					ID.AMBUSH_TERRAIN);
        mapNameId.put(ARMED,						ID.ARMED);
        mapNameId.put(AUTOMATIC_EQUIPMENT,				ID.AUTOMATIC_EQUIPMENT);
        mapNameId.put(AUTOMATIC_PROMOTION,				ID.AUTOMATIC_PROMOTION);
        mapNameId.put(AUTO_PRODUCTION,					ID.AUTO_PRODUCTION);
        mapNameId.put(AVOID_EXCESS_PRODUCTION,				ID.AVOID_EXCESS_PRODUCTION);
        mapNameId.put(BETTER_FOREIGN_AFFAIRS_REPORT,			ID.BETTER_FOREIGN_AFFAIRS_REPORT);
        mapNameId.put(BOMBARD,						ID.BOMBARD);
        mapNameId.put(BOMBARD_SHIPS,					ID.BOMBARD_SHIPS);
        mapNameId.put(BORN_IN_COLONY,					ID.BORN_IN_COLONY);
        mapNameId.put(BORN_IN_INDIAN_SETTLEMENT,			ID.BORN_IN_INDIAN_SETTLEMENT);
        mapNameId.put(BUILD,						ID.BUILD);
        mapNameId.put(BUILD_CUSTOM_HOUSE,				ID.BUILD_CUSTOM_HOUSE);
        mapNameId.put(BUILD_FACTORY,					ID.BUILD_FACTORY);
        mapNameId.put(CAN_BE_CAPTURED,					ID.CAN_BE_CAPTURED);
        mapNameId.put(CAN_BE_EQUIPPED,					ID.CAN_BE_EQUIPPED);
        mapNameId.put(CAN_RECRUIT_UNIT,					ID.CAN_RECRUIT_UNIT);
        mapNameId.put(CAPTURE_EQUIPMENT,				ID.CAPTURE_EQUIPMENT);
        mapNameId.put(CAPTURE_GOODS,					ID.CAPTURE_GOODS);
        mapNameId.put(CAPTURE_UNITS,					ID.CAPTURE_UNITS);
        mapNameId.put(CARRY_GOODS,					ID.CARRY_GOODS);
        mapNameId.put(CARRY_TREASURE,					ID.CARRY_TREASURE);
        mapNameId.put(CARRY_UNITS,					ID.CARRY_UNITS);
        mapNameId.put(COASTAL_ONLY,					ID.COASTAL_ONLY);
        mapNameId.put(CONSUME_ALL_OR_NOTHING,				ID.CONSUME_ALL_OR_NOTHING);
        mapNameId.put(CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES,	ID.CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES);
        mapNameId.put(DEMAND_TRIBUTE,					ID.DEMAND_TRIBUTE);
        mapNameId.put(DEMOTE_ON_ALL_EQUIPMENT_LOST,			ID.DEMOTE_ON_ALL_EQUIPMENT_LOST);
        mapNameId.put(DENOUNCE_HERESY,					ID.DENOUNCE_HERESY);
        mapNameId.put(DISPOSE_ON_ALL_EQUIPMENT_LOST,			ID.DISPOSE_ON_ALL_EQUIPMENT_LOST);
        mapNameId.put(DISPOSE_ON_COMBAT_LOSS,				ID.DISPOSE_ON_COMBAT_LOSS);
        mapNameId.put(DRESS_MISSIONARY,					ID.DRESS_MISSIONARY);
        mapNameId.put(ELECT_FOUNDING_FATHER,				ID.ELECT_FOUNDING_FATHER);
        mapNameId.put(ESTABLISH_MISSION,				ID.ESTABLISH_MISSION);
        mapNameId.put(EVADE_ATTACK,					ID.EVADE_ATTACK);
        mapNameId.put(EXPERT_MISSIONARY,				ID.EXPERT_MISSIONARY);
        mapNameId.put(EXPERT_PIONEER,					ID.EXPERT_PIONEER);
        mapNameId.put(EXPERT_SCOUT,					ID.EXPERT_SCOUT);
        mapNameId.put(EXPERT_SOLDIER,					ID.EXPERT_SOLDIER);
        mapNameId.put(EXPERTS_USE_CONNECTIONS,				ID.EXPERTS_USE_CONNECTIONS);
        mapNameId.put(EXPORT,						ID.EXPORT);
        mapNameId.put(FOUND_COLONY,					ID.FOUND_COLONY);
        mapNameId.put(FOUND_IN_LOST_CITY,				ID.FOUND_IN_LOST_CITY);
        mapNameId.put(FOUNDS_COLONIES,					ID.FOUNDS_COLONIES);
        mapNameId.put(HAS_PORT,						ID.HAS_PORT);
        mapNameId.put(IGNORE_EUROPEAN_WARS,				ID.IGNORE_EUROPEAN_WARS);
        mapNameId.put(IMPROVE_TERRAIN,					ID.IMPROVE_TERRAIN);
        mapNameId.put(INCITE_NATIVES,					ID.INCITE_NATIVES);
        mapNameId.put(INDEPENDENCE_DECLARED,				ID.INDEPENDENCE_DECLARED);
        mapNameId.put(INDEPENDENT_NATION,				ID.INDEPENDENT_NATION);
        mapNameId.put(MERCENARY_UNIT,					ID.MERCENARY_UNIT);
        mapNameId.put(MOUNTED,						ID.MOUNTED);
        mapNameId.put(MOVE_TO_EUROPE,					ID.MOVE_TO_EUROPE);
        mapNameId.put(MULTIPLE_ATTACKS,					ID.MULTIPLE_ATTACKS);
        mapNameId.put(NATIVE,						ID.NATIVE);
        mapNameId.put(NAVAL_UNIT,					ID.NAVAL_UNIT);
        mapNameId.put(NEGOTIATE,					ID.NEGOTIATE);
        mapNameId.put(PERSON,						ID.PERSON);
        mapNameId.put(PILLAGE_UNPROTECTED_COLONY,			ID.PILLAGE_UNPROTECTED_COLONY);
        mapNameId.put(PIRACY,						ID.PIRACY);
        mapNameId.put(PLUNDER_NATIVES,					ID.PLUNDER_NATIVES);
        mapNameId.put(PRODUCE_IN_WATER,					ID.PRODUCE_IN_WATER);
        mapNameId.put(REF_UNIT,						ID.REF_UNIT);
        mapNameId.put(REPAIR_UNITS,					ID.REPAIR_UNITS);
        mapNameId.put(ROYAL_EXPEDITIONARY_FORCE,			ID.ROYAL_EXPEDITIONARY_FORCE);
        mapNameId.put(RUMOURS_ALWAYS_POSITIVE,				ID.RUMOURS_ALWAYS_POSITIVE);
        mapNameId.put(SEE_ALL_COLONIES,					ID.SEE_ALL_COLONIES);
        mapNameId.put(SELECT_RECRUIT,					ID.SELECT_RECRUIT);
        mapNameId.put(SPEAK_WITH_CHIEF,					ID.SPEAK_WITH_CHIEF);
        mapNameId.put(SPY_ON_COLONY,					ID.SPY_ON_COLONY);
        mapNameId.put(SUPPORT_UNIT,					ID.SUPPORT_UNIT);
        mapNameId.put(TEACH,						ID.TEACH);
        mapNameId.put(TRADE_WITH_FOREIGN_COLONIES,			ID.TRADE_WITH_FOREIGN_COLONIES);
        mapNameId.put(UNDEAD,						ID.UNDEAD);
        mapNameId.put(UPGRADE_CONVERT,					ID.UPGRADE_CONVERT);
    }

    /**
     * Deliberately trivial constructor.
     *
     * @param specification The {@code Specification} to use.
     */
    public Ability(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code Ability} instance.
     *
     * @param id The object identifier.
     * @param source The source {@code FreeColSpecObjectType}.
     * @param value The ability value.
     */
    public Ability(String id, FreeColSpecObjectType source, boolean value) {
        this((source == null) ? null : source.getSpecification());

        setId(id);
        setSource(source);
        this.value = value;
    }

    /**
     * Creates a new {@code Ability} instance.
     *
     * @param id The object identifier.
     */
    public Ability(String id) {
        this(id, null, true);
    }

    /**
     * Creates a new {@code Ability} instance.
     *
     * @param id The object identifier.
     * @param value The ability value.
     */
    public Ability(String id, boolean value) {
        this(id, null, value);
    }

    /**
     * Creates a new {@code Ability} instance.
     *
     * @param template An {@code Ability} to copy from.
     */
    public Ability(Ability template) {
        this((Specification)null);

        copyFrom(template);
        this.value = template.value;
    }

    /**
     * Creates a new {@code Ability} instance.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification A {@code Specification} to refer to.
     * @exception XMLStreamException if an error occurs
     */
    public Ability(FreeColXMLReader xr,
                   Specification specification) throws XMLStreamException {
        this(specification);

        readFromXML(xr);
    }


    /**
     * Get the ability value.
     *
     * @return The ability value.
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Set the ability value.
     *
     * @param newValue The new ability value.
     */
    public void setValue(final boolean newValue) {
        this.value = newValue;
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        value = xr.getAttribute(VALUE_TAG, true);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Ability) {
            return this.value == ((Ability)o).value
                && super.equals(o);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash += (value) ? 1 : 0;
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[ ").append(getId());
        if (getSource() != null) {
            sb.append(" (").append(getSource().getId()).append(')');
        }
        sb.append(" = ").append(value).append(" ]");
        return sb.toString();
    }
}
