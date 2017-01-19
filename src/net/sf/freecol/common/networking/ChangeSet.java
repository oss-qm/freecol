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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Feature;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.AnimateAttackMessage;
import net.sf.freecol.common.networking.AnimateMoveMessage;
import net.sf.freecol.common.networking.AttributeMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.SetDeadMessage;
import net.sf.freecol.common.networking.SetStanceMessage;
import net.sf.freecol.common.networking.SpySettlementMessage;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.DOMUtils;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;


/**
 * Changes to be sent to the client.
 */
public class ChangeSet {

    /** Compare changes by ascending priority. */
    private static final Comparator<Change> changeComparator
        = Comparator.comparingInt(Change::getPriority);

    // Convenient way to specify the relative priorities of the fixed
    // change types in one place.
    public static enum ChangePriority {
        CHANGE_ATTRIBUTE(-1), // N/A
        CHANGE_ANIMATION(0),  // Do animations first
        CHANGE_REMOVE(100),   // Do removes last
        CHANGE_STANCE(5),     // Do stance before updates
        CHANGE_OWNED(20),     // Do owned changes after updates
        CHANGE_UPDATE(10),    // There are a lot of updates
        // Symbolic priorities used by various non-fixed types
        CHANGE_EARLY(1),
        CHANGE_NORMAL(15),
        CHANGE_LATE(90);

        private final int level;

        ChangePriority(int level) {
            this.level = level;
        }

        public int getPriority() {
            return level;
        }
    }

    private final ArrayList<Change> changes;


    /**
     * Class to control the visibility of a change.
     */
    public static class See {
        private static final int ALL = 1;
        private static final int PERHAPS = 0;
        private static final int ONLY = -1;
        private ServerPlayer seeAlways;
        private ServerPlayer seePerhaps;
        private ServerPlayer seeNever;
        private final int type;

        private See(int type) {
            this.seeAlways = this.seePerhaps = this.seeNever = null;
            this.type = type;
        }

        /**
         * Check this visibility with respect to a player.
         *
         * @param player The {@code ServerPlayer} to consider.
         * @param perhapsResult The result if the visibility is ambiguous.
         * @return True if the player satisfies the visibility test.
         */
        public boolean check(ServerPlayer player, boolean perhapsResult) {
            return (player == null) ? (type == ALL)
                : (seeNever == player) ? false
                : (seeAlways == player) ? true
                : (seePerhaps == player) ? perhapsResult
                : (type == ALL) ? true
                : (type == ONLY) ? false
                : perhapsResult;
        }

        // Use these public constructor-like functions to define the
        // visibility of changes.

        /**
         * Make this change visible to all players.
         *
         * @return a {@code See} value
         */
        public static See all() {
            return new See(ALL);
        }

        /**
         * Make this change visible to all players, provided they can
         * see the objects that are being changed.
         *
         * @return a {@code See} value
         */
        public static See perhaps() {
            return new See(PERHAPS);
        }

        /**
         * Make this change visible only to the given player.
         *
         * @param player a {@code ServerPlayer} value
         * @return a {@code See} value
         */
        public static See only(ServerPlayer player) {
            return new See(ONLY).always(player);
        }

        // Use these to modify a See visibility.

        /**
         * Make this change visible to the given player.
         *
         * @param player a {@code ServerPlayer} value
         * @return a {@code See} value
         */
        public See always(ServerPlayer player) {
            seeAlways = player;
            return this;
        }

        /**
         * Make this change visible to the given player, provided the
         * player can see the objects being changed.
         *
         * @param player a {@code ServerPlayer} value
         * @return a {@code See} value
         */
        public See perhaps(ServerPlayer player) {
            seePerhaps = player;
            return this;
        }

        /**
         * Make this change invisible to the given player.
         *
         * @param player a {@code ServerPlayer} value
         * @return a {@code See} value
         */
        public See except(ServerPlayer player) {
            seeNever = player;
            return this;
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append((type == ALL) ? "ALL" : (type == PERHAPS) ? "PERHAPS"
                : (type == ONLY) ? "ONLY" : "BADTYPE");
            if (seeAlways != null) {
                sb.append(",always(").append(seeAlways.getId()).append(')');
            }
            if (seePerhaps != null) {
                sb.append(",perhaps(").append(seePerhaps.getId()).append(')');
            }
            if (seeNever != null) {
                sb.append(",never(").append(seeNever.getId()).append(')');
            }
            return sb.toString();
        }
    }

    /**
     * Abstract template for all types of Change.
     */
    private abstract static class Change {

        /**
         * The visibility of the change.
         */
        protected final See see;


        /**
         * Make a new Change.
         *
         * @param see The visibility.
         */
        public Change(See see) {
            this.see = see;
        }


        /**
         * Does this Change operate on the given object?
         *
         * @param fcgo The {@code FreeColGameObject} to check.
         * @return True if the object is a subject of this change.
         */
        public boolean matches(FreeColGameObject fcgo) {
            return false;
        }

        /**
         * Gets the sort priority of a change, to be used by the
         * changeComparator.
         *
         * @return The sort priority.
         */
        public abstract int getPriority();

        /**
         * Should a player be notified of this Change?
         *
         * @param serverPlayer The {@code ServerPlayer} to consider.
         * @return True if this {@code Change} should be sent.
         */
        public boolean isNotifiable(ServerPlayer serverPlayer) {
            return see.check(serverPlayer, isPerhapsNotifiable(serverPlayer));
        }

        /**
         * Should a player be notified of a Change for which the
         * visibility is delegated to the change type, allowing
         * special change-specific overrides.
         *
         * This is false by default, subclasses should override when
         * special case handling is required.
         *
         * @param serverPlayer The {@code ServerPlayer} to consider.
         * @return False.
         */
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return false;
        }

        /**
         * Are the secondary changes consequent to this Change?
         *
         * @param serverPlayer The {@code ServerPlayer} to consider.
         * @return A list of secondary {@code Change}s or the
         *     empty list if there are none, which is usually the case.
         */
        public List<Change> consequences(ServerPlayer serverPlayer) {
            return Collections.<Change>emptyList();
        }

        /**
         * Can this Change be directly converted to an Element?
         *
         * @return True if this change can be directly converted to an Element.
         */
        public boolean convertsToElement() {
            return true;
        }

        /**
         * Specialize a Change for a particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document} to build the element in.
         * @return An {@code Element} encapsulating this change.
         */
        public abstract Element toElement(ServerPlayer serverPlayer,
                                          Document doc);

        /**
         * Some changes can not be directly specialized, but need to be
         * directly attached to an element.
         *
         * @param element The {@code Element} to attach to.
         */
        public abstract void attachToElement(Element element);
    }

    /**
     * Encapsulate an attack.
     */
    private static class AttackChange extends Change {

        private final Unit attacker;
        private final Unit defender;
        private final boolean success;
        private final boolean defenderInSettlement;


        /**
         * Build a new AttackChange.
         *
         * Note that we must copy attackers and defenders because a
         * successful attacker can move, any an unsuccessful
         * participant can die, and unsuccessful defenders can be
         * captured.  Furthermore for defenders, insufficient
         * information is serialized when a unit is inside a
         * settlement, but if unscoped too much is disclosed.  So we
         * make a copy and neuter it.
         *
         * We have to remember if the defender was in a settlement
         * because by the time serialization occurs the settlement
         * might have been destroyed.
         *
         * We just have to accept that combat animation is an
         * exception to the normal visibility rules.
         *
         * @param see The visibility of this change.
         * @param attacker The {@code Unit} that is attacking.
         * @param defender The {@code Unit} that is defending.
         * @param success Did the attack succeed.
         */
        public AttackChange(See see, Unit attacker, Unit defender,
                            boolean success) {
            super(see);
            Game game = attacker.getGame();
            this.defenderInSettlement = defender.getTile().hasSettlement();
            this.attacker = attacker.copy(game, Unit.class);
            this.attacker.setLocationNoUpdate(this.attacker.getTile());
            this.defender = defender.copy(game, Unit.class);
            this.defender.setLocationNoUpdate(this.defender.getTile());
            this.defender.setWorkType(null);
            this.defender.setState(Unit.UnitState.ACTIVE);
            this.success = success;
        }

        /**
         * Is the attacker visible to a player?
         *
         * @return The attacker visibility.
         */
        private boolean attackerVisible(ServerPlayer serverPlayer) {
            return serverPlayer.canSeeUnit(this.attacker);
        }

        /**
         * Is the defender visible to a player?
         *
         * A false positive can occur as the defender may *start*
         * invisible because it is in a settlement, but the settlement
         * falls, exposing the defender (the defender dies).
         * Defenders in settlements must always be considered to be
         * invisible to other players as the animation happens while
         * the settlement stands.
         *
         * @return The defender visibility.
         */
        private boolean defenderVisible(ServerPlayer serverPlayer) {
            return serverPlayer.canSeeUnit(this.defender)
                && (!this.defenderInSettlement
                    || serverPlayer.owns(this.defender));
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_ANIMATION".
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_ANIMATION.getPriority();
        }

        /**
         * Should a player perhaps be notified of this attack?  Do not
         * use canSeeUnit because that gives a false negative for
         * units in settlements, which should be animated.
         *
         * @param serverPlayer The {@code ServerPlayer} to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return serverPlayer == attacker.getOwner()
                || serverPlayer == defender.getOwner()
                || (serverPlayer.canSee(attacker.getTile())
                    && serverPlayer.canSee(defender.getTile()));
        }

        /**
         * Specialize a AttackChange into an "animateAttack" element
         * for a particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return An "animateAttack" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            return new AnimateAttackMessage(attacker, defender, success,
                !attackerVisible(serverPlayer), !defenderVisible(serverPlayer))
                .attachToDocument(doc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(attacker.getId())
                .append('@').append(attacker.getTile().getId())
                .append(' ').append(success)
                .append(' ').append(defender.getId())
                .append('@').append(defender.getTile().getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate an attribute change.
     */
    private static class AttributeChange extends Change {
        private final String key;
        private final String value;

        /**
         * Build a new AttributeChange.
         *
         * @param see The visibility of this change.
         * @param key A key {@code String}.
         * @param value The corresponding value as a {@code String}.
         */
        public AttributeChange(See see, String key, String value) {
            super(see);
            this.key = key;
            this.value = value;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_ATTRIBUTE", attributes are special.
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_ATTRIBUTE.getPriority();
        }

        /**
         * AttributeChanges are tacked onto the final Element, not converted
         * directly.
         *
         * @return false.
         */
        @Override
        public boolean convertsToElement() {
            return false;
        }

        /**
         * We do not specialize AttributeChanges.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return Null.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            return null;
        }

        /**
         * Tack attributes onto the element.
         *
         * @param element The {@code Element} to attach to.
         */
        @Override
        public void attachToElement(Element element) {
            element.setAttribute(key, value);
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(key)
                .append('=').append(value)
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a Message.
     */
    private static class MessageChange extends Change {
        private final ChangePriority priority;
        private final DOMMessage message;

        /**
         * Build a new MessageChange.
         *
         * @param see The visibility of this change.
         * @param priority The priority of the change.
         * @param message The {@code Message} to add.
         */
        public MessageChange(See see, ChangePriority priority,
                             DOMMessage message) {
            super(see);
            this.priority = priority;
            this.message = message;
        }

        /**
         * Gets the sort priority.
         *
         * @return The priority.
         */
        @Override
        public int getPriority() {
            return priority.getPriority();
        }

        /**
         * Specialize a MessageChange to a particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return An element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = message.toXMLElement();
            return (Element) doc.importNode(element, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(message)
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a move.
     */
    private static class MoveChange extends Change {

        private final Unit unit;
        private final Location oldLocation;
        private final Tile newTile;


        /**
         * Build a new MoveChange.
         *
         * @param see The visibility of this change.
         * @param unit The {@code Unit} that is moving.
         * @param oldLocation The location from which the unit is moving.
         * @param newTile The {@code Tile} to which the unit is moving.
         */
        public MoveChange(See see, Unit unit, Location oldLocation,
                          Tile newTile) {
            super(see);
            this.oldLocation = oldLocation;
            this.newTile = newTile;
            if (unit.isOnCarrier()) {
                // Change the unit to a version with a link to an otherwise
                // empty carrier.
                Game game = unit.getGame();
                Unit carrier = unit.getCarrier().copy(game, Unit.class);
                for (Unit u : carrier.getUnitList()) {
                    if (u.getId().equals(unit.getId())) {
                        unit = u;
                    } else {
                        carrier.remove(u);
                    }
                }
                carrier.removeAll();
            }
            this.unit = unit;
        }


        /**
         * Can a player see the old tile?
         *
         * @param serverPlayer The {@code ServerPlayer} to test.
         * @return True if the old tile is visible.
         */
        private boolean seeOld(ServerPlayer serverPlayer) {
            Tile oldTile = oldLocation.getTile();
            return serverPlayer.owns(unit)
                || (oldTile != null
                    && serverPlayer.canSee(oldTile)
                    && !oldTile.hasSettlement()
                    && !(oldLocation instanceof Unit));
        }

        /**
         * Can a player see the new tile?
         *
         * @param serverPlayer The {@code ServerPlayer} to test.
         * @return True if the new tile is visible.
         */
        private boolean seeNew(ServerPlayer serverPlayer) {
            return serverPlayer.canSeeUnit(unit);
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_ANIMATION"
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_ANIMATION.getPriority();
        }

        /**
         * Should a player perhaps be notified of this move?
         *
         * @param serverPlayer The {@code ServerPlayer} to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return seeOld(serverPlayer) || seeNew(serverPlayer);
        }

        /**
         * There are consequences to a move.  If the player can not
         * see the unit after the move, it should be removed.
         *
         * @param serverPlayer The {@code ServerPlayer} to notify.
         * @return A RemoveChange if the unit disappears (but not if it
         *     is destroyed, that is handled elsewhere).
         */
        @Override
        public List<Change> consequences(ServerPlayer serverPlayer) {
            if (seeOld(serverPlayer) && !seeNew(serverPlayer)
                && !unit.isDisposed()) {
                List<Unit> ul = new ArrayList<>();
                ul.add(unit);
                List<Change> changes = new ArrayList<>();
                changes.add(new RemoveChange(See.only(serverPlayer),
                                             unit.getLocation(), ul));
                return changes;
            }
            return Collections.<Change>emptyList();
        }

        /**
         * Specialize a MoveChange into an "animateMove" element for a
         * particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return An "animateMove" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            return new AnimateMoveMessage(unit, oldLocation.getTile(), newTile,
                                          !seeOld(serverPlayer))
                .attachToDocument(doc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(unit.getId())
                .append(' ').append(oldLocation.getId())
                .append(' ').append(newTile.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a FreeColGameObject update.
     */
    private static class ObjectChange extends Change {
        protected final FreeColGameObject fcgo;

        /**
         * Build a new ObjectChange for a single object.
         *
         * @param see The visibility of this change.
         * @param fcgo The {@code FreeColGameObject} to update.
         */
        public ObjectChange(See see, FreeColGameObject fcgo) {
            super(see);
            this.fcgo = fcgo;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(FreeColGameObject fcgo) {
            return this.fcgo == fcgo;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_UPDATE"
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_UPDATE.getPriority();
        }

        /**
         * Should a player perhaps be notified of this update?
         *
         * @param serverPlayer The {@code ServerPlayer} to notify.
         * @return True if the object update can is notifiable.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            if (fcgo instanceof Unit) {
                // Units have a precise test, use that rather than
                // the more general interface-based tests.
                return serverPlayer.canSeeUnit((Unit)fcgo);
            }
            // If we own it, we can see it.
            if (fcgo instanceof Ownable && serverPlayer.owns((Ownable)fcgo)) {
                return true;
            }
            // We do not own it, so the only way we could see it is if
            // it is on the map.  Would like to use getTile() to
            // decide that, but this will include ColonyTiles, which
            // report the colony center tile, yet should never be visible.
            // So just brutally disallow WorkLocations which should always
            // be invisible inside colonies.
            if (fcgo instanceof WorkLocation) {
                return false;
            }
            if (fcgo instanceof Location) {
                Tile tile = ((Location)fcgo).getTile();
                return serverPlayer.canSee(tile);
            }
            return false;
        }

        /**
         * Specialize a ObjectChange to a particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return An "update" element, or null if the update should not
         *     be visible to the player.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("update");
            element.appendChild(DOMUtils.toXMLElement(fcgo, doc, serverPlayer));
            return element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(fcgo.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a partial update of a FreeColGameObject.
     */
    private static class PartialObjectChange extends ObjectChange {
        private final String[] fields;

        /**
         * Build a new PartialObjectChange for a single object.
         *
         * @param see The visibility of this change.
         * @param fcgo The {@code FreeColGameObject} to update.
         * @param fields The fields to update.
         */
        public PartialObjectChange(See see, FreeColGameObject fcgo,
                                   String... fields) {
            super(see, fcgo);
            this.fields = fields;
        }


        /**
         * Should a player perhaps be notified of this update?
         *
         * @param serverPlayer The {@code ServerPlayer} to notify.
         * @return False.  Revert to default from ObjectChange special case.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            return false;
        }

        /**
         * Specialize a PartialObjectChange to a particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return An "update" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("update");
            element.appendChild(DOMUtils.toXMLElementPartial(fcgo, doc, fields));
            return element;
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(fcgo.getId());
            for (String f : fields) sb.append(' ').append(f);
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a new player change.
     */
    private static class PlayerChange extends Change {
        private final ServerPlayer player;

        /**
         * Build a new PlayerChange.
         *
         * @param see The visibility of this change.
         * @param player The {@code Player} to add.
         */
        public PlayerChange(See see, ServerPlayer player) {
            super(see);
            this.player = player;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_EARLY".
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_EARLY.getPriority();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isNotifiable(ServerPlayer serverPlayer) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("addPlayer");
            element.appendChild(DOMUtils.toXMLElement(this.player, doc, serverPlayer));
            return element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(player.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulates removing some objects.
     *
     * -vis: If removing settlements or units, visibility changes.
     */
    private static class RemoveChange extends Change {
        private final Tile tile;
        private final FreeColGameObject fcgo;
        private final List<? extends FreeColGameObject> contents;

        /**
         * Build a new RemoveChange for an object that is disposed.
         *
         * @param see The visibility of this change.
         * @param loc The {@code Location} where the object was.
         * @param objects The {@code FreeColGameObject}s to remove.
         */
        public RemoveChange(See see, Location loc,
                            List<? extends FreeColGameObject> objects) {
            super(see);
            this.tile = (loc instanceof Tile) ? (Tile)loc : null;
            this.contents = objects;
            this.fcgo = this.contents.remove(this.contents.size() - 1);
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_REMOVE"
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_REMOVE.getPriority();
        }

        /**
         * Should a player perhaps be notified of this removal?
         * They should if they can see the tile, and there is no
         * other-player settlement present.
         *
         * @param serverPlayer The {@code ServerPlayer} to notify.
         * @return True if the player should be notified.
         */
        @Override
        public boolean isPerhapsNotifiable(ServerPlayer serverPlayer) {
            Settlement settlement;
            return tile != null
                && serverPlayer.canSee(tile)
                && ((settlement = tile.getSettlement()) == null
                    || settlement.isDisposed()
                    || serverPlayer.owns(settlement));
        }

        /**
         * Specialize a RemoveChange to a particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return A "remove" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            Element element = doc.createElement("remove");
            // The main object may be visible, but the contents are
            // only visible if the deeper ownership test succeeds.
            if (fcgo instanceof Ownable && serverPlayer.owns((Ownable)fcgo)) {
                for (FreeColGameObject o : contents) {
                    element.appendChild(DOMUtils.toXMLElementPartial(o, doc));
                }
                element.setAttribute("divert", (tile != null) ? tile.getId()
                                     : serverPlayer.getId());
            }
            element.appendChild(DOMUtils.toXMLElementPartial(fcgo, doc));
            return element;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(((tile == null) ? "<null>" : tile.getId()));
            for (FreeColGameObject f : contents) {
                sb.append(' ').append(f.getId());
            }
            sb.append(' ').append(fcgo.getId()).append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a feature change.
     */
    private static class FeatureChange extends Change {

        private final FreeColGameObject parent;
        private final FreeColObject child;
        private final boolean add;

        /**
         * Build a new FeatureChange.
         *
         * @param see The visibility of this change.
         * @param parent The {@code FreeColGameObject} to update.
         * @param child The {@code FreeColObject} value to add or remove.
         * @param add If true, add the child, if not, remove it.
         */
        public FeatureChange(See see, FreeColGameObject parent,
                             FreeColObject child, boolean add) {
            super(see);
            this.parent = parent;
            this.child = child;
            this.add = add;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_OWNED"
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_OWNED.getPriority();
        }

        /**
         * Specialize a feature change into an element for a
         * particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return A "featureChange" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            return new DOMMessage("featureChange",
                                  FreeColObject.ID_ATTRIBUTE_TAG, this.parent.getId(),
                                  "add", Boolean.toString(this.add))
                .add(child)
                .attachToDocument(doc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append((this.add) ? "add" : "remove")
                .append(' ').append(this.child)
                .append(' ').append((this.add) ? "to" : "from")
                .append(' ').append(this.parent.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulates a spying action.
     */
    private static class SpyChange extends Change {
        private final Unit unit;
        private final Settlement settlement;

        /**
         * Build a new SpyChange.
         *
         * @param see The visibility of this change.
         * @param unit The {@code Unit} that is spying.
         * @param settlement The {@code Settlement} to spy on.
         */
        public SpyChange(See see, Unit unit, Settlement settlement) {
            super(see);
            this.unit = unit;
            this.settlement = settlement;
        }

        /**
         * Gets the sort priority.
         *
         * @return priority.
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_NORMAL.getPriority();
        }

        /**
         * Specialize a SpyChange into an element with the supplied name.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return An element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            return new SpySettlementMessage(unit, settlement)
                .attachToDocument(doc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(settlement.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate a stance change.
     */
    private static class StanceChange extends Change {
        private final Player first;
        private final Stance stance;
        private final Player second;

        /**
         * Build a new StanceChange.
         *
         * @param see The visibility of this change.
         * @param first The {@code Player} changing stance.
         * @param stance The {@code Stance} to change to.
         * @param second The {@code Player} wrt with to change.
         */
        public StanceChange(See see, Player first, Stance stance,
                            Player second) {
            super(see);
            this.first = first;
            this.stance = stance;
            this.second = second;
        }

        /**
         * Gets the sort priority.
         *
         * @return "CHANGE_STANCE"
         */
        @Override
        public int getPriority() {
            return ChangePriority.CHANGE_STANCE.getPriority();
        }

        /**
         * Specialize a StanceChange to a particular player.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return A "setStance" element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            return new SetStanceMessage(stance, first, second)
                .attachToDocument(doc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(first.getId())
                .append(' ').append(stance)
                .append(' ').append(second.getId())
                .append(']');
            return sb.toString();
        }
    }

    /**
     * Encapsulate trivial element, which will only have attributes apart
     * from its name.
     */
    private static class TrivialChange extends Change {
        private final int priority;
        private final String name;
        private final String[] attributes;

        /**
         * Build a new TrivialChange.
         *
         * @param see The visibility of this change.
         * @param name The name of the element.
         * @param priority The sort priority of this change.
         * @param attributes The attributes to add to the change.
         */
        public TrivialChange(See see, String name, int priority,
                             String[] attributes) {
            super(see);
            if ((attributes.length & 1) == 1) {
                throw new IllegalArgumentException("Attributes must be even sized");
            }
            this.name = name;
            this.priority = priority;
            this.attributes = attributes;
        }

        /**
         * Gets the sort priority.
         *
         * @return priority.
         */
        @Override
        public int getPriority() {
            return priority;
        }

        /**
         * Specialize a TrivialChange into an element with the supplied name.
         *
         * @param serverPlayer The {@code ServerPlayer} to update.
         * @param doc The owner {@code Document}.
         * @return An element.
         */
        @Override
        public Element toElement(ServerPlayer serverPlayer, Document doc) {
            DOMMessage ret = new AttributeMessage(this.name);
            ret.setStringAttributes(this.attributes);
            return ret.attachToDocument(doc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachToElement(Element element) {} // Noop


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append('[').append(getClass().getName())
                .append(' ').append(see)
                .append(" #").append(getPriority())
                .append(' ').append(name);
            for (String a : attributes) sb.append(' ').append(a);
            sb.append(']');
            return sb.toString();
        }
    }

    /**
     * Simple constructor.
     */
    public ChangeSet() {
        changes = new ArrayList<>();
    }

    /**
     * Copying constructor.
     *
     * @param other The other {@code ChangeSet} to copy.
     */
    public ChangeSet(ChangeSet other) {
        changes = new ArrayList<>(other.changes);
    }


    // Helper routines that should be used to construct a change set.

    /**
     * Sometimes we need to backtrack on making a change.
     *
     * @param fcgo A {@code FreeColGameObject} to remove a matching
     *     change for.
     */
    public void remove(FreeColGameObject fcgo) {
        removeInPlace(changes, c -> c.matches(fcgo));
    }

    /**
     * Helper function to add updates for multiple objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param objects The {@code FreeColGameObject}s that changed.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet add(See see, FreeColGameObject... objects) {
        for (FreeColGameObject o : objects) {
            changes.add(new ObjectChange(see, o));
        }
        return this;
    }

    /**
     * Helper function to add updates for multiple objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param objects The {@code FreeColGameObject}s that changed.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet add(See see, Collection<? extends FreeColGameObject> objects) {
        for (FreeColGameObject o : objects) {
            changes.add(new ObjectChange(see, o));
        }
        return this;
    }

    /**
     * Helper function to add a Message to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param cp The priority of this change.
     * @param message The {@code Message} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet add(See see, ChangePriority cp, DOMMessage message) {
        changes.add(new MessageChange(see, cp, message));
        return this;
    }

    /**
     * Helper function to add an attack to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param attacker The {@code Unit} that is attacking.
     * @param defender The {@code Unit} that is defending.
     * @param success Did the attack succeed?
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addAttack(See see, Unit attacker, Unit defender,
                               boolean success) {
        changes.add(new AttackChange(see, attacker, defender, success));
        return this;
    }

    /**
     * Helper function to add an attribute setting to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param key A key {@code String}.
     * @param value The corresponding value as a {@code String}.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addAttribute(See see, String key, String value) {
        changes.add(new AttributeChange(see, key, value));
        return this;
    }

    /**
     * Helper function to add a dead player event to a ChangeSet.
     * Deaths are public knowledge.
     *
     * @param serverPlayer The {@code ServerPlayer} that died.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addDead(ServerPlayer serverPlayer) {
        add(See.all(), ChangePriority.CHANGE_EARLY,
            new SetDeadMessage(serverPlayer));
        return this;
    }

    /**
     * Helper function to add a removal for an object that disappears
     * (that is, moves where it can not be seen) to a ChangeSet.
     *
     * @param owner The {@code ServerPlayer} that owns this object.
     * @param tile The {@code Tile} where the object was.
     * @param fcgo The {@code FreeColGameObject} that disappears.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addDisappear(ServerPlayer owner, Tile tile,
                                  FreeColGameObject fcgo) {
        List<FreeColGameObject> fl = new ArrayList<>();
        fl.add(fcgo);
        changes.add(new RemoveChange(See.perhaps().except(owner), tile,
                                     fl));
        changes.add(new ObjectChange(See.perhaps().except(owner), tile));
        return this;
    }

    /**
     * Helper function to add a founding father addition event to a ChangeSet.
     * Also adds the father to the owner.
     *
     * @param serverPlayer The {@code ServerPlayer} adding the father.
     * @param father The {@code FoundingFather} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addFather(ServerPlayer serverPlayer,
                               FoundingFather father) {
        changes.add(new FeatureChange(See.only(serverPlayer), serverPlayer,
                                      father, true));
        serverPlayer.addFather(father);
        return this;
    }

    /**
     * Helper function to add or remove an Ability to a FreeColGameObject.
     *
     * @param serverPlayer The owning {@code ServerPlayer}.
     * @param object The {@code FreeColGameObject} to add to.
     * @param ability The {@code Ability} to add/remove.
     * @param add If true, add the ability.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addAbility(ServerPlayer serverPlayer,
                                FreeColGameObject object, Ability ability,
                                boolean add) {
        changes.add(new FeatureChange(See.only(serverPlayer),
                                      object, ability, add));
        if (add) {
            object.addAbility(ability);
        } else {
            object.removeAbility(ability);
        }
        return this;
    }

    /**
     * Helper function to add or remove a Modifier to a FreeColGameObject.
     *
     * @param serverPlayer The owning {@code ServerPlayer}.
     * @param object The {@code FreeColGameObject} to add to.
     * @param modifier The {@code Modifier} to add/remove.
     * @param add If true, add the modifier.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addModifier(ServerPlayer serverPlayer,
                                 FreeColGameObject object, Modifier modifier,
                                 boolean add) {
        changes.add(new FeatureChange(See.only(serverPlayer),
                                      object, modifier, add));
        if (add) {
            object.addModifier(modifier);
        } else {
            object.removeModifier(modifier);
        }
        return this;
    }

    /**
     * Helper function to add a global history event to a ChangeSet.
     * Also adds the history to all the European players.
     *
     * @param game The {@code Game} to find players in.
     * @param history The {@code HistoryEvent} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addGlobalHistory(Game game, HistoryEvent history) {
        for (Player p : game.getLiveEuropeanPlayers()) {
            addHistory((ServerPlayer)p, history);
        }
        return this;
    }

    /**
     * Helper function to add a message to all the European players.
     *
     * @param game The {@code Game} to find players in.
     * @param omit An optional {@code ServerPlayer} to omit.
     * @param message The {@code ModelMessage} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addGlobalMessage(Game game, ServerPlayer omit,
                                      ModelMessage message) {
        for (Player p : game.getLiveEuropeanPlayers()) {
            if (p == (Player)omit) continue;
            addMessage((ServerPlayer)p, message);
        }
        return this;
    }

    /**
     * Helper function to add a history event to a ChangeSet.
     * Also adds the history to the owner.
     *
     * @param serverPlayer The {@code ServerPlayer} making history.
     * @param history The {@code HistoryEvent} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addHistory(ServerPlayer serverPlayer,
                                HistoryEvent history) {
        changes.add(new FeatureChange(See.only(serverPlayer), serverPlayer,
                                      history, true));
        serverPlayer.addHistory(history);
        return this;
    }

    /**
     * Helper function to add a message to a ChangeSet.
     *
     * @param player The {@code Player} to send the message to.
     * @param message The {@code ModelMessage} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addMessage(Player player, ModelMessage message) {
        ServerPlayer serverPlayer = (ServerPlayer)player;
        changes.add(new FeatureChange(See.only(serverPlayer), serverPlayer,
                                      message, true));
        return this;
    }

    /**
     * Helper function to add a move to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param unit The {@code Unit} that is moving.
     * @param loc The location from which the unit is moving.
     * @param tile The {@code Tile} to which the unit is moving.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addMove(See see, Unit unit, Location loc, Tile tile) {
        changes.add(new MoveChange(see, unit, loc, tile));
        return this;
    }

    /**
     * Helper function to add a partial update change for an object to
     * a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param fcgo The {@code FreeColGameObject} to update.
     * @param fields The fields to update.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addPartial(See see, FreeColGameObject fcgo,
                                String... fields) {
        changes.add(new PartialObjectChange(see, fcgo, fields));
        return this;
    }

    /**
     * Helper function to add a new player to a ChangeSet.
     *
     * @param serverPlayer The new {@code ServerPlayer} to add.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addPlayer(ServerPlayer serverPlayer) {
        changes.add(new PlayerChange(See.all().except(serverPlayer),
                                     serverPlayer));
        return this;
    }

    /**
     * Helper function to add a removal to a ChangeSet.
     *
     * -vis: If disposing of units or colonies, this routine changes
     * player visibility.
     *
     * @param see The visibility of this change.
     * @param loc The {@code Location} where the object was.
     * @param obj The {@code FreeColGameObject} to remove.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addRemove(See see, Location loc, FreeColGameObject obj) {
        List<FreeColGameObject> dl = new ArrayList<>();
        obj.getDisposables(dl);
        changes.add(new RemoveChange(see, loc, dl));//-vis
        return this;
    }

    /**
     * Helper function to add removals for several objects to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param loc The {@code Location} where the object was.
     * @param objects A list of {@code FreeColGameObject}s to remove.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addRemoves(See see, Location loc,
                                List<? extends FreeColGameObject> objects) {
        for (FreeColGameObject fcgo : objects) {
            List<FreeColGameObject> dl = new ArrayList<>();
            fcgo.getDisposables(dl);
            changes.add(new RemoveChange(see, loc, dl));
        }
        return this;
    }

    /**
     * Helper function to add a sale change to a ChangeSet.
     *
     * @param serverPlayer The {@code ServerPlayer} making the sale.
     * @param settlement The {@code Settlement} that is buying.
     * @param type The {@code GoodsType} bought.
     * @param price The per unit price.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addSale(ServerPlayer serverPlayer, Settlement settlement,
                             GoodsType type, int price) {
        Game game = settlement.getGame();
        LastSale sale = new LastSale(settlement, type, game.getTurn(), price);
        changes.add(new FeatureChange(See.only(serverPlayer), serverPlayer,
                                      sale, true));
        serverPlayer.addLastSale(sale);
        return this;
    }

    /**
     * Helper function to add a spying change to a ChangeSet.
     *
     * @param unit The {@code Unit} that is spying.
     * @param settlement The {@code Settlement} to spy on.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addSpy(Unit unit, Settlement settlement) {
        changes.add(new SpyChange(See.only((ServerPlayer)unit.getOwner()),
                                  unit, settlement));
        return this;
    }

    /**
     * Helper function to add a stance change to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param first The {@code Player} changing stance.
     * @param stance The {@code Stance} to change to.
     * @param second The {@code Player} wrt with to change.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addStance(See see, Player first, Stance stance,
                               Player second) {
        changes.add(new StanceChange(see, first, stance, second));
        return this;
    }

    /**
     * Helper function to add a new trade route change to a ChangeSet.
     * Also adds the trade route to the player.
     *
     * @param serverPlayer The {@code ServerPlayer} adding the route.
     * @param tradeRoute The new {@code TradeRoute}.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addTradeRoute(ServerPlayer serverPlayer,
                                   TradeRoute tradeRoute) {
        changes.add(new FeatureChange(See.only(serverPlayer), serverPlayer,
                                      tradeRoute, true));
        return this;
    }

    /**
     * Helper function to add a trivial element to a ChangeSet.
     *
     * @param see The visibility of this change.
     * @param name The name of the element.
     * @param cp The {@code ChangePriority} for this change.
     * @param attributes Attributes to add to this trivial change.
     * @return The updated {@code ChangeSet}.
     */
    public ChangeSet addTrivial(See see, String name, ChangePriority cp,
                                String... attributes) {
        changes.add(new TrivialChange(see, name, cp.getPriority(), attributes));
        return this;
    }

    // Conversion of a change set to a corresponding element.

    /**
     * Collapse one element into another.
     *
     * @param head The {@code Element} to collapse into.
     * @param tail The {@code Element} to extract nodes from.
     */
    private static void collapseElements(Element head, Element tail) {
        while (tail.hasChildNodes()) {
            head.appendChild(tail.removeChild(tail.getFirstChild()));
        }
    }

    /**
     * Can two elements be collapsed?
     * They need to have the same name and attributes.
     *
     * @param e1 The first {@code Element}.
     * @param e2 The second {@code Element}.
     * @return True if they can be collapsed.
     */
    private static boolean collapseOK(Element e1, Element e2) {
        if (!e1.getTagName().equals(e2.getTagName())) return false;
        NamedNodeMap nnm1 = e1.getAttributes();
        NamedNodeMap nnm2 = e2.getAttributes();
        if (nnm1.getLength() != nnm2.getLength()) return false;
        for (int i = 0; i < nnm1.getLength(); i++) {
            if (nnm1.item(i).getNodeType() != nnm2.item(i).getNodeType()) {
                return false;
            }
            if (!nnm1.item(i).getNodeName().equals(nnm2.item(i).getNodeName())) {
                return false;
            }
            if (!nnm1.item(i).getNodeValue().equals(nnm2.item(i).getNodeValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Collapse adjacent elements in a list with the same tag.
     *
     * @param elements The list of {@code Element}s to consider.
     * @return A collapsed list of elements.
     */
    private static List<Element> collapseElementList(List<Element> elements) {
        List<Element> results = new ArrayList<>();
        if (!elements.isEmpty()) {
            Element head = elements.remove(0);
            while (!elements.isEmpty()) {
                Element e = elements.remove(0);
                if (collapseOK(head, e)) {
                    collapseElements(head, e);
                } else {
                    results.add(head);
                    head = e;
                }
            }
            results.add(head);
        }
        return results;
    }

    /**
     * Build a generalized update.
     * Beware that removing an object does not necessarily update
     * its tile correctly on the client side--- if a tile update
     * is needed the tile should be supplied in the objects list.
     *
     * @param serverPlayer The {@code ServerPlayer} to send the
     *            update to.
     * @return An element encapsulating an update of the objects to
     *         consider, or null if there is nothing to report.
     */
    public Element build(ServerPlayer serverPlayer) {
        List<Change> c = sort(changes, changeComparator);
        List<Element> elements = new ArrayList<>();
        List<Change> diverted = new ArrayList<>();
        Document doc = DOMUtils.createNewDocument();

        // For all sorted changes, if it is notifiable to the target
        // player then convert it to an Element, or divert for later
        // attachment.  Then add all consequence changes to the list.
        while (!c.isEmpty()) {
            Change change = c.remove(0);
            if (change.isNotifiable(serverPlayer)) {
                if (change.convertsToElement()) {
                    elements.add(change.toElement(serverPlayer, doc));
                } else {
                    diverted.add(change);
                }
                c.addAll(change.consequences(serverPlayer));
            }
        }
        elements = collapseElementList(elements);

        // Decide what to return.  If there are several parts with
        // children then return multiple, if there is one viable part,
        // return that, if there is none return null unless there are
        // attributes in which case they become viable as an update.
        Element result;
        switch (elements.size()) {
        case 0:
            if (diverted.isEmpty()) return null;
            result = doc.createElement("update");
            break;
        case 1:
            result = elements.get(0);
            break;
        default:
            result = new MultipleMessage(elements).toXMLElement();
            break;
        }
        result = (Element)doc.importNode(result, true);
        for (Change change : diverted) change.attachToElement(result);
        return result;
    }


    // Convenience functions to create change sets

    /**
     * Convenience function to create an i18n client error message and
     * wrap it into a change set.
     *
     * @param serverPlayer An optional {@code ServerPlayer} to restrict
     *     visibility to.
     * @param template An i18n template.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(ServerPlayer serverPlayer,
                                        StringTemplate template) {
        See see = (serverPlayer == null) ? See.all() : See.only(serverPlayer);
        return clientError(see, template);
    }

    /**
     * Convenience function to create an i18n client error message and
     * wrap it into a change set.
     *
     * @param see The message visibility.
     * @param template An i18n template.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(See see, StringTemplate template) {
        ChangeSet cs = new ChangeSet();
        if (see == null) see = See.all();
        cs.add(see, ChangeSet.ChangePriority.CHANGE_NORMAL,
               new ErrorMessage(template));
        return cs;
    }

    /**
     * Convenience function to create a non-i18n client error message
     * and wrap it into a change set.
     *
     * @param serverPlayer An optional {@code ServerPlayer} to restrict
     *     visibility to.
     * @param message The message.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(ServerPlayer serverPlayer,
                                        String message) {
        See see = (serverPlayer == null) ? See.all() : See.only(serverPlayer);
        return clientError(see, message);
    }

    /**
     * Convenience function to create a non-i18n client error message
     * and wrap it into a change set.
     *
     * @param see The message visibility.
     * @param message A non-i18n message.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet clientError(See see, String message) {
        ChangeSet cs = new ChangeSet();
        if (see == null) see = See.all();
        cs.add(see, ChangeSet.ChangePriority.CHANGE_NORMAL,
               new ErrorMessage(message));
        return cs;
    }

    /**
     * Convenience function to create a change set containing a message.
     *
     * @param serverPlayer An optional {@code ServerPlayer} to restrict
     *     visibility to.
     * @param message The message to wrap.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet simpleChange(ServerPlayer serverPlayer,
                                         DOMMessage message) {
        See see = (serverPlayer == null) ? See.all() : See.only(serverPlayer);
        return simpleChange(see, message);
    }

    /**
     * Convenience function to create a change set containing a message.
     *
     * @param see The message visibility.
     * @param message The message to wrap.
     * @return A new {@code ChangeSet}.
     */
    public static ChangeSet simpleChange(See see, DOMMessage message) {
        ChangeSet cs = new ChangeSet();
        cs.add((see == null) ? See.all() : see, ChangePriority.CHANGE_NORMAL,
               message);
        return cs;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Change c : sort(changes, changeComparator)) {
            sb.append(c).append('\n');
        }
        return sb.toString();
    }
}
