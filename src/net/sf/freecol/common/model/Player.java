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

import java.awt.Color;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.i18n.NameCache;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * Represents a player.  The player can be either a human player or an
 * AI-player, which is further subdivided by PlayerType.
 *
 * In addition to storing the name, nation etc of the player, it also
 * stores various defaults for the player.  One example of this is the
 * {@link #getEntryLocation entry location}.
 */
public class Player extends FreeColGameObject implements Nameable {

    private static final Logger logger = Logger.getLogger(Player.class.getName());

    public static final String TAG = "player";

    /**
     * Class object for use by {@link net.sf.freecol.client.ClientOptions}
     */
    protected int classindex = 10;

    //
    // Types
    //

    /** Types of players. */
    public static enum PlayerType {
        NATIVE, COLONIAL, REBEL, INDEPENDENT, ROYAL, UNDEAD, RETIRED
    }

    /** Colony value categories. */
    public static enum ColonyValueCategory {
        A_OVERRIDE, // override slot containing showstopper NoValueType values
        A_PROD,     // general production level
        A_TILE,     // strangeness with the tile
        A_EUROPE,   // proximity to Europe
        A_RESOURCE, // penalize building on top of a resource
        A_ADJACENT, // penalize adjacent units and settlement-owned-tiles
        A_FOOD,     // penalize food shortage
        A_LEVEL,    // reward high production potential
        A_NEARBY,   // penalize nearby units and settlements
        A_GOODS;    // check sufficient critical goods available (e.g. lumber)
        // A_GOODS must be last, the spec is entitled to require checks on
        // as many goods types as it likes

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return super.toString().substring(2);
        }
    }

    /** Special return values for showstopper getColonyValue fail. */
    public static enum NoValueType {
        BOGUS(-1), TERRAIN(-2), RUMOUR(-3), SETTLED(-4), FOOD(-5), INLAND(-6), POLAR(-7);

        private static final int MAX = values().length;

        private final int value;


        NoValueType(int value) { this.value = value; }

        public int getValue() { return value; }

        public double getDouble() { return (double)value; }

        public static NoValueType fromValue(int i) {
            int n = -i - 1;
            return (n >= 0 && n < MAX) ? NoValueType.values()[n] : BOGUS;
        }
    }

    /**
     * VisibilityMap - holds a bitmap for the visible tiles
     *
     * note: semantically, composition instead of inheritance would
     * be more correct. OTOH, it would require yet another instance.
     * as its just an private internal class, we can afford being a
     * bit unclean to save a bit of memory.
     */
    private class TileVisibilityMap extends BitSet {
        private int width;
        private int height;
        private BitSet bits = new BitSet();

        protected TileVisibilityMap(int w, int h) {
            width = w;
            height = h;
        }

        /** hope the compiler inlines it **/
        private final int bitid(int x, int y) {
            return y * width + x;
        }

        /** trivial case: mark visible via coordinates **/
        protected final void setVisible(int x, int y) {
            bits.set(bitid(x,y));
        }

        /** mark an individual tile visible **/
        protected final void setVisible(Tile t) {
            if (t != null) {
                bits.set(bitid(t.getX(), t.getY()));
                t.seeTile(Player.this);
            }
        }

        /** mark a whole set of tiles visible **/
        protected final void setVisible(Set<Tile> st) {
            if (st != null)
                for (Tile t : st)
                    setVisible(t);
        }

        /** mark an settlement and its surrounding visible **/
        protected final void setVisible(Settlement s) {
            if (s != null)
                setVisible(s.getVisibleTiles());
        }

        /** mark an unit and is surrounding visible - only when the
            unit is on the map (skipping those in Europe) **/
        protected final void setVisible(Unit u) {
            if ((u != null) && (u.getLocation() instanceof Tile))
                setVisible(u.getVisibleTiles());
        }

        /** check whether a tile is visible **/
        protected final boolean isVisible(Tile t) {
            return (t == null ? false
                              : bits.get(bitid(t.getX(), t.getY())));
        }
    }

    //
    // Constants
    //

    /** A comparator for ordering players. */
    public static final Comparator<Player> playerComparator
        = new Comparator<Player>() {
            public int compare(Player a, Player b) {
                return a.getRank() - b.getRank();
            }};

    /** A magic constant to denote that a players gold is not tracked. */
    public static final int GOLD_NOT_ACCOUNTED = Integer.MIN_VALUE;

    /**
     * A token to use for the settlement name in requests to the server
     * to ask the server to choose a settlement name.
     */
    public static final String ASSIGN_SETTLEMENT_NAME = "";


    //
    // Class variables
    //

    /**
     * The name of this player.  This defaults to the user name in
     * case of a human player and the rulerName of the NationType in
     * case of an AI player.
     */
    protected String name;

    /** The name of this player as an independent nation. */
    protected String independentNationName;

    /** The type of player. */
    protected PlayerType playerType;

    /** The player nation type. */
    protected NationType nationType;

    /** The nation identifier of this player, e.g. "model.nation.dutch". */
    protected String nationId;

    /** The name this player uses for the New World. */
    protected String newLandName = null;

    /** Is this player an admin? */
    protected boolean admin;

    /** Is this player an AI? */
    protected boolean ai;

    /** Is this player ready to start? */
    protected boolean ready;

    /** Is this player dead? */
    protected boolean dead = false;

    /** True if player has been attacked by privateers. */
    protected boolean attackedByPrivateers = false;

    /**
     * Whether the player is bankrupt, i.e. unable to pay for the
     * maintenance of all buildings.
     */
    private boolean bankrupt;

    /** The current score of this player. */
    protected int score;

    /** The amount of gold this player owns. */
    protected int gold;

    /**
     * The number of immigration points.  Immigration points are an
     * abstract game concept.  They are generated by but are not
     * identical to crosses.
     */
    protected int immigration;

    /**
     * The amount of immigration needed until the next unit decides
     * to migrate.
     */
    protected int immigrationRequired;

    /**
     * The number of liberty points.  Liberty points are an
     * abstract game concept.  They are generated by but are not
     * identical to bells.
     */
    protected int liberty;

    /** SoL from last turn. */
    protected int oldSoL = 0;

    /** The number of liberty bells produced towards the intervention force. */
    protected int interventionBells;

    /** The current tax rate for this player. */
    protected int tax = 0;

    /** The player starting location on the map. */
    protected Location entryLocation;

    /** The market for Europe. */
    protected Market market;

    /** The European port/location for this player. */
    protected Europe europe;

    /** The monarch for this player. */
    protected Monarch monarch;

    /** The founding fathers in this Player's congress. */
    protected final Set<FoundingFather> foundingFathers
        = new HashSet<>();
    /** Current founding father being recruited. */
    protected FoundingFather currentFather;
    /** The offered founding fathers. */
    protected final List<FoundingFather> offeredFathers = new ArrayList<>();

    /**
     * The tension levels, 0-1000, with 1000 being maximum hostility.
     *
     * Only used by AI, but resist the temptation to move it to AIPlayer, the
     * complexity is not worth it.
     */
    protected final java.util.Map<Player, Tension> tension = new HashMap<>();

    /** A list of players who can not establish missions to this player. */
    protected Set<Player> bannedMissions = null;

    /**
     * Stores the stance towards the other players. One of: WAR, CEASE_FIRE,
     * PEACE and ALLIANCE.
     */
    protected final java.util.Map<String, Stance> stance = new HashMap<>();

    /** The trade routes defined by this player. */
    protected final List<TradeRoute> tradeRoutes = new ArrayList<>();

    /** The current model messages for this player. */
    protected final List<ModelMessage> modelMessages = new ArrayList<>();

    /** The history events occuring with this player. */
    protected final List<HistoryEvent> history = new ArrayList<>();

    /** The last-sale data. */
    protected HashMap<String, LastSale> lastSales = null;

    // Temporary/transient variables, do not serialize.

    /** The units this player owns. */
    private final List<Unit> units = new ArrayList<>();

    /** The settlements this player owns. */
    protected final List<Settlement> settlements = new ArrayList<>();

    /** The tiles the player can see. */
    private TileVisibilityMap visibilityMap = null;

    /** A container for the abilities and modifiers of this type. */
    protected final FeatureContainer featureContainer = new FeatureContainer();

    /** The maximum food consumption of unit types available to this player. */
    private int maximumFoodConsumption = -1;

    /** An iterator for the player units that are still active this turn. */
    private final UnitIterator nextActiveUnitIterator
        = new UnitIterator(this, UnitIterator.Type.ACTIVE);

    /** An iterator for the player units that have a destination to go to. */
    private final UnitIterator nextGoingToUnitIterator
        = new UnitIterator(this, UnitIterator.Type.GOING_TO);

    /**
     * The HighSeas is a Location that enables Units to travel between
     * the New World and one or several European Ports.
     */
    protected HighSeas highSeas = null;

    /** A cached map of the current nation summary for all live nations. */
    private final java.util.Map<Player, NationSummary> nationCache
        = new HashMap<>();


    //
    // Constructors
    //

    /**
     * Constructor for ServerPlayer.
     *
     * @param game The enclosing {@code Game}.
     */
    protected Player(Game game) {
        super(game);
    }

    /**
     * Creates a new {@code Player} with the given id.
     *
     * The object should be initialized later.
     *
     * @param game The {@code Game} this object belongs to.
     * @param id The object identifier.
     */
    public Player(Game game, String id) {
        super(game, id);
    }


    //
    // Names and naming
    //

    /**
     * Gets the name of this player.
     *
     * @return The name of this player.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the player name.
     *
     * @param newName The new name value.
     */
    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Get a label for this player.
     *
     * @return A suitable {@code StringTemplate}.
     */
    public StringTemplate getLabel() {
        return StringTemplate.label("")
            .add(getRulerNameKey())
            .addName(" (")
            .addStringTemplate(getNationLabel())
            .addName(")");
    }

    /**
     * Is this player the unknown enemy?
     *
     * @return True if this player is the unknown enemy.
     */
    public boolean isUnknownEnemy() {
        return Nation.UNKNOWN_NATION_ID.equals(nationId);
    }

    /**
     * Get the new post-declaration player name.
     *
     * @return The post-declaration player name.
     */
    public final String getIndependentNationName() {
        return independentNationName;
    }

    /**
     * Set the post-declaration player name.
     *
     * @param newIndependentNationName The new player name.
     */
    public final void setIndependentNationName(final String newIndependentNationName) {
        this.independentNationName = newIndependentNationName;
    }

    /**
     * Gets the name this player has chosen for the new world.
     *
     * @return The name of the new world as chosen by the {@code Player},
     *     or null if none chosen yet.
     */
    public String getNewLandName() {
        return newLandName;
    }

    /**
     * Has the player already selected a name for the new world?
     *
     * @return True if the new world has been named by this player.
     */
    public boolean isNewLandNamed() {
        return newLandName != null;
    }

    /**
     * Sets the name this player uses for the new world.
     *
     * @param newLandName This {@code Player}'s name for the new world.
     */
    public void setNewLandName(String newLandName) {
        this.newLandName = newLandName;
    }

    /**
     * Get a name for the new land.
     *
     * @return A suitable name.
     */
    public String getNameForNewLand() {
        return NameCache.getNewLandName(this);
    }

    /**
     * Get a name key for the player Europe.
     *
     * @return A name key, or null if Europe is null.
     */
    public String getEuropeNameKey() {
        return (europe == null) ? null : nationId + ".europe";
    }

    /**
     * Gets a resource key for the nation name.
     *
     * @return A nation resource key.
     */
    public String getNationResourceKey() {
        return lastPart(nationId, ".");
    }

    /**
     * Get a template for this players nation name.
     *
     * @return A template for this nation name.
     */
    public StringTemplate getNationLabel() {
        return (playerType == PlayerType.REBEL
                || playerType == PlayerType.INDEPENDENT)
            ? StringTemplate.name(independentNationName)
            : StringTemplate.key(Messages.nameKey(nationId));
    }

    /**
     * Get a template for this players country.
     *
     * @return A template for this country.
     */
    public StringTemplate getCountryLabel() {
        return (playerType == PlayerType.REBEL
                || playerType == PlayerType.INDEPENDENT)
            ? StringTemplate.name(independentNationName)
            : StringTemplate.template("countryName")
                .addStringTemplate("%nation%", getNationLabel());
    }

    /**
     * Get a label indicating for the national forces.
     *
     * @return A suitable {@code StringTemplate}.
     */
    public StringTemplate getForcesLabel() {
        return StringTemplate.template("model.player.forces")
            .addStringTemplate("%nation%", getNationLabel());
    }

    /**
     * Get a label indicating that we are waiting for this player.
     *
     * @return A suitable {@code StringTemplate}.
     */
    public StringTemplate getWaitingLabel() {
        return StringTemplate.template("model.player.waitingFor")
            .addStringTemplate("%nation%", getNationLabel());
    }

    /**
     * Get a short debug-suitable name for this player.
     *
     * @return A short name for this player.
     */
    public String getDebugName() {
        return getNation().getSuffix();
    }

    /**
     * Get a name key for the player nation ruler.
     *
     * @return The ruler name key.
     */
    public final String getRulerNameKey() {
        return Messages.rulerKey(nationId);
    }

    /**
     * Get a resource key for the player monarch image.
     *
     * @return The monarch image key.
     */
    public String getMonarchKey() {
        return "image.flavor.monarch." + nationId;
    }

    /**
     * What is the name of the player's market?
     * Following a declaration of independence we are assumed to trade
     * broadly with any European market rather than a specific port.
     *
     * @return A {@code StringTemplate} for the player market.
     */
    public StringTemplate getMarketName() {
        return (getEurope() == null)
            ? StringTemplate.key("model.player.independentMarket")
            : StringTemplate.key(getEuropeNameKey());
    }

    /**
     * Gets the name of this players capital.  Only meaningful to natives.
     *
     * @param random An optional pseudo-random number source.
     * @return The name of this players capital.
     */
    public String getCapitalName(Random random) {
        return NameCache.getCapitalName(this, random);
    }

    /**
     * Gets a settlement name suitable for this player.
     *
     * @param random An optional pseudo-random number source.
     * @return A new settlement name.
     */
    public String getSettlementName(Random random) {
        return NameCache.getSettlementName(this, random);
    }

    /**
     * Puts a suggested settlement name back into the pool.
     *
     * @param name A formerly suggested settlement name.
     */
    public void putSettlementName(String name) {
        NameCache.putSettlementName(this, name);
    }

    /**
     * Get a name for a region.
     *
     * @param region The {@code Region} to name.
     * @return A suitable name.
     */
    public String getNameForRegion(Region region) {
        return NameCache.getRegionName(this, region);
    }

    /**
     * Gets a new name for a unit.
     *
     * @param type The {@code UnitType} to choose a name for.
     * @param random A pseudo-random number source.
     * @return A name for the unit, or null if not available.
     */
    public String getNameForUnit(UnitType type, Random random) {
        return NameCache.getUnitName(this, type, random);
    }


    //
    // Player / nation types and the implications thereof
    //

    /**
     * Get the type of this player.
     *
     * @return The player type.
     */
    public PlayerType getPlayerType() {
        return playerType;
    }

    /**
     * Sets the player type.
     *
     * @param type The new player type.
     * @see #getPlayerType
     */
    private void setPlayerType(PlayerType type) {
        playerType = type;
    }

    /**
     * Change the player type.
     *
     * Handle special abilities that are added following declaration
     * of independence.  Do not bother removing them ATM, only cases are
     * when going undead and retiring where they are moot.
     *
     * @param type The new player type.
     */
    public void changePlayerType(PlayerType type) {
        if (playerType != PlayerType.REBEL
            && playerType != PlayerType.INDEPENDENT) {
            switch (type) {
            case REBEL: case INDEPENDENT:
                addAbility(new Ability(Ability.INDEPENDENCE_DECLARED, true));
                addAbility(new Ability(Ability.INDEPENDENT_NATION, true));
                break;
            default:
                break;
            }
        }

        setPlayerType(type);
    }

    /**
     * Checks if this player is colonial, and thus can recruit units
     * by producing immigration.
     *
     * @return True if this player is colonial.
     */
    public boolean isColonial() {
        return playerType == PlayerType.COLONIAL;
    }

    /**
     * Checks if this player is European, with does include the REF.
     *
     * @return True if this player is European.
     */
    public boolean isEuropean() {
        return playerType == PlayerType.COLONIAL
            || playerType == PlayerType.REBEL
            || playerType == PlayerType.INDEPENDENT
            || playerType == PlayerType.ROYAL;
    }

    public final boolean isLive() {
        return (!isUnknownEnemy() && !isDead());
    }

    public final boolean isLiveEuropean() {
        return (!isUnknownEnemy() && !isDead() && isEuropean());
    }

    public final boolean isLiveNative() {
        return (!isUnknownEnemy() && !isDead() && isIndian());
    }

    /**
     * Is this a native player?
     *
     * @return True if this player is a native player.
     */
    public boolean isIndian() {
        return playerType == PlayerType.NATIVE;
    }

    /**
     * Is this a colonial rebel player?
     *
     * @return True if this player is a rebel player.
     */
    public boolean isRebel() {
        return playerType == PlayerType.REBEL;
    }

    /**
     * Is this an undead player?
     *
     * @return True if this player is undead.
     */
    public boolean isUndead() {
        return playerType == PlayerType.UNDEAD;
    }

    /**
     * Is this a REF player?
     *
     * @return True if this is a REF player.
     */
    public boolean isREF() {
        return playerType == PlayerType.ROYAL;
    }

    /**
     * Is this player currently on good terms with a given player, and thus
     * a suitable candidate for a random monarch war declaration?
     *
     * @param player The {@code Player} to possibly declare war on.
     * @return True if this player is a potential enemy.
     */
    public boolean isPotentialEnemy(Player player) {
        if (!hasAbility(Ability.IGNORE_EUROPEAN_WARS)
            && player.getREFPlayer() != this) {
            switch (getStance(player)) {
            case PEACE: case CEASE_FIRE: return true;
            default: break;
            }
        }
        return false;
    }

    /**
     * Is this player currently on bad terms with a given player, and thus
     * a suitable candidate for a random monarch peace declaration?
     *
     * @param player The {@code Player} to possibly declare peace with.
     * @return True if this player is a potential friend.
     */
    public boolean isPotentialFriend(Player player) {
        if (player.getREFPlayer() != this) {
            switch (getStance(player)) {
            case WAR: case CEASE_FIRE: return true;
            default: break;
            }
        }
        return false;
    }

    /**
     * Get the nation type of this player.
     *
     * @return The {@code NationType} of this player.
     */
    public NationType getNationType() {
        return nationType;
    }

    /**
     * Sets the nation type of this player.
     *
     * @param newNationType The new {@code NationType}.
     */
    public void setNationType(NationType newNationType) {
        nationType = newNationType;
    }

    /**
     * Changes the nation type of this player, handling the features.
     *
     * @param newNationType The new {@code NationType}.
     */
    public void changeNationType(NationType newNationType) {
        if (nationType != null) removeFeatures(nationType);
        setNationType(newNationType);
        if (newNationType != null) addFeatures(newNationType);
    }

    /**
     * Can this player build colonies?
     *
     * @return True if this player can found colonies.
     */
    public boolean canBuildColonies() {
        return nationType.hasAbility(Ability.FOUNDS_COLONIES);
    }

    /**
     * Can this player recruit founding fathers?
     *
     * @return True if this player can recruit founding fathers.
     */
    public boolean canHaveFoundingFathers() {
        return nationType.hasAbility(Ability.ELECT_FOUNDING_FATHER);
    }

    /**
     * Get the identifier for this Player's nation.
     *
     * @return The nation identifier.
     */
    public String getNationId() {
        return nationId;
    }

    /**
     * Gets this Player's nation.
     *
     * @return The player {@code Nation}.
     */
    public Nation getNation() {
        return getSpecification().getNation(nationId);
    }

    /**
     * Sets the nation for this player.
     *
     * @param newNation The new {@code Nation}.
     */
    public void setNation(Nation newNation) {
        Nation oldNation = getNation();
        nationId = newNation.getId();
        java.util.Map<Nation, NationState> nations
            = getGame().getNationOptions().getNations();
        nations.put(oldNation, NationState.AVAILABLE);
        nations.put(newNation, NationState.NOT_AVAILABLE);
    }

    /**
     * Is this player an admin.
     *
     * @return True if the player is an admin.
     */
    public boolean isAdmin() {
        return this.admin;
    }

    /**
     * Is this an AI player?
     *
     * @return True if this is an AI player.
     */
    public boolean isAI() {
        return ai;
    }

    /**
     * Sets whether this player is an AI player.
     *
     * @param ai The AI player value.
     */
    public void setAI(boolean ai) {
        this.ai = ai;
    }

    /**
     * Is this player ready to start the game?
     *
     * @return True if this {@code Player} is ready to start the game.
     */
    public boolean isReady() {
        return getReady() || isAI();
    }

    /**
     * Get the readiness state.
     *
     * @return The readiness state.
     */
    public boolean getReady() {
        return this.ready;
    }

    /**
     * Sets this players readiness state.
     *
     * @param ready The new readiness state.
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    /**
     * Checks if this player is dead.  A {@code Player} dies when it
     * loses the game.
     *
     * @return True if this {@code Player} is dead.
     */
    public boolean isDead() {
        return dead;
    }

    /**
     * Get the player death state.
     * This is indeed identical to isDead(), but is needed for partial
     * updates to complement the setDead() function.
     *
     * @return True if this {@code Player} is dead.
     */
    public boolean getDead() {
        return dead;
    }

    /**
     * Sets this player to be dead or not.
     *
     * @param dead The new death state.
     * @see #getDead
     */
    public void setDead(boolean dead) {
        this.dead = dead;
    }

    /**
     * Has player has been attacked by privateers?
     *
     * @return True if this player has been attacked by privateers.
     */
    public boolean getAttackedByPrivateers() {
        return attackedByPrivateers;
    }

    /**
     * Sets whether this player has been attacked by privateers.
     *
     * @param attacked True if the player has been attacked by privateers.
     */
    public void setAttackedByPrivateers(boolean attacked) {
        attackedByPrivateers = attacked;
    }

    /**
     * Checks if this player has work to do if it is a REF-player.
     *
     * @return True if any of our units are located in the new
     *     world or a nation is in rebellion against us.
     */
    public boolean isWorkForREF() {
        return Unit.anyHasTile(getUnits())
            ? true // Work to do still if there exists a unit in the new world
            : !getRebels().isEmpty();
    }

    /**
     * Gets a list of the players in rebellion against this (REF) player.
     *
     * @return A list of nations in rebellion against us.
     */
    public List<Player> getRebels() {
        return transform(getGame().getLiveEuropeanPlayers(this),
                         p -> (p.getREFPlayer() == this
                             && (p.isRebel() || p.isUndead())));
    }

    /**
     * Gets the {@code Player} controlling the "Royal Expeditionary
     * Force" for this player.
     *
     * @return The player, or {@code null} if this player does not have a
     *         royal expeditionary force.
     */
    public Player getREFPlayer() {
        Nation ref = getNation().getREFNation();
        return (ref == null) ? null : getGame().getPlayerByNation(ref);
    }

    /**
     * Gets the player nation color.
     *
     * @return The {@code Color}.
     */
    public Color getNationColor() {
        final Nation nation = getNation();
        Color color = nation.getColor();
        return (color != null) ? color : nation.forceDefaultColor();
    }

    /**
     * Get an integer to broadly categorized the player for sorting.
     * AIs should always follow humans, and the administrator is special.
     *
     * @return An identifying integer.
     */
    public int getRank() {
        int ret = (isEuropean()) ? 1 : 0;
        if (isAI()) ret |= 2;
        if (isAdmin()) ret |= 4;
        return ret;
    }


    //
    // Scoring and finance
    //

    /**
     * Gets the current score of the player.
     *
     * @return The score.
     */
    public int getScore() {
        return score;
    }

    /**
     * Set the current score of the player.
     *
     * @param score The new score.
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Gets the score by which we decide the weakest and strongest AI
     * players for the Spanish Succession event.
     *
     * @return A strength score.
     */
    public int getSpanishSuccessionScore() {
        return getScore();
    }

    /**
     * Get the amount of gold that this player has.
     *
     * Some players do not account their gold.  These players return
     * GOLD_NOT_ACCOUNTED.
     *
     * @return The amount of gold that this player has.
     */
    public int getGold() {
        return gold;
    }

    /**
     * Set the amount of gold that this player has.
     *
     * @param newGold The new player gold value.
     */
    public void setGold(int newGold) {
        gold = newGold;
    }

    /**
     * Checks if the player has enough gold to make a purchase.
     * Use this rather than comparing with getGold(), as this handles
     * players that do not account for gold.
     *
     * @param amount The purchase price to check.
     * @return True if the player can afford the purchase.
     */
    public boolean checkGold(int amount) {
        return this.gold == GOLD_NOT_ACCOUNTED || this.gold >= amount;
    }

    /**
     * Modifies the amount of gold that this player has.  The argument can be
     * both positive and negative.
     *
     * @param amount The amount of gold to be added to this player.
     * @return The amount of gold post-modification.
     */
    public int modifyGold(int amount) {
        if (this.gold != Player.GOLD_NOT_ACCOUNTED) {
            if ((gold + amount) >= 0) {
                gold += amount;
            } else {
                // This can happen if the server and the client get
                // out of sync.  Perhaps it can also happen if the
                // client tries to adjust gold for another player,
                // where the balance is unknown. Just keep going and
                // do the best thing possible, we don't want to crash
                // the game here.
                logger.warning("Cannot add " + amount + " gold for "
                    + this + ": would be negative!");
                gold = 0;
            }
        }
        return gold;
    }

    /**
     * Get the bankruptcy state.
     *
     * isBankrupt would be nicer, but the introspector expects getBankrupt.
     *
     * @return True if this player is bankrupt.
     */
    public final boolean getBankrupt() {
        return bankrupt;
    }

    /**
     * Set the bankruptcy state.
     *
     * @param newBankrupt The new bankruptcy value.
     */
    public final void setBankrupt(final boolean newBankrupt) {
        this.bankrupt = newBankrupt;
    }


    //
    // Migration
    //

    /**
     * Gets the amount of immigration this player possess.
     *
     * @return The immigration value.
     * @see #reduceImmigration
     */
    public int getImmigration() {
        return (isColonial()) ? immigration : 0;
    }

    /**
     * Sets the amount of immigration this player possess.
     *
     * @param immigration The immigration value for this player.
     */
    public void setImmigration(int immigration) {
        if (!isColonial()) return;
        this.immigration = immigration;
    }

    /**
     * Sets the number of immigration this player possess.
     */
    public void reduceImmigration() {
        if (!isColonial()) return;

        int cost = getSpecification()
            .getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW)
            ? immigrationRequired : immigration;
        if (cost > immigration) {
            immigration = 0;
        } else {
            immigration -= cost;
        }
    }

    /**
     * Modify the player immigration.
     *
     * @param amount The amount to modify the immigration by.
     */
    public void modifyImmigration(int amount) {
        immigration = Math.max(0, immigration + amount);
    }

    /**
     * Gets the amount of immigration required to cause a new colonist
     * to emigrate.
     *
     * @return The immigration points required to trigger emigration.
     */
    public int getImmigrationRequired() {
        return immigrationRequired;
    }

    /**
     * Sets the number of immigration required to cause a new colonist
     * to emigrate.
     *
     * @param immigrationRequired The new number of immigration points.
     */
    public void setImmigrationRequired(int immigrationRequired) {
        this.immigrationRequired = immigrationRequired;
    }

    /**
     * Updates the amount of immigration needed to emigrate a {@code Unit}
     * from {@code Europe}.
     */
    public void updateImmigrationRequired() {
        if (!isColonial()) return;

        final Specification spec = getSpecification();
        final int turn = getGame().getTurn();
        final int current = immigrationRequired;
        int base = spec.getInteger(GameOptions.CROSSES_INCREMENT);
        // If the religious unrest bonus is present, immigrationRequired
        // has already been reduced.  We want to apply the bonus to the
        // sum of the *unreduced* immigration target and the increment.
        int unreduced = Math.round(current
            / applyModifiers(1f, turn, Modifier.RELIGIOUS_UNREST_BONUS));
        immigrationRequired = (int)applyModifiers(unreduced + base, turn,
            Modifier.RELIGIOUS_UNREST_BONUS);;
        logger.finest("Immigration for " + getId() + " updated " + current
            + " -> " + immigrationRequired);
    }

    /**
     * Should a new colonist emigrate?
     *
     * @return Whether a new colonist should emigrate.
     */
    public boolean checkEmigrate() {
        return (isColonial()) ? getImmigrationRequired() <= immigration
            : false;
    }

    /**
     * Get the total immigration production for this player.
     *
     * @return The total immigration production.
     */
    public final int getTotalImmigrationProduction() {
        if (!isColonial()) return 0;

        final List<GoodsType> immigrationGoodsTypes = getSpecification()
            .getImmigrationGoodsTypeList();

        int production = 0;
        synchronized (this.settlements) {
            for (Settlement s : this.settlements)
                for (GoodsType gt : immigrationGoodsTypes)
                    production += s.getTotalProductionOf(gt);
        }

        final Europe europe = getEurope();
        if (europe != null) production += europe.getImmigration(production);
        return production;
    }

    /**
     * Get a message for a newly migrating unit.
     *
     * @param unit The {@code Unit} that is migrating.
     * @return A suitable {@code ModelMessage}.
     */
    public ModelMessage getEmigrationMessage(Unit unit) {
        return new ModelMessage(ModelMessage.MessageType.UNIT_ADDED,
                                "model.player.emigrate",
                                this, unit)
            .addNamed("%europe%", getEurope())
            .addStringTemplate("%unit%", unit.getLabel());
    }


    //
    // Liberty and founding fathers
    //

    /**
     * Gets the current amount of liberty points this player has.
     * Liberty is regularly reduced to pay for a founding father.
     *
     * @return The amount of liberty points.
     */
    public int getLiberty() {
        return (canHaveFoundingFathers()) ? liberty : 0;
    }

    /**
     * Sets the current amount of liberty this player has.
     *
     * @param liberty The new amount of liberty.
     */
    public void setLiberty(int liberty) {
        if (!canHaveFoundingFathers()) return;
        this.liberty = liberty;
    }

    /**
     * Modifies the current amount of liberty this player has.
     *
     * @param amount The amount of liberty to add.
     */
    public void modifyLiberty(int amount) {
        setLiberty(Math.max(0, liberty + amount));
        if (isRebel()) interventionBells += amount;
    }

    /**
     * Recalculate bells bonus when tax changes.
     *
     * @return True if a bells bonus was set.
     */
    protected boolean recalculateBellsBonus() {
        boolean ret = false;
        for (Ability ability : transform(getAbilities(Ability.ADD_TAX_TO_BELLS),
                                         isNotNull(Ability::getSource))) {
            FreeColObject source = ability.getSource();
            for (Modifier modifier : transform(getModifiers("model.goods.bells"),
                                               matchKeyEquals(source, Modifier::getSource))) {
                modifier.setValue(tax);
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Gets the total percentage of rebels in all this player's colonies.
     *
     * @return The total percentage of rebels in all this player's colonies.
     */
    public final int getSoL() {
        synchronized (this.settlements) {
            int total = 0;
            int count = 0;

            for (Settlement s : this.settlements)
                if (s instanceof Colony) {
                    total += ((Colony)s).getSoL();
                    count++;
                }

            return (count == 0 ? 0 : total/count);
        }
    }

    /**
     * Gets the founding fathers in this player's congress.
     *
     * @return A set of {@code FoundingFather}s in congress.
     */
    public Set<FoundingFather> getFathers() {
        return foundingFathers;
    }

    /**
     * Does this player have a certain Founding father.
     *
     * @param someFather The {@code FoundingFather} to check.
     * @return Whether this player has this Founding father
     * @see FoundingFather
     */
    public boolean hasFather(FoundingFather someFather) {
        return foundingFathers.contains(someFather);
    }

    /**
     * Gets the number of founding fathers in this players congress.
     * Used to calculate number of liberty needed to recruit new fathers.
     *
     * @return The number of founding fathers in this players congress
     */
    public int getFatherCount() {
        return foundingFathers.size();
    }

    /**
     * Add a founding father to the congress.
     *
     * @param father The {@code FoundingFather} to add.
     */
    public final void addFather(FoundingFather father) {
        foundingFathers.add(father);
        addFeatures(father);
        synchronized (this.settlements) {
            for (Settlement s : this.settlements)
                if (s instanceof Colony)
                    ((Colony)s).invalidateCache();
        }
    }

    /**
     * Gets the {@link FoundingFather founding father} this player is working
     * towards.
     *
     * @return The current {@code FoundingFather}, or null if
     *     there is none.
     * @see #setCurrentFather
     * @see FoundingFather
     */
    public FoundingFather getCurrentFather() {
        return currentFather;
    }

    /**
     * Sets the current founding father to recruit.
     *
     * @param someFather The {@code FoundingFather} to recruit.
     * @see FoundingFather
     */
    public void setCurrentFather(FoundingFather someFather) {
        currentFather = someFather;
    }

    /**
     * Gets the offered fathers for this player.
     *
     * @return A list of the current offered {@code FoundingFather}s.
     */
    public List<FoundingFather> getOfferedFathers() {
        return offeredFathers;
    }

    /**
     * Clear the set of offered fathers.
     */
    public void clearOfferedFathers() {
        offeredFathers.clear();
    }

    /**
     * Sets the set of offered fathers.
     *
     * @param fathers A list of {@code FoundingFather}s to offer.
     */
    public void setOfferedFathers(List<FoundingFather> fathers) {
        clearOfferedFathers();
        offeredFathers.addAll(fathers);
    }

    /**
     * Gets the number of liberty points needed to recruit the next
     * founding father.
     *
     * @return How many more liberty points the {@code Player}
     *     needs in order to recruit the next {@code FoundingFather}.
     */
    public int getRemainingFoundingFatherCost() {
        return getTotalFoundingFatherCost() - getLiberty();
    }

    /**
     * How many liberty points in total are needed to earn the
     * Founding Father we are trying to recruit.  See
     * https://sourceforge.net/p/freecol/bugs/2623 where the Col1
     * numbers were checked.
     *
     * @return Total number of liberty points the {@code Player}
     *     needs to recruit the next {@code FoundingFather}.
     */
    public int getTotalFoundingFatherCost() {
        final Specification spec = getSpecification();
        int base = spec.getInteger(GameOptions.FOUNDING_FATHER_FACTOR);
        int count = getFatherCount();
        return (count == 0) ? base : 2 * (count + 1) * base + 1;
    }

    /**
     * Gets the {@code Turn}s during which FoundingFathers were
     * elected to the Continental Congress
     *
     * @return A map of father id to {@code Turn}s.
     */
    public java.util.Map<String, Integer> getElectionTurns() {
        return transform(getHistory(),
                         matchKey(HistoryEvent.HistoryEventType.FOUNDING_FATHER,
                                  HistoryEvent::getEventType),
                         Function.identity(),
                         Collectors.toMap(he -> he.getReplacement("%father%").getId(),
                                          HistoryEvent::getTurn));
    }

    /**
     * Check whether this player can declare independence.
     *
     * @return Null if there is no barrier to declaration, otherwise
     *     a {@code StringTemplate} explaining the problem.
     */
    public StringTemplate checkDeclareIndependence() {
        if (getPlayerType() != PlayerType.COLONIAL)
            return StringTemplate.template("model.player.colonialIndependence");
        final Event event = getSpecification()
            .getEvent("model.event.declareIndependence");
        for (Limit limit : event.getLimits())
            if (!limit.evaluate(this))
                return StringTemplate.template(limit.getDescriptionKey())
                .addAmount("%limit%", limit.getRightHandSide()
                    .getValue(getGame()));
        return null;
    }

    /**
     * Generic strength calculation.
     *
     * @param naval If true consider naval units, otherwise land units.
     * @return A measure of naval or land offensive power.
     */
    public int calculateStrength(boolean naval) {
        final CombatModel cm = getGame().getCombatModel();
        return (int)sumDouble(getUnits(), u -> u.isNaval() == naval,
                              u -> cm.getOffencePower(u, null));
    }

    /**
     * Get the strength ratio of this player with respect to its REF.
     *
     * @param naval If true consider naval units, otherwise land units.
     * @return A measure of the military viability of this player.
     */
    public double getRebelStrengthRatio(boolean naval) {
        if (getPlayerType() != PlayerType.COLONIAL) return 0.0;
        return strengthRatio(calculateStrength(naval),
            getMonarch().getExpeditionaryForce().calculateStrength(naval));
    }


    /**
     * Get a list of abstract REF units for this player.
     *
     * @return A list of {@code AbstractUnit}s defining the REF,
     *     or null if not available.
     */
    public List<AbstractUnit> getREFUnits() {
        return (getPlayerType() == PlayerType.COLONIAL)
            ? getMonarch().getExpeditionaryForce().getUnits()
            : null;
    }

    /**
     * Get a list of the military units for this player.
     *
     * @return A list of military {@code AbstractUnit}s.
     */
    public List<AbstractUnit> getMilitaryUnits() {
        final UnitType defaultType = getSpecification().getDefaultUnitType(this);
        java.util.Map<UnitType, HashMap<String, Integer>> unitHash
            = new HashMap<>();
        List<AbstractUnit> units = new ArrayList<>();
        for (Unit unit : transform(getUnits(), Unit::isOffensiveUnit)) {
            UnitType unitType = defaultType;
            if (unit.getType().getOffence() > 0
                || unit.hasAbility(Ability.EXPERT_SOLDIER)) {
                unitType = unit.getType();
            }
            HashMap<String, Integer> roleMap = unitHash.get(unitType);
            if (roleMap == null) roleMap = new HashMap<>();
            String roleId = unit.getRole().getId();
            Integer count = roleMap.get(roleId);
            roleMap.put(roleId, (count == null) ? 1 : count + 1);
            unitHash.put(unitType, roleMap);
        }
        forEachMapEntry(unitHash, te ->
            forEachMapEntry(te.getValue(), re ->
                units.add(new AbstractUnit(te.getKey(), re.getKey(), re.getValue()))));
        return units;
    }


    //
    // Taxation and trade
    //

    /**
     * Get the current tax.
     *
     * @return The current tax.
     */
    public int getTax() {
        return tax;
    }

    /**
     * Sets the current tax
     *
     * @param amount The new tax amount.
     */
    public final void setTax(int amount) {
        tax = amount;
        if (recalculateBellsBonus())
            synchronized (this.settlements) {
                for (Settlement s : this.settlements)
                    if (s instanceof Colony)
                        ((Colony)s).invalidateCache();
            }
    }

    /**
     * Get this player's Market.
     *
     * @return The {@code Market}.
     */
    public Market getMarket() {
        return market;
    }

    /**
     * Resets this player's Market.
     */
    public void reinitialiseMarket() {
        market = new Market(getGame(), this);
    }

    /**
     * Gets the current sales data for a location and goods type.
     *
     * @param where The {@code Location} of the sale.
     * @param what The {@code GoodsType} sold.
     *
     * @return An appropriate {@code LastSaleData} record, or
     *     null if no appropriate sale can be found.
     */
    public LastSale getLastSale(Location where, GoodsType what) {
        return (lastSales == null) ? null
            : lastSales.get(LastSale.makeKey(where, what));
    }

    /**
     * Saves a record of a sale.
     *
     * @param sale The {@code LastSale} to save.
     */
    public void addLastSale(LastSale sale) {
        if (lastSales == null) lastSales = new HashMap<>();
        lastSales.put(sale.getId(), sale);
    }

    /**
     * Gets the last sale price for a location and goods type as a string.
     *
     * @param where The {@code Location} of the sale.
     * @param what The {@code GoodsType} sold.
     * @return An abbreviation for the sale price, or null if none found.
     */
    public String getLastSaleString(Location where, GoodsType what) {
        LastSale data = getLastSale(where, what);
        return (data == null) ? null : String.valueOf(data.getPrice());
    }

    /**
     * Get a template for the last sale price for a location and goods type.
     *
     * @param where The {@code Location} of the sale.
     * @param what The {@code GoodsType} sold.
     * @return A {@code StringTemplate} for the sale, or null if
     *     none found.
     */
    public StringTemplate getLastSaleTip(Location where, GoodsType what) {
        LastSale data = getLastSale(where, what);
        return (data == null) ? null
            : StringTemplate.template("model.indianSettlement.lastSale")
                .addNamed("%goodsType%", what)
                .addAmount("%price%", data.getPrice())
                .addStringTemplate("%turn%", Turn.getLabel(data.getWhen()));
    }

    /**
     * Gets the arrears due for a type of goods.
     *
     * @param type The {@code GoodsType} to check.
     * @return The arrears due for this type of goods.
     */
    public int getArrears(GoodsType type) {
        return getMarket().getArrears(type);
    }

    /**
     * Can a type of goods can be traded in Europe?
     *
     * @param type The {@code GoodsType} to check.
     * @return True if there are no arrears due for this type of goods.
     */
    public boolean canTrade(GoodsType type) {
        return canTrade(type, Market.Access.EUROPE);
    }

    /**
     * Can a type of goods can be traded at a specified place?
     *
     * @param type The {@code GoodsType} to check.
     * @param access The way the goods are traded (Europe OR Custom)
     * @return True if type of goods can be traded.
     */
    public boolean canTrade(GoodsType type, Market.Access access) {
        return getMarket().getArrears(type) == 0
            || (access == Market.Access.CUSTOM_HOUSE
                && (getSpecification().getBoolean(GameOptions.CUSTOM_IGNORE_BOYCOTT)
                    || (hasAbility(Ability.CUSTOM_HOUSE_TRADES_WITH_FOREIGN_COUNTRIES)
                        && any(getGame().getLiveEuropeanPlayers(this),
                               p -> (getStance(p) == Stance.PEACE
                                   || getStance(p) == Stance.ALLIANCE)))));
    }

    /**
     * Get the current sales of a type of goods.
     *
     * @param goodsType The {@code GoodsType} to query.
     * @return The current sales.
     */
    public int getSales(GoodsType goodsType) {
        return getMarket().getSales(goodsType);
    }

    /**
     * Modifies the current sales.
     *
     * @param goodsType The {@code GoodsType} to modify.
     * @param amount The new sales.
     */
    public void modifySales(GoodsType goodsType, int amount) {
        getMarket().modifySales(goodsType, amount);
    }

    /**
     * Has a type of goods been traded?
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return Whether these goods have been traded.
     */
    public boolean hasTraded(GoodsType goodsType) {
        return getMarket().hasBeenTraded(goodsType);
    }

    /**
     * Get the most valuable goods available in one of the player's
     * colonies for the purposes of choosing a threat-to-boycott.  The
     * goods must not currently be boycotted, the player must have
     * traded in it, and the amount to be discarded will not exceed
     * GoodsContainer.CARGO_SIZE.
     *
     * @return A goods object, or null if nothing suitable found.
     */
    public Goods getMostValuableGoods() {
        if (!isEuropean()) return null;

        Goods picked_goods = null;
        int picked_price = 0;

        for (Colony c : getColonies()) {
            for (Goods g : c.getCompactGoods()) {
                GoodsType gt = g.getType();
                if (getArrears(gt) <= 0 && hasTraded(gt)) {
                    int myprice = market.getSalePrice(gt, Math.min(g.getAmount(), GoodsContainer.CARGO_SIZE));
                    if (myprice > picked_price) {
                        picked_price = myprice;
                        picked_goods = g;
                    }
                }
            }
        }
        return picked_goods;
    }

    /**
     * Get the current incomeBeforeTaxes.
     *
     * @param goodsType The {@code GoodsType} to query.
     * @return The current incomeBeforeTaxes.
     */
    public int getIncomeBeforeTaxes(GoodsType goodsType) {
        return getMarket().getIncomeBeforeTaxes(goodsType);
    }

    /**
     * Modifies the current incomeBeforeTaxes.
     *
     * @param goodsType The {@code GoodsType} to modify.
     * @param amount The new incomeBeforeTaxes.
     */
    public void modifyIncomeBeforeTaxes(GoodsType goodsType, int amount) {
        getMarket().modifyIncomeBeforeTaxes(goodsType, amount);
    }

    /**
     * Get the current incomeAfterTaxes.
     *
     * @param goodsType The {@code GoodsType} to query.
     * @return The current incomeAfterTaxes.
     */
    public int getIncomeAfterTaxes(GoodsType goodsType) {
        return getMarket().getIncomeAfterTaxes(goodsType);
    }

    /**
     * Modifies the current incomeAfterTaxes.
     *
     * @param goodsType The {@code GoodsType} to modify.
     * @param amount The new incomeAfterTaxes.
     */
    public void modifyIncomeAfterTaxes(GoodsType goodsType, int amount) {
        getMarket().modifyIncomeAfterTaxes(goodsType, amount);
    }


    //
    // Europe
    //

    /**
     * Gets this players Europe object.
     *
     * @return The Europe object, or null if the player is not
     *     European or indpendent.
     */
    public Europe getEurope() {
        return europe;
    }

    /**
     * Set the Europe object for a player.
     *
     * @param europe The new {@code Europe} object.
     */
    public void setEurope(Europe europe) {
        this.europe = europe;
    }

    /**
     * Checks if this player can move units to Europe.
     *
     * @return True if this player has an instance of {@code Europe}.
     */
    public boolean canMoveToEurope() {
        return getEurope() != null;
    }

    /**
     * Gets the price for a recruit in Europe.
     *
     * @return The price of a single recruit in {@link Europe}.
     */
    public int getRecruitPrice() {
        // return Math.max(0, (getCrossesRequired() - crosses) * 10);
        return getEurope().getRecruitPrice();
    }

    /**
     * Gets the price to this player to purchase a unit in Europe.
     *
     * @param au The proposed {@code AbstractUnit}.
     * @return The price for the unit.
     */
    public int getPrice(AbstractUnit au) {
        final Specification spec = getSpecification();
        final UnitType unitType = au.getType(spec);
        if (!unitType.hasPrice()) return INFINITY;

        return au.getNumber() * (getEurope().getUnitPrice(unitType)
            + au.getRole(spec).getRequiredGoodsPrice(getMarket()));
    }

    /**
     * Gets the monarch object this player has.
     *
     * @return The {@code Monarch} object this player has, or null
     *     if there is no monarch.
     */
    public Monarch getMonarch() {
        return monarch;
    }

    /**
     * Sets the monarch object this player has.
     *
     * @param monarch The new {@code Monarch} object.
     */
    public void setMonarch(Monarch monarch) {
        this.monarch = monarch;
    }


    //
    // Units and trade routes
    //

    /**
     * Does this player's units list contain the given unit?
     *
     * @param unit The {@code Unit} to test.
     * @return True if the player has the unit.
     */
    public boolean hasUnit(Unit unit) {
        synchronized (this.units) {
            return this.units.contains(unit);
        }
    }

    /**
     * Get a copy of the players units.
     * (copying instead of reference for concurrency)
     *
     * @return A list of the player {@code Unit}s.
     */
    public List<Unit> getUnits() {
        synchronized (this.units) {
            return (this.units.isEmpty()) ? Collections.<Unit>emptyList()
                : new ArrayList<>(this.units);
        }
    }

    /**
     * Retrieve a list of still movable units
     */
    public final List<Unit> getMovableUnits() {
        List<Unit> result = new ArrayList<>();
        for (Unit u : this.units)
            if (u.couldMove())
                result.add(u);
        return result;
    }

    /**
     * Get the number of units a player has.
     *
     * @return The number of units.
     */
    public int getUnitCount() {
        synchronized (this.units) {
            return count(this.units);
        }
    }

    /**
     * Get a player unit by name.
     *
     * @param name The name of the unit.
     * @return The unit with the given name, or null if none found.
     */
    public Unit getUnitByName(String name) {
        synchronized (this.units) {
            for (Unit u : this.units)
                if (Utils.equals(name, u.getName()))
                    return u;
            return null;
        }
    }

    /**
     * Add a unit to this player.
     *
     * @param newUnit The new {@code Unit} value.
     * @return True if the units container changed.
     */
    public final boolean addUnit(final Unit newUnit) {
        if (newUnit == null) return false;

        // Make sure the owner of the unit is set first, before adding
        // it to the list
        if (!this.owns(newUnit)) {
            throw new IllegalStateException("Adding another players unit:"
                + newUnit.getId() + " to " + this);
        }
        if (hasUnit(newUnit)) return false;

        synchronized (this.units) {
            return this.units.add(newUnit);
        }
    }

    /**
     * Remove a unit from this player.
     *
     * @param oldUnit The {@code Unit} to remove.
     * @return True if the units container changed.
     */
    public boolean removeUnit(final Unit oldUnit) {
        if (oldUnit == null) return false;

        nextActiveUnitIterator.remove(oldUnit);
        nextGoingToUnitIterator.remove(oldUnit);

        synchronized (this.units) {
            return this.units.remove(oldUnit);
        }
    }

    /**
     * Gets the carrier units that can carry the supplied unit, if one exists.
     *
     * @param unit The {@code Unit} to carry.
     * @return A list of suitable carriers.
     */
    public List<Unit> getCarriersForUnit(Unit unit) {
        return transform(getUnits(), u -> u.couldCarry(unit));
    }

    /**
     * Get the number of units a player has, either land or naval.
     *
     * @param naval If true, count naval units, else count land units.
     * @return The number of units.
     */
    public int getUnitCount(boolean naval) {
        return count(getUnits(), u -> u.isNaval() == naval);
    }

    /**
     * Gets the number of King's land units.
     *
     * @return The number of units
     */
    public int getNumberOfKingLandUnits() {
        return count(getUnits(),
                     u -> u.hasAbility(Ability.REF_UNIT) && !u.isNaval());
    }

    /**
     * Checks if this player has at least one of a given unit type.
     *
     * @param typeId The identifier for the unit type to check.
     * @return True if this player owns at least one of the specified unit type.
     */
    public boolean hasUnitType(String typeId) {
        return any(getUnits(),
                   matchKeyEquals(typeId, u -> u.getType().getId()));
    }

    /**
     * Gets a new active unit.
     *
     * @return A {@code Unit} that can be made active.
     */
    public Unit getNextActiveUnit() {
        return nextActiveUnitIterator.next();
    }

    /**
     * Sets a new active unit.
     *
     * @param unit A {@code Unit} to make the next one to be active.
     * @return True if the operation succeeded.
     */
    public boolean setNextActiveUnit(Unit unit) {
        return nextActiveUnitIterator.setNext(unit);
    }

    /**
     * Checks if a new active unit can be made active.
     *
     * @return True if there is a potential active unit.
     */
    public boolean hasNextActiveUnit() {
        return nextActiveUnitIterator.hasNext();
    }

    /**
     * Gets a new going-to unit.
     *
     * @return A {@code Unit} that can be made active.
     */
    public Unit getNextGoingToUnit() {
        return nextGoingToUnitIterator.next();
    }

    /**
     * Sets a new going-to unit.
     *
     * @param unit A {@code Unit} to make the next one to be active.
     * @return True if the operation succeeded.
     */
    public boolean setNextGoingToUnit(Unit unit) {
        return nextGoingToUnitIterator.setNext(unit);
    }

    /**
     * Checks if there is a unit that has a destination.
     *
     * @return True if there is a unit with a destination.
     */
    public boolean hasNextGoingToUnit() {
        return nextGoingToUnitIterator.hasNext();
    }

    /**
     * Reset the player iterators ready for a new turn.
     */
    public void resetIterators() {
        nextActiveUnitIterator.reset();
        nextGoingToUnitIterator.reset();
    }

    /**
     * Get the trade routes defined for this player.
     *
     * @return A copy of the list of {@code TradeRoute}s for this player.
     */
    public final List<TradeRoute> getTradeRoutes() {
        synchronized (this.tradeRoutes) {
            return new ArrayList<>(this.tradeRoutes);
        }
    }

    /**
     * Get number of trade routes defined for this player.
     *
     * @return The trade route count.
     */
    public final int getTradeRouteCount() {
        synchronized (this.tradeRoutes) {
            return this.tradeRoutes.size();
        }
    }

    /**
     * Get the most recently defined trade route.
     *
     * Relies on trade routes *always* being added at the end of the list.
     *
     * @return The most recently defined {@code TradeRoute}.
     */
    public final TradeRoute getNewestTradeRoute() {
        synchronized (this.tradeRoutes) {
            return (this.tradeRoutes.isEmpty()) ? null
                : this.tradeRoutes.get(this.tradeRoutes.size()-1);
        }
    }

    /**
     * Add a new trade route.
     *
     * @param tradeRoute The {@code TradeRoute} to add.
     */
    public final void addTradeRoute(TradeRoute tradeRoute) {
        String name;
        if (tradeRoute != null && (name = tradeRoute.getName()) != null
            && getTradeRouteByName(name, tradeRoute) == null) {
            synchronized (this.tradeRoutes) {
                this.tradeRoutes.add(tradeRoute);
            }
        }
    }

    /**
     * Get a trade route by name.
     *
     * @param name The trade route name.
     * @param exclude An optional {@code TradeRoute} to exclude.
     * @return The {@code TradeRoute} found.
     */
    public final TradeRoute getTradeRouteByName(final String name,
        final TradeRoute exclude) {
        synchronized (this.tradeRoutes) {
            for (TradeRoute t : this.tradeRoutes)
                if (t.getName().equals(name) && t != exclude)
                    return t;
        }
        return null;
    }

    /**
     * Remove a trade route.
     *
     * @param tradeRoute The {@code TradeRoute} to remove.
     * @return A list of units that were formally assigned to the trade route.
     */
    public final List<Unit> removeTradeRoute(TradeRoute tradeRoute) {
        List<Unit> ret;
        synchronized (this.tradeRoutes) {
            ret = (!this.tradeRoutes.remove(tradeRoute))
                ? Collections.<Unit>emptyList()
                : tradeRoute.getAssignedUnits();
        }
        for (Unit u : ret) u.setTradeRoute(null);
        return ret;
    }

    /**
     * Clear the trade routes.
     */
    private void clearTradeRoutes() {
        synchronized (this.tradeRoutes) {
            this.tradeRoutes.clear();
        }
    }

    /**
     * Get a unique name for a new trade route.
     *
     * @return A suitable name.
     */
    public String getNameForTradeRoute() {
        return NameCache.getTradeRouteName(this);
    }

    /**
     * Add an ownable to a caching container.  Not all ownables are
     * cached.
     *
     * @param o The {@code Ownable} to add.
     * @return True if the container changed.
     */
    public boolean addOwnable(Ownable o) {
        return (o instanceof Settlement) ? addSettlement((Settlement)o)
            : (o instanceof Unit) ? addUnit((Unit)o)
            : false;
    }

    /**
     * Remove an ownable from a caching container.  Not all ownables
     * are cached.
     *
     * @param o The {@code Ownable} to remove.
     * @return True if the container changed.
     */
    public boolean removeOwnable(Ownable o) {
        return (o instanceof Settlement) ? removeSettlement((Settlement)o)
            : (o instanceof Unit) ? removeUnit((Unit)o)
            : false;
    }


    //
    // Settlements
    //

    /**
     * Gets a the settlements this player owns.
     *
     * @return The list of {@code Settlements} this player owns.
     */
    public List<Settlement> getSettlementList() {
        synchronized (this.settlements) {
            return new ArrayList<>(this.settlements);
        }
    }

    /**
     * Gets the first settlement this player owns.
     *
     * @return The first {@code Settlement} this player owns.
     */
    public Settlement getFirstSettlement() {
        synchronized (this.settlements) {
            return this.settlements.get(0);
        }
    }

    /**
     * Does this player have any settlements at present.
     *
     * @return True if this player has settlements.
     */
    public boolean hasSettlements() {
        synchronized (this.settlements) {
            return !settlements.isEmpty();
        }
    }

    /**
     * Get the count of settlements.
     *
     * @return The number of settlements this player has.
     */
    public int getSettlementCount() {
        synchronized (this.settlements) {
            return this.settlements.size();
        }
    }

    /**
     * Get the number of port settlements.
     *
     * @return The number of port settlements this player has.
     */
    public final int getNumberOfPorts() {
        if (!isEuropean())
            return 0;

        synchronized (this.settlements) {
            int x = 0;
            for (Settlement s : this.settlements)
                if (s.isConnectedPort()) x++;
            return x;
        }
    }

    /**
     * Get the connected port settlements.
     *
     * @return A list of connected port {@code Colony}s.
     */
    public List<Colony> getConnectedPortList() {
        if (!isEuropean())
            return Collections.<Colony>emptyList();

        synchronized (this.settlements) {
            List<Colony> result = new ArrayList<>();
            for (Settlement s : this.settlements)
                if (s instanceof Colony && s.isConnectedPort())
                    result.add((Colony)s);

            return result;
        }
    }

    /**
     * Get a stream of the connected port settlements.
     *
     * @return A stream of connected port {@code Colony}s.
     */
    public Stream<Colony> getConnectedPorts() {
        return getConnectedPortList().stream();
    }

    /**
     * Does this player own a given settlement.
     *
     * @param settlement The {@code Settlement} to check.
     * @return True if this {@code Player} owns the given
     *     {@code Settlement}.
     */
    public boolean hasSettlement(Settlement settlement) {
        synchronized (this.settlements) {
            return this.settlements.contains(settlement);
        }
    }

    /**
     * Adds a given settlement to this player's list of settlements.
     *
     * @param settlement The {@code Settlement} to add.
     * @return True if the settlements container changed.
     */
    public boolean addSettlement(Settlement settlement) {
        if (settlement == null) return false;
        if (!owns(settlement)) {
            throw new IllegalStateException("Does not own: " + settlement);
        }
        if (hasSettlement(settlement)) return false;
        synchronized (this.settlements) {
            this.settlements.add(settlement);
        }
        return true;
    }

    /**
     * Removes the given settlement from this player's list of settlements.
     *
     * @param settlement The {@code Settlement} to remove.
     * @return True if the settlements container changed.
     */
    public boolean removeSettlement(Settlement settlement) {
        synchronized (this.settlements) {
            return this.settlements.remove(settlement);
        }
    }

    /**
     * Gets the sum of units currently working in the colonies of this
     * player.
     *
     * @return The sum of the units currently working in the colonies.
     */
    public final int getColoniesPopulation() {
        synchronized (this.settlements) {
            int x = 0;
            for (Settlement s : this.settlements)
                if (s instanceof Colony)
                    x += ((Colony)s).getUnitCount();
            return x;
        }
    }

    /**
     * Gets the sum of liberty produced by the player's coloniess.
     *
     * @return The sum of of liberty produced by the player's colonies..
     */
    public final int getColoniesLiberty() {
        synchronized (this.settlements) {
            int x = 0;
            for (Settlement s : this.settlements)
                if (s instanceof Colony)
                    x += ((Colony)s).getLiberty();
            return x;
        }
    }

    /**
     * Gets the {@code Colony} with the given name.
     *
     * @param name The name of the {@code Colony}.
     * @return The {@code Colony} with the given name, or null if
     *     not found.
     */
    public final Colony getColonyByName(String name) {
        synchronized (this.settlements) {
            for (Settlement s : this.settlements)
                if (s instanceof Colony && Utils.equals(name, ((Colony)s).getName()))
                    return (Colony)s;
        }
        return null;
    }

    /**
     * Gets the {@code IndianSettlement} with the given name.
     *
     * @param name The name of the {@code IndianSettlement}.
     * @return The {@code IndianSettlement} with the given name,
     *     or null if not found.
     */
    public IndianSettlement getIndianSettlementByName(String name) {
        synchronized (this.settlements) {
            for (Settlement s : this.settlements)
                if (s instanceof IndianSettlement && Utils.equals(name, ((IndianSettlement)s).getName()))
                    return (IndianSettlement)s;
        }
        return null;
    }

    /**
     * Get a fresh list of all colonies this player owns.
     *
     * @return A list of the {@code Colony}s this player owns.
     */
    public final List<Colony> getColonies() {
        synchronized (this.settlements) {
            List<Colony> result = new ArrayList<>();
            for (Settlement s : this.settlements)
                if (s instanceof Colony)
                    result.add((Colony)s);
            return result;
        }
    }

    /**
     * Get the first colony.
     *
     * @return The first {@code Colony} or null.
     */
    public final Colony getFirstColony() {
        synchronized (this.settlements) {
            for (Settlement s : this.settlements)
                if (s instanceof Colony)
                    return (Colony)s;
        }
        return null;
    }

    /**
     * Get a list of all colonies this player owns, which can
     * bombard enemy ships.
     *
     * @return A list of the {@code Colony}s then can bombard.
     */
    public final List<Colony> getColoniesCanBombard() {
        synchronized (this.settlements) {
            List<Colony> result = new ArrayList<>();
            for (Settlement s : this.settlements)
                if ((s instanceof Colony) && s.canBombardEnemyShip())
                    result.add((Colony)s);
            return result;
        }
    }

    /**
     * Get a sorted list of all colonies this player owns.
     *
     * @param comp A {@code Comparator} to operate on the colony list.
     * @return A fresh list of the {@code Colony}s this player owns.
     */
    public final List<Colony> getSortedColonies(Comparator<Colony> comp) {
        List<Colony> result = getColonies();
        Collections.sort(result, comp);
        return result;
    }

    /**
     * Get a list of all the IndianSettlements this player owns.
     *
     * @return A list of the {@code IndianSettlement}s this player owns.
     */
    public List<IndianSettlement> getIndianSettlements() {
        synchronized (this.settlements) {
            List<IndianSettlement> result = new ArrayList<>();
            for (Settlement s : this.settlements)
                if (s instanceof IndianSettlement)
                    result.add((IndianSettlement)s);
            return result;
        }
    }

    /**
     * Get a list of all the IndianSettlements this player owns, that have
     * been contacted by given player.
     *
     * @return A list of this player's {@code IndianSettlement}s, that have
     * been contacted by given player.
     */
    public List<IndianSettlement> getIndianSettlementsContacted(Player player) {
        synchronized (this.settlements) {
            List<IndianSettlement> result = new ArrayList<>();
            for (Settlement s : this.settlements)
                if (s instanceof IndianSettlement && s.hasContacted(player))
                    result.add((IndianSettlement)s);
            return result;
        }
    }

    /**
     * Get a list of all indian settlements owned by this player with
     * a missionary from a given player.
     *
     * @param p The {@code Player}.
     * @return A list of the {@code IndianSettlement}s with a matching
     *     missionary.
     */
    public List<IndianSettlement> getIndianSettlementsWithMissionary(Player p) {
        synchronized (this.settlements) {
            List<IndianSettlement> result = new ArrayList<>();
            for (Settlement s : this.settlements)
                if ((s instanceof IndianSettlement) && ((IndianSettlement)s).hasMissionary(p))
                    result.add((IndianSettlement)s);
            return result;
        }
    }

    /**
     * Check whether player has an indian settlements owned by this player with
     * a missionary from a given player.
     *
     * @param p The {@code Player}.
     * @return A True if there player has an indian settlement with missionary
     *     from given player.
     */
    public final boolean hasIndianSettlementsWithMissionary(Player p) {
        synchronized (this.settlements) {
            List<IndianSettlement> result = new ArrayList<>();
            for (Settlement s : this.settlements)
                if ((s instanceof IndianSettlement) && ((IndianSettlement)s).hasMissionary(p))
                    return true;
            return false;
        }
    }

    /**
     * Find a {@code Settlement} by name.
     *
     * @param name The name of the {@code Settlement}.
     * @return The {@code Settlement}, or {@code null} if not found.
     **/
    public Settlement getSettlementByName(String name) {
        return (isIndian()) ? getIndianSettlementByName(name)
            : getColonyByName(name);
    }

    /**
     * Gets the port closest to Europe owned by this player.
     *
     * @return This players closest port.
     */
    public Settlement getClosestPortForEurope() {
        int dist = Integer.MAX_VALUE;
        Settlement nearest = null;

        for (Settlement s : getSettlementList()) {
            int d = s.getHighSeasCount();
            if (d < dist) {
                dist = d;
                nearest = s;
            }
        }

        return nearest;
    }


    //
    // Messages and history
    //

    /**
     * Gets all the model messages for this player.
     *
     * @return A copy of the {@code ModelMessage}s for this
     *     {@code Player}.
     */
    public List<ModelMessage> getModelMessages() {
        synchronized (this.modelMessages) {
            return new ArrayList<>(this.modelMessages);
        }
    }

    /**
     * Gets all new messages for this player.
     *
     * @return all The new {@code ModelMessage}s for this
     *     {@code Player}.
     */
    public List<ModelMessage> getNewModelMessages() {
        synchronized (this.modelMessages) {
            return transform(this.modelMessages, m -> !m.hasBeenDisplayed());
        }
    }

    /**
     * Adds a message for this player.
     *
     * @param modelMessage The {@code ModelMessage} to add.
     */
    public void addModelMessage(ModelMessage modelMessage) {
        synchronized (this.modelMessages) {
            this.modelMessages.add(modelMessage);
        }
    }

    /**
     * Refilters the current model messages, removing the ones that
     * are no longer valid.
     *
     * @param options The {@code OptionGroup} for message display
     *     to enforce.
     */
    public void refilterModelMessages(OptionGroup options) {
        synchronized (this.modelMessages) {
            removeInPlace(this.modelMessages, m ->
                !options.getBoolean(m.getMessageType().getOptionName()));
        }
    }

    /**
     * Removes all undisplayed model messages for this player.
     */
    public void removeDisplayedModelMessages() {
        synchronized (this.modelMessages) {
            removeInPlace(this.modelMessages, ModelMessage::hasBeenDisplayed);
        }
    }

    /**
     * Removes all the model messages for this player.
     */
    public void clearModelMessages() {
        synchronized (this.modelMessages) {
            this.modelMessages.clear();
        }
    }

    /**
     * Sometimes an event causes the source (and display) fields in an
     * accumulated model message to become invalid (e.g. Europe disappears
     * on independence).  This routine is for cleaning up such cases.
     *
     * @param source The source field that has become invalid.
     * @param newSource A new source field to replace the old with, or
     *     if null then remove the message
     */
    public void divertModelMessages(FreeColGameObject source,
                                    FreeColGameObject newSource) {
        final Predicate<ModelMessage> sourcePred = m ->
            Utils.equals(m.getSourceId(), source.getId());
        synchronized (this.modelMessages) {
            if (newSource == null) {
                removeInPlace(this.modelMessages, sourcePred);
            } else {
                for (ModelMessage m : transform(this.modelMessages,
                                                sourcePred)) {
                    m.divert(newSource);
                }
            }
        }
    }

    /**
     * Add the tutorial message for the start of the game.
     */
    public void addStartGameMessage() {
        Tile tile = getEntryLocation().getTile();
        String sailTag = (tile == null) ? "unknown"
            : (tile.getX() < tile.getMap().getWidth() / 2) ? "east"
            : "west";
        addModelMessage(new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                                         "model.player.startGame", this)
            .<ModelMessage>addTagged("%direction%", sailTag));
    }

    /**
     * Get the history events for this player.
     *
     * @return A copy of the {@code HistoryEvent}s for this player.
     */
    public final List<HistoryEvent> getHistory() {
        synchronized (this.history) {
            return new ArrayList<>(this.history);
        }
    }

    /**
     * Add a history event to this player.
     *
     * @param event The {@code HistoryEvent} to add.
     */
    public void addHistory(HistoryEvent event) {
        synchronized (this.history) {
            this.history.add(event);
        }
    }

    /**
     * Clear the history events.
     */
    private void clearHistory() {
        synchronized (this.history) {
            this.history.clear();
        }
    }


    //
    // The players view of the Map
    //

    /**
     * Gets the default initial location where the units arriving from
     * {@link Europe} appear on the map.
     *
     * @return The entry {@code Location}.
     * @see Unit#getEntryLocation
     */
    public Location getEntryLocation() {
        return entryLocation;
    }

    /**
     * Sets the default initial location where the units arriving from
     * {@link Europe} appear on the map.
     *
     * @param entryLocation The new entry {@code Location}.
     * @see #getEntryLocation
     */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
    }

    /**
     * Get a default tile to display at the start of the player turn should
     * there not be any active units.
     *
     * Favour the first settlement, followed by the entry tile.
     *
     * @return A suitable {@code Tile}.
     */
    public Tile getFallbackTile() {
        return (this.settlements.size() == 0) ?
            getEntryLocation().getTile() : this.settlements.get(0).getTile();
    }

    /**
     * Get the players high seas.
     *
     * @return The {@code HighSeas} for this player.
     */
    public final HighSeas getHighSeas() {
        return highSeas;
    }

    /**
     * Initialize the highSeas.
     * Needs to be public until the backward compatibility code in
     * FreeColServer is gone.
     */
    public void initializeHighSeas() {
        Game game = getGame();
        highSeas = new HighSeas(game);
        if (europe != null) highSeas.addDestination(europe);
        if (game.getMap() != null ) highSeas.addDestination(game.getMap());
    }

    /**
     * Can this player see a given tile.
     *
     * The tile can be seen if it is in a unit or settlement's line of sight.
     *
     * @param tile The {@code Tile} to check.
     * @return True if this player can see the given {@code Tile}.
     */
    public boolean canSee(Tile tile) {

        /** the visibility map is allocated on-demand and killed on invalidation **/
        TileVisibilityMap vismap = visibilityMap;

        /** if we dont have a map yet, create a new one, while holding a lock **/
        if (vismap == null)
            vismap = makeCanSeeTiles(getGame().getMap());

        return vismap.isVisible(tile);
    }

    /**
     * Forces an update of the {@code canSeeTiles}.
     *
     * This method should be used to invalidate the current
     * {@code canSeeTiles} when something significant changes.
     * The method {@link #makeCanSeeTiles} will be called whenever it
     * is needed.
     *
     * So what is "significant"?  Looking at the makeCanSeeTiles routine
     * suggests the following:
     *
     * - Unit added to map
     * - Unit removed from map
     * - Unit moved on map
     * - Unit type changes (which may change its line-of-sight)
     * - Unit ownership changes
     * - Settlement added to map
     * - Settlement removed from map
     * - Settlement ownership changes
     * - Coronado added (can now see other colonies)
     * - Coronado removed (only in debug mode)
     * - Mission established (if enhanced missionaries enabled)
     * - Mission removed (if enhanced missionaries enabled)
     * - Mission ownership changes (Spanish succession with enhanced
     *                              missionaries enabled)
     * - Map is unexplored (debug mode)
     *
     * Ideally then when any of these events occurs we should call
     * invalidateCanSeeTiles().  However while iCST is quick and
     * cheap, as soon as we then call canSee() the big expensive
     * makeCanSeeTiles will be run.  Often the situation in the server
     * is that several routines with visibility implications will be
     * called in succession.  Usually there, the best solution is to
     * make all the changes and issue the iCST at the end.  So, to
     * make this a bit more visible, routines that change visibility
     * are annotated with a "-vis" comment at both definition and call
     * sites.  Similarly routines that fix up the mess have a "+vis"
     * comment.  Thus it is an error for a -vis to appear without a
     * following +vis (unless the enclosing routine is marked -vis).
     * By convention, we try to avoid cs* routines being -vis.
     */
    public void invalidateCanSeeTiles() {
        visibilityMap = null;
    }

    /**
     * Checks if this player has explored the given tile.
     *
     * @param tile The {@code Tile} to check.
     * @return True if the {@code Tile} has been explored.
     */
    public boolean hasExplored(Tile tile) {
        return tile.isExplored();
    }

    /**
     * Builds a canSeeTiles array.
     *
     * Note that tiles must be tested for null as they may be both
     * valid tiles but yet null during a save game load.
     *
     * Note the use of copies of the unit and settlement lists to
     * avoid nasty surprises due to asynchronous disappearance of
     * members of either.  FIXME: see if this can be relaxed.
     *
     * @param map The {@code Map} to use.
     * @return A canSeeTiles array.
     *
     * note: theoretically we could do completely without a lock,
     * which could cause the bitmap generation twice, while one
     * copy just being thrown away. even though still semantically
     * correct, it's just a waste of resources w/o any gain.
     */
    private synchronized TileVisibilityMap makeCanSeeTiles(Map map) {

        /* ugh, dont have a map yet ? */
        if (map == null) {
            TileVisibilityMap vismap = new TileVisibilityMap(1,1);
            visibilityMap = vismap;
            return vismap;
        }

        TileVisibilityMap vismap = new TileVisibilityMap(map.getWidth(), map.getHeight());

        final Specification spec = getSpecification();
        // Simple case when there is no fog of war: a tile is
        // visible once it is explored.
        if (!spec.getBoolean(GameOptions.FOG_OF_WAR)) {
            Tile t = null;
            for (Iterator<Tile> it = map.getWholeMapIterator(); it.hasNext(); t = it.next())
                if (t != null && t.isExplored()) vismap.setVisible(t);
        }
        else
        {
            // player has units, settlements, (optionally) missions, and
            // extra visibility.
            // Set the PET for visible tiles to the tile itself.

            // Only consider units directly on the map, not those on a
            // carrier or in Europe.
            for (Unit unit : getUnits())
                vismap.setVisible(unit);

            // All the settlements.
            for (Settlement settlement : getSettlementList())
                vismap.setVisible(settlement);

            if (isEuropean()) {

                // All missions if using enhanced missionaries.
                if (spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES))
                    for (Player other : getGame().getLiveNativePlayers(this))
                        for (IndianSettlement is : getIndianSettlementsWithMissionary(this))
                            vismap.setVisible(is);

                // All other European settlements if can see all colonies.
                if (hasAbility(Ability.SEE_ALL_COLONIES))
                    for (Colony c : getGame().getAllColonies(this))
                        vismap.setVisible(c);
            }
        }

        // finally update the current visibility map
        visibilityMap = vismap;
        return vismap;
    }

    //
    // Foreign relations
    //

    /**
     * Gets the hostility this player has against the given player.
     *
     * @param player The other {@code Player}.
     * @return An object representing the tension level.
     */
    public Tension getTension(Player player) {
        if (player == null) throw new IllegalStateException("Null player.");
        Tension newTension = tension.get(player);
        if (newTension == null) {
            newTension = new Tension(Tension.TENSION_MIN);
            tension.put(player, newTension);
        }
        return newTension;
    }

    /**
     * Sets the tension with respect to a given player.
     *
     * @param player The other {@code Player}.
     * @param newTension The new {@code Tension}.
     */
    public void setTension(Player player, Tension newTension) {
        if (player == this || player == null) return;
        tension.put(player, newTension);
    }

    /**
     * Removes all tension with respect to a given player.  Used when a
     * player leaves the game.
     *
     * @param player The {@code Player} to remove tension for.
     */
    public void removeTension(Player player) {
        if (player != null) tension.remove(player);
    }

    /**
     * Does this player ban missions from another player?
     *
     * @param player The other {@code Player} to test.
     * @return True if the given player is banned.
     */
    public boolean missionsBanned(Player player) {
        return bannedMissions != null && bannedMissions.contains(player);
    }

    /**
     * Ban a player from establishing missions to this player.
     *
     * @param player The {@code Player} to ban.
     */
    public void addMissionBan(Player player) {
        if (bannedMissions == null) bannedMissions = new HashSet<>();
        bannedMissions.add(player);
    }

    /**
     * Remove a mission ban from a player.
     *
     * @param player The {@code Player} to clear the ban for.
     */
    public void removeMissionBan(Player player) {
        if (bannedMissions != null) bannedMissions.remove(player);
    }

    /**
     * Gets the stance towards a given player.
     *
     * @param player The other {@code Player} to check.
     * @return The stance.
     */
    public Stance getStance(Player player) {
        return (player == null || stance.get(player.getId()) == null)
            ? Stance.UNCONTACTED
            : stance.get(player.getId());
    }

    /**
     * Sets the stance towards a given player.
     *
     * @param player The {@code Player} to set the
     *     {@code Stance} for.
     * @param newStance The new {@code Stance}.
     * @return True if the stance change was valid.
     * @throws IllegalArgumentException if player is null or this.
     */
    public boolean setStance(Player player, Stance newStance) {
        if (player == null) {
            throw new IllegalArgumentException("Player must not be 'null'.");
        }
        if (player == this) {
            throw new IllegalArgumentException("Cannot set the stance towards ourselves.");
        }
        if (newStance == null) {
            stance.remove(player.getId());
            return true;
        }
        Stance oldStance = stance.get(player.getId());
        if (newStance == oldStance) return true;

        boolean valid = true;;
        if ((newStance == Stance.CEASE_FIRE && oldStance != Stance.WAR)
            || newStance == Stance.UNCONTACTED) {
            valid = false;
        }
        stance.put(player.getId(), newStance);
        return valid;
    }

    /**
     * Is this player at war with the specified one.
     *
     * @param player The other {@code Player} to check.
     * @return True if the players are at war.
     */
    public boolean atWarWith(Player player) {
        return getStance(player) == Stance.WAR;
    }

    /**
     * Checks whether this player is at war with any other player.
     *
     * @return True if this player is at war with any other.
     */
    public boolean isAtWar() {
        return any(getGame().getLivePlayers(), p -> atWarWith(p));
    }

    /**
     * Has this player met contacted the given one?
     *
     * @param player The other {@code Player} to check.
     * @return True if this {@code Player} has contacted the other.
     */
    public boolean hasContacted(Player player) {
        return getStance(player) != Stance.UNCONTACTED;
    }

    /**
     * Has this player has met with any Europeans at all?
     *
     * @return True if this {@code Player} has contacted any Europeans.
     */
    public boolean hasContactedEuropeans() {
        return any(getGame().getLiveEuropeanPlayers(this),
                   p -> hasContacted(p));
    }

    /**
     * Has this player met any natives at all?
     *
     * @return True if this {@code Player} has contacted any natives.
     */
    public boolean hasContactedIndians() {
        return any(getGame().getLiveNativePlayers(this),
                   p -> hasContacted(p));
    }

    /**
     * Set this player as having made initial contact with another player.
     * Always start with PEACE, which can go downhill fast.
     *
     * @param player1 One {@code Player} to check.
     * @param player2 The other {@code Player} to check.
     */
    public static void makeContact(Player player1, Player player2) {
        player1.stance.put(player2.getId(), Stance.PEACE);
        player2.stance.put(player1.getId(), Stance.PEACE);
        player1.setTension(player2, new Tension(Tension.TENSION_MIN));
        player2.setTension(player1, new Tension(Tension.TENSION_MIN));
    }

    /**
     * Gets the price of the given land.
     *
     * @param tile The {@code Tile} to get the price for.
     * @return The price of the land if it is for sale, zero if it is
     *     already ours, unclaimed or unwanted, negative if it is not
     *     for sale.
     */
    public int getLandPrice(Tile tile) {
        final Specification spec = getSpecification();
        Player nationOwner = tile.getOwner();

        if (nationOwner == null || nationOwner == this) {
            return 0; // Freely available
        } else if (tile.hasSettlement()) {
            return -1; // Not for sale
        } else if (nationOwner.isEuropean()) {
            if (tile.getOwningSettlement() != null
                && tile.getOwningSettlement().getOwner() == nationOwner) {
                return -1; // Nailed down by a European colony
            } else {
                return 0; // Claim abandoned or only by tile improvement
            }
        } // Else, native ownership
        int price = spec.getInteger(GameOptions.LAND_PRICE_FACTOR)
            // Only consider specific food types, not the aggregation.
            * sum(spec.getGoodsTypeList(),
                  gt -> gt != spec.getPrimaryFoodType(),
                  gt -> tile.getPotentialProduction(gt, null))
            + 100;
        return (int)applyModifiers(price, getGame().getTurn(),
                                   Modifier.LAND_PAYMENT_MODIFIER);
    }

    /**
     * Access the nation cache.
     *
     * @param player The {@code Player} to get the summary for.
     * @return The current {@code NationSummary} for a player.
     */
    public NationSummary getNationSummary(Player player) {
        return nationCache.get(player);
    }

    /**
     * Update the nation cache.
     *
     * @param player The {@code Player} to get the summary for.
     * @param ns The new {@code NationSummary} for the player.
     */
    public void putNationSummary(Player player, NationSummary ns) {
        nationCache.put(player, ns);
    }

    /**
     * Clear the nation cache.
     */
    public void clearNationCache() {
        nationCache.clear();
    }

    /**
     * Get the strength ratio of this player with respect to another.
     *
     * This relies on an up-to-date nation cache value for the target
     * player.
     *
     * @param other The other {@code Player}.
     * @param naval If true, get the naval strength, else the land strength.
     * @return The strength ratio (strength/sum(strengths)), or negative
     *     on error.
     */
    public double getStrengthRatio(Player other, boolean naval) {
        NationSummary ns = getNationSummary(other);
        if (ns == null) return -1.0;
        int strength = calculateStrength(naval);
        return strengthRatio((double)strength, ns.getMilitaryStrength());
    }

    /**
     * Abstraction of the strength ratio calculation.
     *
     * @param ours The player strength.
     * @param theirs The enemy strength.
     * @return The resulting strength ratio.
     */
    public static double strengthRatio(double ours, double theirs) {
        return (ours == 0.0) ? 0.0 : ours / (ours + theirs);
    }

    //
    // Claiming of tiles
    //

    /**
     * A variety of reasons why a tile can not be claimed, either
     * to found a settlement or just to be used by one, including the
     * double negative NONE == "no reason" case.
     */
    public static enum NoClaimReason implements Named {
        NONE,            // Actually, tile can be claimed
        TERRAIN,         // Not on settleable terrain
        RUMOUR,          // Europeans can not claim tiles with LCR
        WATER,           // Natives do not claim water
        OCCUPIED,        // Hostile unit present.
        SETTLEMENT,      // Settlement present
        WORKED,          // One of our settlements is working this tile
        EUROPEANS,       // Owned by Europeans and not for sale
        NATIVES;         // Owned by natives and they want payment for it

        /**
         * Get a message key describing this reason.
         *
         * @return A message key.
         */
        private String getKey() {
            return "noClaimReason." + getEnumKey(this);
        }

        public String getDescriptionKey() {
            return Messages.descriptionKey("model." + getKey());
        }

        // Implement Named

        /**
         * {@inheritDoc}
         */
        public String getNameKey() {
            return Messages.nameKey("model." + getKey());
        }
    };

    /**
     * Can a tile be owned by this player?
     *
     * @param tile The {@code Tile} to consider.
     * @return True if the tile can be owned by this player.
     */
    public boolean canOwnTile(Tile tile) {
        return canOwnTileReason(tile) == NoClaimReason.NONE;
    }

    /**
     * Can a tile be owned by this player?
     * This is a test of basic practicality and does not consider
     * the full complexity of tile ownership issues.
     *
     * @param tile The {@code Tile} to consider.
     * @return The reason why/not the tile can be owned by this player.
     */
    private NoClaimReason canOwnTileReason(Tile tile) {
        return (any(tile.getUnits(),
                    u -> u.getOwner() != this && u.isOffensiveUnit()))
            ? NoClaimReason.OCCUPIED // The tile is held against us
            : (isEuropean())
            ? ((tile.hasLostCityRumour())
                ? NoClaimReason.RUMOUR
                : NoClaimReason.NONE)
            : ((tile.isLand())
                ? NoClaimReason.NONE
                : NoClaimReason.WATER);
    }

    /**
     * Checks if a tile can be claimed for use by a settlement.
     *
     * @param tile The {@code Tile} to try to claim.
     * @return True if the tile can be claimed to found a settlement.
     */
    public boolean canClaimForSettlement(Tile tile) {
        return canClaimForSettlementReason(tile) == NoClaimReason.NONE;
    }

    /**
     * The test for whether a tile can be freely claimed by a player
     * settlement (freely implies not by purchase or stealing).  The
     * rule for the center tile is different, see below.
     *
     * The tile must be ownable by this player, settlement-free, and
     * either not currently owned, owned by this player and not by
     * another settlement that is using the tile, or owned by someone
     * else who does not want anything for it.  Got that?
     *
     * @param tile The {@code Tile} to try to claim.
     * @return The reason why/not the tile can be claimed.
     */
    public NoClaimReason canClaimForSettlementReason(Tile tile) {
        int price;
        NoClaimReason reason = canOwnTileReason(tile);
        return (reason != NoClaimReason.NONE) ? reason
            : (tile.hasSettlement()) ? NoClaimReason.SETTLEMENT
            : (tile.getOwner() == null) ? NoClaimReason.NONE
            : (tile.getOwner() == this) ? ((tile.isInUse())
                                           ? NoClaimReason.WORKED
                                           : NoClaimReason.NONE)
            : ((price = getLandPrice(tile)) < 0) ? NoClaimReason.EUROPEANS
            : (price > 0) ? NoClaimReason.NATIVES
            : NoClaimReason.NONE;
    }

    /**
     * Can a tile be claimed to found a settlement on?
     *
     * @param tile The {@code Tile} to try to claim.
     * @return True if the tile can be claimed to found a settlement.
     */
    public boolean canClaimToFoundSettlement(Tile tile) {
        return canClaimToFoundSettlementReason(tile) == NoClaimReason.NONE;
    }

    /**
     * Can a tile be claimed to found a settlement on?
     * Almost the same as canClaimForSettlement but there is an extra
     * requirement that the tile be of a settleable type, and some
     * relaxations that allow free center tile acquisition
     *
     * @param tile The {@code Tile} to try to claim.
     * @return The reason why/not the tile can be claimed.
     */
    public NoClaimReason canClaimToFoundSettlementReason(Tile tile) {
        NoClaimReason reason;
        return (!tile.getType().canSettle()) ? NoClaimReason.TERRAIN
            : ((reason = canClaimForSettlementReason(tile))
               != NoClaimReason.NATIVES) ? reason
            : (!tile.getAdjacentColonies().isEmpty()) ? NoClaimReason.SETTLEMENT
            : (canClaimFreeCenterTile(tile)) ? NoClaimReason.NONE
            : NoClaimReason.NATIVES;
    }

    /**
     * Is this tile claimable for a colony center tile under the
     * special provisions of the model.option.buildOnNativeLand option.
     *
     * @param tile The {@code Tile} to try to claim.
     * @return True if the tile can be claimed.
     */
    private boolean canClaimFreeCenterTile(Tile tile) {
        final Specification spec = getGame().getSpecification();
        String build = spec.getString(GameOptions.BUILD_ON_NATIVE_LAND);
        return isEuropean()
            && tile.getOwner() != null
            && tile.getOwner().isIndian()
            && (GameOptions.BUILD_ON_NATIVE_LAND_ALWAYS.equals(build)
                || (GameOptions.BUILD_ON_NATIVE_LAND_FIRST.equals(build)
                    && hasZeroSettlements())
                || (GameOptions.BUILD_ON_NATIVE_LAND_FIRST_AND_UNCONTACTED.equals(build)
                    && hasZeroSettlements()
                    && (tile.getOwner() == null
                        || tile.getOwner().getStance(this)
                        == Stance.UNCONTACTED)));
    }

    /**
     * The second and third cases of buildOnNative land need to test
     * if the player has no settlements yet.  We can not just check
     * that the number of settlement is zero because by the time the
     * settlement is being placed and we are collecting the tiles to
     * claim, the settlement already exists and thus there will
     * already be one settlement--- so we have to check if that one
     * settlement is on the map yet.
     *
     * @return True if the player has no settlements (on the map) yet.
     */
    private boolean hasZeroSettlements() {
        List<Settlement> settlements = getSettlementList();
        return settlements.isEmpty()
            || (settlements.size() == 1
                && settlements.get(0).getTile().getSettlement() == null);
    }

    /**
     * Can the ownership of this tile be claimed for the purposes of
     * making an improvement.  Quick test that does not handle the
     * curly case of tile transfer between colonies, or guarantee
     * success (natives may want to be paid), but just that success is
     * possible.
     *
     * @param tile The {@code Tile} to consider.
     *
     * @return True if the tile ownership can be claimed.
     */
    public boolean canClaimForImprovement(Tile tile) {
        Player owner = tile.getOwner();
        return owner == null || owner == this || getLandPrice(tile) == 0;
    }

    /**
     * Can a tile be acquired from its owners and used for an improvement?
     * Slightly weakens canClaimForImprovement to allow for purchase
     * and/or stealing.
     *
     * @param tile The {@code Tile} to consider.
     * @return True if the tile ownership can be claimed.
     */
    public boolean canAcquireForImprovement(Tile tile) {
        return canClaimForImprovement(tile)
            || getLandPrice(tile) > 0;
    }

    /**
     * Gets the list of tiles that might be claimable by a settlement.
     * We can not do a simple iteration of the rings because this
     * allows settlements to claim tiles across unclaimable gaps
     * (e.g. Aztecs owning tiles on nearby islands).  So we have to
     * only allow tiles that are adjacent to a known connected tile.
     *
     * @param centerTile The intended settlement center {@code Tile}.
     * @param radius The radius of the settlement.
     * @return A list of potentially claimable tiles.
     */
    public List<Tile> getClaimableTiles(Tile centerTile, int radius) {
        List<Tile> tiles = new ArrayList<>();
        List<Tile> layer = new ArrayList<>();
        if (canClaimToFoundSettlement(centerTile)) {
            layer.add(centerTile);
            for (int r = 1; r <= radius; r++) {
                List<Tile> lastLayer = new ArrayList<>(layer);
                tiles.addAll(layer);
                layer.clear();
                for (Tile ll : lastLayer)
                    for (Tile t : ll.getSurroundingTiles(1, 1))
                        if (!tiles.contains(t) && canClaimForSettlement(t))
                            layer.add(t);
            }
            tiles.addAll(layer);
        }
        return tiles;
    }


    //
    // AI helpers for evaluation settlement locations
    //

    /**
     * Gets a list of values for building a {@code Colony} on the
     * given tile for each {@code ColonyValueCategory}.
     *
     * FIXME: tune magic numbers and expose more to the spec.
     *
     * @param tile The {@code Tile}
     * @return A list of values.
     */
    public List<Double> getAllColonyValues(Tile tile) {
        // Want a few settlements before taking risks
        final int LOW_SETTLEMENT_NUMBER = 3;

        // Would like a caravel to reach high seas in 3 moves
        final int LONG_PATH_TILES = 12;

        // Applied once
        final double MOD_HAS_RESOURCE           = 0.75;
        final double MOD_FOOD_LOW               = 0.75;
        final double MOD_INITIAL_FOOD           = 2.0;
        final double MOD_STEAL                  = 0.5;
        final double MOD_INLAND                 = 0.5;

        // Applied per surrounding tile
        final double MOD_OWNED_EUROPEAN         = 0.67;
        final double MOD_OWNED_NATIVE           = 0.8;

        // Applied per goods production, per surrounding tile
        final double MOD_HIGH_PRODUCTION        = 1.2;
        final double MOD_GOOD_PRODUCTION        = 1.1;

        // Applied per occurrence (own colony only one-time), range-dependent.
        final int DISTANCE_MAX = 5;
        final double[] MOD_OWN_COLONY     = {0.0, 0.0, 0.5, 1.50, 1.25};
        final double[] MOD_ENEMY_COLONY   = {0.0, 0.0, 0.4, 0.50, 0.70};
        final double[] MOD_NEUTRAL_COLONY = {0.0, 0.0, 0.7, 0.80, 1.00};
        final double[] MOD_ENEMY_UNIT     = {0.4, 0.5, 0.6, 0.75, 0.90};

        // Goods production in excess of this on a tile counts as good/high
        final int GOOD_PRODUCTION = 4;
        final int HIGH_PRODUCTION = 8;

        // Counting "high" production as 2, "good" production as 1
        // overall food production is considered low/very low if less than...
        final int FOOD_LOW = 4;
        final int FOOD_VERY_LOW = 1;

        // Multiplicative modifiers, to be applied to value later
        List<Double> values = transform(ColonyValueCategory.values(),
                                        alwaysTrue(), v -> 1.0);

        // Penalize certain problems more in the initial colonies.
        double development = Math.min(LOW_SETTLEMENT_NUMBER,
                                      getSettlementCount())
                / (double)LOW_SETTLEMENT_NUMBER;
        int portCount = getNumberOfPorts();

        if (tile.isPolar() && getSettlementCount() < LOW_SETTLEMENT_NUMBER) {
            values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                       NoValueType.POLAR.getDouble());
            return values;
        }

        switch (canClaimToFoundSettlementReason(tile)) {
        case NONE:
            break;
        case TERRAIN: case WATER:
            values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                       NoValueType.TERRAIN.getDouble());
            return values;
        case RUMOUR:
            if (!hasSettlements()) {
                values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                           NoValueType.RUMOUR.getDouble());
                return values;
            }
            values.set(ColonyValueCategory.A_TILE.ordinal(),
                       development);
            break;
        case OCCUPIED: // transient we hope
            values.set(ColonyValueCategory.A_TILE.ordinal(),
                       MOD_ENEMY_UNIT[0]);
            break;
        case SETTLEMENT: case WORKED: case EUROPEANS:
            values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                       NoValueType.SETTLED.getDouble());
            return values;
        case NATIVES: // If we have no ports, we are desperate enough to steal
            if (tile.getOwningSettlement() != null
                && tile.getOwningSettlement().getTile() != null
                && tile.getOwningSettlement().getTile().isAdjacent(tile)) {
                values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                           NoValueType.SETTLED.getDouble());
                return values;
            }
            int price = getLandPrice(tile);
            if (price > 0 && !checkGold(price) && portCount > 0) {
                values.set(ColonyValueCategory.A_TILE.ordinal(),
                           MOD_STEAL);
            }
            break;
        default:
            values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                       NoValueType.BOGUS.getDouble());
            return values;
        }

        // Set up maps for all foods and building materials
        final Specification spec = getSpecification();
        TypeCountMap<GoodsType> production = new TypeCountMap<>();

        // Initialize tile value with food production.
        final GoodsType foodType = spec.getPrimaryFoodType();
        final AbstractGoods bestFood = tile.getBestFoodProduction();
        int initialFood = (bestFood == null) ? 0 : bestFood.getAmount();
        if (initialFood <= FOOD_VERY_LOW) {
            values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                       NoValueType.FOOD.getDouble());
            return values;
        }
        production.incrementCount(foodType, initialFood);
        values.set(ColonyValueCategory.A_PROD.ordinal(),
                   (double)initialFood * foodType.getProductionWeight());

        // Penalty if there is no direct connection to the high seas, or
        // if it is too long.
        int tilesToHighSeas = tile.getHighSeasCount();
        if (tilesToHighSeas < 0) {
            if (portCount < LOW_SETTLEMENT_NUMBER) {
                values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                           NoValueType.INLAND.getDouble());
                return values;
            }
            values.set(ColonyValueCategory.A_EUROPE.ordinal(),
                       MOD_INLAND);
        } else if (tilesToHighSeas >= LONG_PATH_TILES) {
            // Normally penalize in direct proportion to length of
            // path, but scale up to penalizing by the square of the
            // path length for the first colony.
            double trip = (double)LONG_PATH_TILES / tilesToHighSeas;
            values.set(ColonyValueCategory.A_EUROPE.ordinal(),
                       Math.pow(trip, 2.0 - development));
        } else {
            values.set(ColonyValueCategory.A_EUROPE.ordinal(),
                       1.0 + 0.25 * ((double)LONG_PATH_TILES
                           / (LONG_PATH_TILES - tilesToHighSeas)));
        }

        // Penalty for building on a resource tile, because production
        // can not be improved much.
        values.set(ColonyValueCategory.A_RESOURCE.ordinal(),
                   (tile.hasResource()) ? MOD_HAS_RESOURCE : 1.0);

        Set<GoodsType> highProduction = new HashSet<>();
        Set<GoodsType> goodProduction = new HashSet<>();
        for (Tile t : transform(tile.getSurroundingTiles(1,1),
                                isNotNull(Tile::getType))) {
            if (t.getSettlement() != null) { // Should not happen, tested above
                values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                           NoValueType.SETTLED.getDouble());
                return values;
            }

            double pf = 1.0;
            if (t.getOwner() != null && !this.owns(t)) {
                if (t.getOwner().isEuropean()) {
                    if (portCount < LOW_SETTLEMENT_NUMBER) {
                        values.set(ColonyValueCategory.A_OVERRIDE.ordinal(),
                                   NoValueType.SETTLED.getDouble());
                        return values;
                    }
                    values.set(ColonyValueCategory.A_ADJACENT.ordinal(),
                               values.get(ColonyValueCategory.A_ADJACENT.ordinal())
                               * MOD_OWNED_EUROPEAN * development);
                    continue; // Always ignore production from this tile
                } else {
                    pf = MOD_OWNED_NATIVE;
                    if (portCount > 0) pf *= development;
                }
            }

            // Count production
            for (AbstractGoods ag : t.getSortedPotential()) {
                GoodsType type = ag.getType();
                if (type.isFoodType()) type = foodType;
                int amount = ag.getAmount();
                if (!t.isLand()) amount *= development;
                values.set(ColonyValueCategory.A_PROD.ordinal(),
                           values.get(ColonyValueCategory.A_PROD.ordinal())
                           + amount * type.getProductionWeight() * pf);
                production.incrementCount(type, amount);
                // A few tiles with distinct high production are
                // better than many tiles with low production.
                if (amount > HIGH_PRODUCTION) {
                    highProduction.add(type);
                } else if (amount > GOOD_PRODUCTION) {
                    goodProduction.add(type);
                }
            }

            for (Unit u : transform(t.getUnits(),
                                    u -> !owns(u) && u.isOffensiveUnit()
                                        && atWarWith(u.getOwner()))) {
                values.set(ColonyValueCategory.A_ADJACENT.ordinal(),
                    values.get(ColonyValueCategory.A_ADJACENT.ordinal())
                    * MOD_ENEMY_UNIT[1]);
            }
        }

        for (GoodsType g : highProduction) {
            values.set(ColonyValueCategory.A_LEVEL.ordinal(),
                       values.get(ColonyValueCategory.A_LEVEL.ordinal())
                       * MOD_HIGH_PRODUCTION);
            goodProduction.remove(g);
        }
        if (!goodProduction.isEmpty()) {
            values.set(ColonyValueCategory.A_LEVEL.ordinal(),
                       values.get(ColonyValueCategory.A_LEVEL.ordinal())
                       * MOD_GOOD_PRODUCTION * goodProduction.size());
        }

        // Apply modifiers for other settlements and units at distance.
        boolean supportingColony = false;
        for (int radius = 2; radius < DISTANCE_MAX; radius++) {
            for (Tile t : getGame().getMap().getCircleTiles(tile, false,
                                                            radius)) {
                Settlement settlement = t.getSettlement();
                if (settlement != null) {
                    if (owns(settlement)) {
                        if (!supportingColony) {
                            supportingColony = true;
                            values.set(ColonyValueCategory.A_NEARBY.ordinal(),
                                values.get(ColonyValueCategory.A_NEARBY.ordinal())
                                * MOD_OWN_COLONY[radius]);
                        }
                    } else if (atWarWith(settlement.getOwner())) {
                        values.set(ColonyValueCategory.A_NEARBY.ordinal(),
                            values.get(ColonyValueCategory.A_NEARBY.ordinal())
                            * MOD_ENEMY_COLONY[radius]);
                    } else {
                        values.set(ColonyValueCategory.A_NEARBY.ordinal(),
                            values.get(ColonyValueCategory.A_NEARBY.ordinal())
                            * MOD_NEUTRAL_COLONY[radius]);
                    }
                }

                for (Unit u : transform(t.getUnits(),
                                        u -> (!owns(u) && u.isOffensiveUnit()
                                            && atWarWith(u.getOwner())))) {
                    values.set(ColonyValueCategory.A_NEARBY.ordinal(),
                        values.get(ColonyValueCategory.A_NEARBY.ordinal())
                        * MOD_ENEMY_UNIT[radius]);
                }
            }
        }

        // Check availability of key goods
        if (production.getCount(foodType) < FOOD_LOW) {
            values.set(ColonyValueCategory.A_FOOD.ordinal(),
                       values.get(ColonyValueCategory.A_FOOD.ordinal())
                       * MOD_FOOD_LOW);
        }
        int a = ColonyValueCategory.A_GOODS.ordinal() - 1;
        for (GoodsType type : production.keySet()) {
            Integer amount = production.getCount(type);
            double threshold = type.getLowProductionThreshold();
            if (threshold > 0.0) {
                if (++a == values.size()) values.add(1.0);
                if (amount < threshold) {
                    double fraction = (double)amount / threshold;
                    double zeroValue = type.getZeroProductionFactor();
                    values.set(a, (1.0 - fraction) * zeroValue + fraction);
                }
            }
        }

        return values;
    }

    /**
     * Gets the value for building a {@code Colony} on
     * the given tile.
     *
     * FIXME: tune magic numbers and expose more to the spec.
     *
     * @param tile The {@code Tile}
     * @return A score for the tile.
     */
    public int getColonyValue(Tile tile) {
        List<Double> values = getAllColonyValues(tile);
        if (values.get(0) < 0.0) return (int)Math.round(values.get(0));
        double v = 1.0;
        for (Double d : values) v *= d;
        return (int)Math.round(v);
    }


    //
    // Miscellaneous
    //

    /**
     * Standardized log of an instance of cheating by this player.
     *
     * @param what A description of the cheating.
     */
    public void logCheat(String what) {
        logger.finest("CHEAT: " + getGame().getTurn()
            + " " + lastPart(getNationId(), ".")
            + " " + what);
    }

    /**
     * Gets the maximum food consumption of any unit types
     * available to this player.
     *
     * @return A maximum food consumption value.
     */
    public int getMaximumFoodConsumption() {
        if (maximumFoodConsumption < 0) {
            final Specification spec = getSpecification();
            maximumFoodConsumption = max(spec.getUnitTypeList(),
                ut -> ut.isAvailableTo(this),
                ut -> sum(spec.getFoodGoodsTypeList(),
                          ft -> ut.getConsumptionOf(ft)));
        }
        return maximumFoodConsumption;
    }

    /**
     * Does this player own something?
     *
     * @param ownable The {@code Ownable} to check.
     * @return True if the {@code Ownable} is ours.
     */
    public boolean owns(Ownable ownable) {
        return (ownable == null) ? false : this.equals(ownable.getOwner());
    }

    /**
     * Get a {@code FreeColGameObject} with the specified
     * identifier and class, owned by this player.
     *
     * Used mainly in message decoding.
     *
     * @param <T> The actual return type.
     * @param id The object identifier.
     * @param returnClass The expected class of the object.
     * @return The game object, or null if not found.
     */
    public <T extends FreeColGameObject> T getOurFreeColGameObject(String id,
        Class<T> returnClass) {
        T t = getGame().getFreeColGameObject(id, returnClass);
        if (t == null) {
            FreeColGameObject fcgo = getGame().getFreeColGameObject(id);
            throw new RuntimeException("Not a " + returnClass.getName()
                + ": " + id + "/" + fcgo);
        } else if (t instanceof Ownable) {
            if (!owns((Ownable)t)) {
                throw new IllegalStateException(returnClass.getName()
                    + " not owned by " + getId() + ": " + id + "/" + t);
            }
        } else {
            throw new RuntimeException("Not ownable: " + id + "/" + t);
        }
        return t;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        for (Unit unit : getUnits()) {
            if (unit.getOwner() == null) {
                if (fix) {
                    unit.setOwner(this);
                    logger.warning("Fixed missing owner for: " + unit.getId());
                    result = 0;
                } else {
                    logger.warning("Missing owner for: " + unit.getId());
                    result = -1;
                }
            }
            result = Math.min(result, unit.checkIntegrity(fix));
        }
        if (monarch != null) {
            result = Math.min(result, monarch.checkIntegrity(fix));
        }
        return result;
    }


    //
    // Override FreeColObject
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public final FeatureContainer getFeatureContainer() {
        return featureContainer;
    }


    // Serialization

    private static final String ADMIN_TAG = "admin";
    private static final String AI_TAG = "ai";
    private static final String ATTACKED_BY_PRIVATEERS_TAG = "attackedByPrivateers";
    private static final String BANKRUPT_TAG = "bankrupt";
    private static final String BAN_MISSIONS_TAG = "banMissions";
    private static final String CURRENT_FATHER_TAG = "currentFather";
    private static final String DEAD_TAG = "dead";
    private static final String ENTRY_LOCATION_TAG = "entryLocation";
    private static final String FOUNDING_FATHERS_TAG = "foundingFathers";
    private static final String GOLD_TAG = "gold";
    private static final String IMMIGRATION_TAG = "immigration";
    private static final String IMMIGRATION_REQUIRED_TAG = "immigrationRequired";
    private static final String LIBERTY_TAG = "liberty";
    private static final String INDEPENDENT_NATION_NAME_TAG = "independentNationName";
    private static final String INTERVENTION_BELLS_TAG = "interventionBells";
    private static final String NATION_ID_TAG = "nationId";
    private static final String NATION_TYPE_TAG = "nationType";
    private static final String NEW_LAND_NAME_TAG = "newLandName";
    private static final String OFFERED_FATHERS_TAG = "offeredFathers";
    private static final String OLD_SOL_TAG = "oldSoL";
    private static final String PLAYER_TAG = "player";
    private static final String PLAYER_TYPE_TAG = "playerType";
    private static final String READY_TAG = "ready";
    private static final String SCORE_TAG = "score";
    private static final String STANCE_TAG = "stance";
    private static final String TAX_TAG = "tax";
    private static final String TENSION_TAG = "tension";
    private static final String USERNAME_TAG = "username";
    // @compat 0.10.7
    private static final String OLD_NATION_ID_TAG = "nationID";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(USERNAME_TAG, name);

        xw.writeAttribute(NATION_ID_TAG, nationId);

        if (nationType != null) {
            xw.writeAttribute(NATION_TYPE_TAG, nationType);
        }

        xw.writeAttribute(ADMIN_TAG, admin);

        xw.writeAttribute(READY_TAG, ready);

        xw.writeAttribute(DEAD_TAG, dead);

        xw.writeAttribute(PLAYER_TYPE_TAG, playerType);

        xw.writeAttribute(AI_TAG, ai);

        if (xw.validFor(this)) {

            xw.writeAttribute(BANKRUPT_TAG, bankrupt);

            xw.writeAttribute(TAX_TAG, tax);

            xw.writeAttribute(GOLD_TAG, gold);

            xw.writeAttribute(IMMIGRATION_TAG, immigration);

            xw.writeAttribute(LIBERTY_TAG, liberty);

            xw.writeAttribute(INTERVENTION_BELLS_TAG, interventionBells);

            if (currentFather != null) {
                xw.writeAttribute(CURRENT_FATHER_TAG, currentFather);
            }

            xw.writeAttribute(IMMIGRATION_REQUIRED_TAG, immigrationRequired);

            xw.writeAttribute(ATTACKED_BY_PRIVATEERS_TAG, attackedByPrivateers);

            xw.writeAttribute(OLD_SOL_TAG, oldSoL);

            xw.writeAttribute(SCORE_TAG, score);
        }

        if (newLandName != null) {
            xw.writeAttribute(NEW_LAND_NAME_TAG, newLandName);
        }

        if (independentNationName != null) {
            xw.writeAttribute(INDEPENDENT_NATION_NAME_TAG, independentNationName);
        }

        if (entryLocation != null) {
            xw.writeLocationAttribute(ENTRY_LOCATION_TAG, entryLocation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (market != null) market.toXML(xw);

        if (xw.validFor(this)) {

            for (Ability ability : transform(getSortedAbilities(),
                                             Ability::isIndependent)) {
                ability.toXML(xw);
            }

            final int turn = getGame().getTurn();
            for (Modifier modifier : transform(getSortedModifiers(), m ->
                    m.isTemporary() && !m.isOutOfDate(turn))) {
                modifier.toXML(xw);
            }

            for (Player p : sort(tension.keySet())) {
                xw.writeStartElement(TENSION_TAG);

                xw.writeAttribute(PLAYER_TAG, p);

                xw.writeAttribute(VALUE_TAG, tension.get(p).getValue());

                xw.writeEndElement();
            }

            if (bannedMissions != null) {
                for (Player p : sort(bannedMissions)) {
                    xw.writeStartElement(BAN_MISSIONS_TAG);

                    xw.writeAttribute(PLAYER_TAG, p.getId());

                    xw.writeEndElement();
                }
            }

            for (Entry<String, Stance> e : mapEntriesByKey(stance)) {
                Stance s = e.getValue();
                if (s == Stance.UNCONTACTED) continue;

                xw.writeStartElement(STANCE_TAG);

                xw.writeAttribute(PLAYER_TAG, e.getKey());

                xw.writeAttribute(VALUE_TAG, s);

                xw.writeEndElement();
            }

            for (HistoryEvent event : getHistory()) { // Already in order
                event.toXML(xw);
            }

            for (TradeRoute route : sort(getTradeRoutes())) {
                route.toXML(xw);
            }

            if (highSeas != null) highSeas.toXML(xw);

            xw.writeToListElement(FOUNDING_FATHERS_TAG, foundingFathers);

            xw.writeToListElement(OFFERED_FATHERS_TAG, offeredFathers);

            if (europe != null) europe.toXML(xw);

            if (monarch != null) monarch.toXML(xw);

            for (ModelMessage m : getModelMessages()) m.toXML(xw);

            if (lastSales != null) {
                for (LastSale ls : sort(lastSales.values())) {
                    ls.toXML(xw);
                }
            }

        } else {
            Player player = xw.getClientPlayer();
            Tension t = getTension(player);
            if (t != null) {
                xw.writeStartElement(TENSION_TAG);

                xw.writeAttribute(PLAYER_TAG, player);

                xw.writeAttribute(VALUE_TAG, t.getValue());

                xw.writeEndElement();
            }

            if (missionsBanned(player)) {
                xw.writeStartElement(BAN_MISSIONS_TAG);

                xw.writeAttribute(PLAYER_TAG, player.getId());

                xw.writeEndElement();
            }

            Stance s = getStance(player);
            if (s != null && s != Stance.UNCONTACTED) {
                xw.writeStartElement(STANCE_TAG);

                xw.writeAttribute(PLAYER_TAG, player);

                xw.writeAttribute(VALUE_TAG, s);

                xw.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();
        final Game game = getGame();

        name = xr.getAttribute(USERNAME_TAG, (String)null);
        // @compat 0.11.5
        if (name.startsWith("model.nation.")) name = Messages.message(name);
        // end @compat 0.11.5

        nationId = xr.getAttribute(NATION_ID_TAG,
            // @compat 0.10.7
            xr.getAttribute(OLD_NATION_ID_TAG,
            // end @compat 0.10.7
                (String)null));

        if (isUnknownEnemy()) {
            nationType = null;
        } else {
            nationType = xr.getType(spec, NATION_TYPE_TAG,
                                    NationType.class, (NationType)null);
        }

        admin = xr.getAttribute(ADMIN_TAG, false);

        gold = xr.getAttribute(GOLD_TAG, 0);

        immigration = xr.getAttribute(IMMIGRATION_TAG, 0);

        liberty = xr.getAttribute(LIBERTY_TAG, 0);

        interventionBells = xr.getAttribute(INTERVENTION_BELLS_TAG, 0);

        oldSoL = xr.getAttribute(OLD_SOL_TAG, 0);

        score = xr.getAttribute(SCORE_TAG, 0);

        ready = xr.getAttribute(READY_TAG, false);

        ai = xr.getAttribute(AI_TAG, false);

        dead = xr.getAttribute(DEAD_TAG, false);

        bankrupt = xr.getAttribute(BANKRUPT_TAG, false);

        tax = xr.getAttribute(TAX_TAG, 0);

        changePlayerType(xr.getAttribute(PLAYER_TYPE_TAG,
                                         PlayerType.class, (PlayerType)null));

        currentFather = xr.getType(spec, CURRENT_FATHER_TAG,
                                   FoundingFather.class, (FoundingFather)null);

        immigrationRequired = xr.getAttribute(IMMIGRATION_REQUIRED_TAG, 12);

        newLandName = xr.getAttribute(NEW_LAND_NAME_TAG, (String)null);

        independentNationName = xr.getAttribute(INDEPENDENT_NATION_NAME_TAG,
                                                (String)null);

        attackedByPrivateers = xr.getAttribute(ATTACKED_BY_PRIVATEERS_TAG,
                                               false);

        entryLocation = xr.getLocationAttribute(game, ENTRY_LOCATION_TAG,
                                                true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        tension.clear();
        if (bannedMissions != null) bannedMissions.clear();
        stance.clear();
        foundingFathers.clear();
        offeredFathers.clear();
        europe = null;
        monarch = null;
        clearHistory();
        clearTradeRoutes();
        if (xr.getReadScope() == FreeColXMLReader.ReadScope.SERVER) {
            // ModelMessages are always sent direct to the client, not
            // passed through the player structure, so only clear the
            // current list on the server side.
            clearModelMessages();
        }
        lastSales = null;
        highSeas = null;
        featureContainer.clear();
        if (nationType != null) addFeatures(nationType);

        super.readChildren(xr);

        // @compat 0.10.7
        // Fixup production modifiers deriving from founding fathers
        final Specification spec = getSpecification();
        for (Modifier m : transform(getModifiers(), isNotNull(Modifier::getSource))) {
            String type = spec.fatherGoodsFixMap.get(m.getSource().getId());
            if (type != null && m.getId().equals(type)) {
                m.requireNegatedPersonScope();
            }
        }
        // end @compat 0.10.7

        recalculateBellsBonus(); // Bells bonuses depend on tax
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (BAN_MISSIONS_TAG.equals(tag)) {
            Player player = xr.makeFreeColObject(game, PLAYER_TAG,
                                                 Player.class, true);
            if (player != null && player.isEuropean()) addMissionBan(player);
            xr.closeTag(BAN_MISSIONS_TAG);

        } else if (FOUNDING_FATHERS_TAG.equals(tag)) {
            List<FoundingFather> ffs = xr.readList(spec, FOUNDING_FATHERS_TAG,
                                                   FoundingFather.class);
            if (ffs != null) {
                for (FoundingFather ff : ffs) {
                    addFather(ff); // addFather adds the features
                }
            }

        } else if (OFFERED_FATHERS_TAG.equals(tag)) {
            List<FoundingFather> ofs = xr.readList(spec, OFFERED_FATHERS_TAG,
                                                   FoundingFather.class);
            offeredFathers.addAll(ofs);

        } else if (STANCE_TAG.equals(tag)) {
            Player player = xr.makeFreeColObject(game, PLAYER_TAG,
                                                 Player.class, true);
            stance.put(player.getId(),
                xr.getAttribute(VALUE_TAG, Stance.class, Stance.UNCONTACTED));
            xr.closeTag(STANCE_TAG);

        } else if (TENSION_TAG.equals(tag)) {
            tension.put(xr.makeFreeColObject(game, PLAYER_TAG,
                                             Player.class, true),
                        new Tension(xr.getAttribute(VALUE_TAG, 0)));
            xr.closeTag(TENSION_TAG);

        } else if (Ability.TAG.equals(tag)) {
            Ability ability = new Ability(xr, spec);
            if (ability.isIndependent()) addAbility(ability);

        } else if (Europe.TAG.equals(tag)) {
            europe = xr.readFreeColObject(game, Europe.class);

        } else if (HighSeas.TAG.equals(tag)) {
            highSeas = xr.readFreeColObject(game, HighSeas.class);

        } else if (HistoryEvent.TAG.equals(tag)) {
            addHistory(new HistoryEvent(xr));

        } else if (LastSale.TAG.equals(tag)) {
            addLastSale(new LastSale(xr));

        } else if (Market.TAG.equals(tag)) {
            market = xr.readFreeColObject(game, Market.class);

        } else if (ModelMessage.TAG.equals(tag)) {
            addModelMessage(new ModelMessage(xr));

        } else if (Modifier.TAG.equals(tag)) {
            Modifier modifier = new Modifier(xr, spec);
            if (modifier.isIndependent()) addModifier(modifier);

        } else if (Monarch.TAG.equals(tag)) {
            monarch = xr.readFreeColObject(game, Monarch.class);

        } else if (TradeRoute.TAG.equals(tag)) {
            addTradeRoute(xr.readFreeColObject(game, TradeRoute.class));

        } else {
            super.readChild(xr);
        }
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
    public String toString() {
        return nationId;
    }
}
