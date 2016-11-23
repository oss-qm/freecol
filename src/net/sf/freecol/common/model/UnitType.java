/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.UnitTypeChange.ChangeType;


public final class UnitType extends BuildableType
    implements Comparable<UnitType>, Consumer {

    /**
     * The default offence value.
     */
    public static final int DEFAULT_OFFENCE = 0;

    /**
     * The default offence value.
     */
    public static final int DEFAULT_DEFENCE = 1;

    /**
     * The offence of this UnitType. Only Units with an offence value
     * greater than zero can attack.
     */
    private int offence = DEFAULT_OFFENCE;

    /**
     * The defence of this UnitType.
     */
    private int defence = DEFAULT_DEFENCE;

    /**
     * The capacity of this UnitType.
     */
    private int space = 0;

    /**
     * The number of hit points this UnitType has. At the moment, this
     * is only used for ships. All other UnitTypes are downgraded or
     * destroyed if they lose a battle.
     */
    private int hitPoints = 0;

    /**
     * The space taken by this UnitType.
     */
    private int spaceTaken = 1;

    /**
     * The skill level of this UnitType.
     */
    private int skill = UNDEFINED;

    /**
     * The price of this UnitType.
     */
    private int price = UNDEFINED;

    /**
     * The initial moves of this UnitType.
     */
    private int movement = 3;

    /**
     * The maximum distance of tiles this UnitType can observe.
     */
    private int lineOfSight = 1;

    /**
     * The probability of recruiting a Unit of this type in Europe.
     */
    private int recruitProbability = 0;

    /**
     * The expert production of this UnitType.
     */
    private GoodsType expertProduction = null;

    /**
     * How much a Unit of this type contributes to the Player's score.
     */
    private int scoreValue = 0;

    /**
     * The maximum experience a unit of this type can accumulate.
     */
    private int maximumExperience = 0;

    /**
     * The maximum attrition this UnitType can accumulate without
     * being destroyed.
     */
    private int maximumAttrition = INFINITY;

    /**
     * The skill this UnitType teaches, mostly its own.
     */
    private UnitType skillTaught = null;

    /**
     * The default equipment for a unit of this type.
     */
    private EquipmentType defaultEquipment = null;

    /**
     * The possible type changes for this unit type.
     */
    private List<UnitTypeChange> typeChanges = null;

    /**
     * The goods consumed per turn when in a settlement.
     */
    private TypeCountMap<GoodsType> consumption = null;


    /**
     * Creates a new <code>UnitType</code> instance.
     *
     * @param id The unit type id.
     * @param specification The <code>Specification</code> in which to create
     *     this unit type.
     */
    public UnitType(String id, Specification specification) {
        super(id, specification);

        setModifierIndex(Modifier.EXPERT_PRODUCTION_INDEX);
    }

    /**
     * Get a key for the working as this unit type message.
     *
     * @return A message key.
     */
    public final String getWorkingAsKey() {
        return getId() + ".workingAs";
    }

    /**
     * Can this unit type carry units?
     *
     * @return True if units can be carried.
     */
    public boolean canCarryUnits() {
        return hasAbility(Ability.CARRY_UNITS);
    }

    /**
     * Can this unit type carry goods?
     *
     * @return True if goods can be carried.
     */
    public boolean canCarryGoods() {
        return hasAbility(Ability.CARRY_GOODS);
    }

    /**
     * Gets the score for acquiring a unit of this type.
     *
     * @return The score for this unit type.
     */
    public int getScoreValue() {
        return scoreValue;
    }

    /**
     * Get the offence value.
     *
     * @return The offence value.
     */
    public int getOffence() {
        return offence;
    }

    /**
     * Is this an offensive unit type?
     *
     * @return True if offensive ability is greater than the default.
     */
    public boolean isOffensive() {
        return getOffence() > UnitType.DEFAULT_OFFENCE;
    }

    /**
     * Get the defence value.
     *
     * @return The defence value.
     */
    public int getDefence() {
        return defence;
    }

    /**
     * Is this a defensive unit type?
     *
     * @return True if defensive ability is greater than the default.
     */
    public boolean isDefensive() {
        return getDefence() > UnitType.DEFAULT_DEFENCE;
    }

    /**
     * Get the `line of sight' distance (in tiles).
     *
     * @return The line of sight distance.
     */
    public int getLineOfSight() {
        return lineOfSight;
    }

    /**
     * Get the space this unit type has to carry cargo.
     *
     * @return The cargo capacity of this unit type.
     */
    public int getSpace() {
        return space;
    }

    /**
     * Set the space this unit type has to carry cargo.
     * Required by the test suite.
     *
     * @param newSpace The new cargo capacity.
     */
    public void setSpace(final int newSpace) {
        this.space = newSpace;
    }

    /**
     * Get the unit type hit points.
     *
     * @return The hit points.
     */
    public int getHitPoints() {
        return hitPoints;
    }

    /**
     * Gets the number of cargo slots a unit of this type takes on a carrier.
     *
     * @return The number of cargo slots.
     */
    public int getSpaceTaken() {
        return Math.max(spaceTaken, space + 1);
    }

    /**
     * Set the number of cargo slots a unit of this type takes on a carrier.
     * Required by the test suite.
     *
     * @param newSpaceTaken The new number of cargo slots.
     */
    public void setSpaceTaken(final int newSpaceTaken) {
        this.spaceTaken = newSpaceTaken;
    }

    /**
     * Is this UnitType recruitable in Europe?
     *
     * @return True if European-recruitable.
     */
    public boolean isRecruitable() {
        return recruitProbability > 0;
    }

    /**
     * Get the relative probability of recruiting this unit in Europe.
     *
     * @return A relative probability.
     */
    public int getRecruitProbability() {
        return recruitProbability;
    }

    /**
     * Get the skill level associated with this unit type.
     *
     * @return The skill level.
     */
    public int getSkill() {
        return skill;
    }

    /**
     * Set the skill level associated with this unit type.
     * Required by the test suite.
     *
     * @param newSkill The new skill level.
     */
    public void setSkill(final int newSkill) {
        this.skill = newSkill;
    }

    /**
     * Get the base price of this unit type.
     * For the actual price of the unit, use
     * {@link Europe#getUnitPrice(UnitType)}
     *
     * @return The base price.
     */
    public int getPrice() {
        return price;
    }

    /**
     * Get the base movement of this unit type.
     *
     * @return The base movement.
     */
    public int getMovement() {
        return movement;
    }

    /**
     * Get the maximum experience required a unit of this type may achieve.
     *
     * @return The maximum experience.
     */
    public final int getMaximumExperience() {
        return maximumExperience;
    }

    /**
     * Get the maximum attrition for this unit type (greater attrition than
     * this destroys the unit).
     *
     * @return The maximum attrition.
     */
    public int getMaximumAttrition() {
        return maximumAttrition;
    }

    /**
     * Get the type of goods this unit type has expert ability to produce.
     *
     * @return The expert production <code>GoodsType</code>.
     */
    public GoodsType getExpertProduction() {
        return expertProduction;
    }

    /**
     * Get the skill taught by this unit type.
     *
     * @return The skill taught by this unit type.
     */
    public UnitType getSkillTaught() {
        return skillTaught;
    }

    /**
     * Gets the default equipment type this unit type is equipped with.
     *
     * @return The default <code>EquipmentType</code>.
     */
    public EquipmentType getDefaultEquipmentType() {
        return defaultEquipment;
    }

    /**
     * Gets the default equipment to equip a unit of this type with.
     *
     * @return The default equipment.
     */
    public EquipmentType[] getDefaultEquipment() {
        if (hasAbility(Ability.CAN_BE_EQUIPPED) && defaultEquipment != null) {
            int count = defaultEquipment.getMaximumCount();
            EquipmentType[] result = new EquipmentType[count];
            for (int index = 0; index < count; index++) {
                result[index] = defaultEquipment;
            }
            return result;
        } else {
            return EquipmentType.NO_EQUIPMENT;
        }
    }

    /**
     * Gets the list of all type changes associated with this unit type.
     *
     * @return The list of type changes.
     */
    public List<UnitTypeChange> getTypeChanges() {
        return (typeChanges == null) ? new ArrayList<UnitTypeChange>()
            : typeChanges;
    }

    /**
     * Sets the list of all type changes associated with this unit type.
     * Public for the test suite.
     *
     * @param typeChanges The new list of type changes.
     */
    public void setTypeChanges(List<UnitTypeChange> typeChanges) {
        this.typeChanges = typeChanges;
    }

    /**
     * Gets a unit type resulting from a given change type and player.
     *
     * @param changeType A <code>ChangeType</code> to match.
     * @param player A <code>Player</code> to check.
     * @return Any changed <code>UnitType</code> found, or null if none.
     */
    public UnitType getTargetType(ChangeType changeType, Player player) {
        UnitTypeChange change = getUnitTypeChange(changeType, player);
        return (change == null) ? null : change.getNewUnitType();
    }

    /**
     * Gets any suitable unit type changes applicable to a given change type
     * and player.
     *
     * @param changeType The <code>ChangeType</code> to match.
     * @param player The <code>Player</code> to check.
     * @return Any <code>UnitTypeChange</code> found, or null if none.
     */
    public UnitTypeChange getUnitTypeChange(ChangeType changeType, Player player) {
        for (UnitTypeChange change : getTypeChanges()) {
            if (change.asResultOf(changeType) && change.appliesTo(player)) {
                UnitType result = change.getNewUnitType();
                if (result.isAvailableTo(player)) {
                    return change;
                }
            }
        }
        return null;
    }

    /**
     * Gets the type change required to become another given unit type.
     *
     * @param newType The target <code>UnitType</code>.
     * @return The type change, or null if impossible.
     */
    public UnitTypeChange getUnitTypeChange(UnitType newType) {
        for (UnitTypeChange change : getTypeChanges()) {
            if (change.getNewUnitType() == newType) {
                return change;
            }
        }
        return null;
    }

    /**
     * Can this type of unit be upgraded to another given type by a given
     * educational change type?
     *
     * If the target type is null, return true if the UnitType can be
     * upgraded to any other type by the given means of education.
     *
     * @param newType The <code>UnitType</code> to learn (may be null
     *     in the case of attempting to move to a native settlement
     *     when the skill taught there is still unknown).
     * @param changeType The educational <code>ChangeType</code>.
     * @return True if this unit type can learn.
     */
    public boolean canBeUpgraded(UnitType newType, ChangeType changeType) {
        for (UnitTypeChange change : getTypeChanges()) {
            if ((newType == null || newType == change.getNewUnitType())
                && change.getProbability(changeType) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the unit types which can be learned from a lost city rumour.
     *
     * @return A list of unit types.
     */
    public List<UnitType> getUnitTypesLearntInLostCity() {
        List<UnitType> unitTypes = new ArrayList<UnitType>();
        for (UnitTypeChange change : getTypeChanges()) {
            if (change.asResultOf(ChangeType.LOST_CITY)) {
                unitTypes.add(change.getNewUnitType());
            }
        }
        return unitTypes;
    }

    /**
     * Get a UnitType to learn with a level skill less or equal than
     * given level.
     *
     * @param maximumSkill The maximum level skill which we are searching for.
     * @return A <code>UnitType</code> with a skill equal or less than given
     *     maximum.
     */
    public UnitType getEducationUnit(int maximumSkill) {
        for (UnitTypeChange change : getTypeChanges()) {
            if (change.canBeTaught()) {
                UnitType unitType = change.getNewUnitType();
                if (unitType.hasSkill() && unitType.getSkill() <= maximumSkill) {
                    return unitType;
                }
            }
        }
        return null;
    }

    /**
     * Get the number of turns to educate this unit type to become
     * another type.
     *
     * @param unitType The <code>UnitType</code> to teach.
     * @return The number of turns, or UNDEFINED if impossible.
     */
    public int getEducationTurns(UnitType unitType) {
        for (UnitTypeChange change : getTypeChanges()) {
            if (change.asResultOf(UnitTypeChange.ChangeType.EDUCATION)) {
                if (unitType == change.getNewUnitType()) {
                    return change.getTurnsToLearn();
                }
            }
        }
        return UNDEFINED;
    }

    /**
     * Is this a naval unit type?
     *
     * @return True if this is a naval unit type.
     */
    public boolean isNaval() {
        return hasAbility(Ability.NAVAL_UNIT);
    }

    /**
     * Can this unit type move to the High Seas?
     *
     * ATM this is synonymous with being a naval unit, but we should use
     * this routine instead of isNaval() in case this changes.
     *
     * @return True if units of this type can move to the High Seas.
     */
    public boolean canMoveToHighSeas() {
        return isNaval();
    }

    /**
     * Does this UnitType have a skill?
     *
     * @return True if this unit type has a skill.
     */
    public boolean hasSkill() {
        return skill != UNDEFINED;
    }

    /**
     * Does this UnitType have a price?
     *
     * @return True if the unit type has a price.
     */
    public boolean hasPrice() {
        return price != UNDEFINED;
    }

    /**
     * Gets the number of units of the given GoodsType this UnitType
     * consumes per turn (when in a settlement).
     *
     * @return The amount of goods consumed per turn.
     */
    public int getConsumptionOf(GoodsType goodsType) {
        return (consumption == null) ? 0 : consumption.getCount(goodsType);
    }

    // Interface Comparable

    /**
     * {@inheritDoc}
     */
    public int compareTo(UnitType other) {
        return getIndex() - other.getIndex();
    }

    // Interface Consumer

    /**
     * Gets a list of goods this Consumer consumes.
     *
     * @return The goods consumed by this unit type.
     */
    public List<AbstractGoods> getConsumedGoods() {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>();
        if (consumption != null) {
            for (GoodsType goodsType : consumption.keySet()) {
                result.add(new AbstractGoods(goodsType,
                        consumption.getCount(goodsType)));
            }
        }
        return result;
    }

    /**
     * Gets the priority of this Consumer.  The higher the priority,
     * the earlier will the Consumer be allowed to consume the goods
     * it requires.
     *
     * @return The priority of this unit type.
     */
    public int getPriority() {
        // TODO: make this configurable
        return UNIT_PRIORITY;
    }

    /**
     * Is this unit type able to build a colony?
     *
     * @return True if this unit type can build colonies.
     */
    public boolean canBuildColony() {
        return hasAbility(Ability.FOUND_COLONY);
    }


    // Serialization

    private static final String CONSUMES_TAG = "consumes";
    private static final String DEFAULT_EQUIPMENT_TAG = "default-equipment";

    private static final String DEFENCE_TAG = "defence";
    private static final String EXPERT_PRODUCTION_TAG = "expert-production";
    private static final String HIT_POINTS_TAG = "hitPoints";
    private static final String LINE_OF_SIGHT_TAG = "lineOfSight";
    private static final String MOVEMENT_TAG = "movement";
    private static final String MAXIMUM_EXPERIENCE_TAG = "maximumExperience";
    private static final String MAXIMUM_ATTRITION_TAG = "maximumAttrition";
    private static final String OFFENCE_TAG = "offence";
    private static final String PRICE_TAG = "price";
    private static final String RECRUIT_PROBABILITY_TAG = "recruitProbability";
    private static final String SCORE_VALUE_TAG = "scoreValue";
    private static final String SKILL_TAG = "skill";
    private static final String SKILL_TAUGHT_TAG = "skillTaught";
    private static final String SPACE_TAG = "space";
    private static final String SPACE_TAKEN_TAG = "spaceTaken";

    private static final String DOWNGRADE_TAG = "downgrade";
    private static final String UNIT_TAG = "unit";
    private static final String UPGRADE_TAG = "upgrade";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, OFFENCE_TAG, offence);

        writeAttribute(out, DEFENCE_TAG, defence);

        writeAttribute(out, MOVEMENT_TAG, movement);

        writeAttribute(out, LINE_OF_SIGHT_TAG, lineOfSight);

        writeAttribute(out, SCORE_VALUE_TAG, scoreValue);

        writeAttribute(out, SPACE_TAG, space);

        writeAttribute(out, SPACE_TAKEN_TAG, spaceTaken);

        writeAttribute(out, HIT_POINTS_TAG, hitPoints);

        writeAttribute(out, MAXIMUM_EXPERIENCE_TAG, maximumExperience);

        if (maximumAttrition < INFINITY) {
            writeAttribute(out, MAXIMUM_ATTRITION_TAG, maximumAttrition);
        }

        writeAttribute(out, RECRUIT_PROBABILITY_TAG, recruitProbability);

        if (hasSkill()) {
            writeAttribute(out, SKILL_TAG, skill);
        }

        if (hasPrice()) {
            writeAttribute(out, PRICE_TAG, price);
        }

        writeAttribute(out, SKILL_TAUGHT_TAG, skillTaught);

        if (expertProduction != null) {
            writeAttribute(out, EXPERT_PRODUCTION_TAG, expertProduction);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (defaultEquipment != null) {
            out.writeStartElement(DEFAULT_EQUIPMENT_TAG);

            writeAttribute(out, ID_ATTRIBUTE_TAG, defaultEquipment);

            out.writeEndElement();
        }

        if (typeChanges != null) {
            for (UnitTypeChange change : typeChanges) {
                change.toXMLImpl(out);
            }
        }

        if (consumption != null) {
            for (GoodsType goodsType : consumption.keySet()) {
                out.writeStartElement(CONSUMES_TAG);

                writeAttribute(out, ID_ATTRIBUTE_TAG, goodsType);

                writeAttribute(out, VALUE_TAG, consumption.getCount(goodsType));

                out.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        final Specification spec = getSpecification();

        UnitType parent = spec.getType(in, EXTENDS_TAG, UnitType.class, this);

        offence = getAttribute(in, OFFENCE_TAG, parent.offence);

        defence = getAttribute(in, DEFENCE_TAG, parent.defence);

        movement = getAttribute(in, MOVEMENT_TAG, parent.movement);

        lineOfSight = getAttribute(in, LINE_OF_SIGHT_TAG, parent.lineOfSight);

        scoreValue = getAttribute(in, SCORE_VALUE_TAG, parent.scoreValue);

        space = getAttribute(in, SPACE_TAG, parent.space);

        hitPoints = getAttribute(in, HIT_POINTS_TAG, parent.hitPoints);

        spaceTaken = getAttribute(in, SPACE_TAKEN_TAG, parent.spaceTaken);

        maximumExperience = getAttribute(in, MAXIMUM_EXPERIENCE_TAG,
                                         parent.maximumExperience);

        maximumAttrition = getAttribute(in, MAXIMUM_ATTRITION_TAG,
                                        parent.maximumAttrition);

        skillTaught = spec.getType(in, SKILL_TAUGHT_TAG,
                                   UnitType.class, this);

        recruitProbability = getAttribute(in, RECRUIT_PROBABILITY_TAG,
                                          parent.recruitProbability);

        skill = getAttribute(in, SKILL_TAG, parent.skill);

        price = getAttribute(in, PRICE_TAG, parent.price);

        expertProduction = spec.getType(in, EXPERT_PRODUCTION_TAG,
                                        GoodsType.class, parent.expertProduction);

        if (parent != this) { // Handle "extends" for super-type fields
            if (!hasAttribute(in, REQUIRED_POPULATION_TAG)) {
                setRequiredPopulation(parent.getRequiredPopulation());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            consumption = null;
            defaultEquipment = null;
            typeChanges = null;
        }

        final Specification spec = getSpecification();
        UnitType parent = spec.getType(in, EXTENDS_TAG, UnitType.class, this);
        if (parent != this) {
            defaultEquipment = parent.defaultEquipment;

            if (parent.typeChanges != null) {
                if (typeChanges == null) {
                    typeChanges = new ArrayList<UnitTypeChange>();
                }
                typeChanges.addAll(parent.typeChanges);
            }

            if (parent.consumption != null) {
                if (consumption == null) {
                    consumption = new TypeCountMap<GoodsType>();
                }
                consumption.putAll(parent.consumption);
            }

            addFeatures(parent);
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }

        super.readChildren(in);

        // @compat 0.10.6
        if (hasAbility("model.ability.person")) {
            if (!containsModifierKey("model.modifier.conversionSkill")) {
                addModifier(new Modifier("model.modifier.conversionSkill", 8.0f,
                        Modifier.Type.ADDITIVE));
                if (hasAbility("model.ability.expertMissionary")) {
                    addModifier(new Modifier("model.modifier.conversionSkill", 5.0f,
                            Modifier.Type.ADDITIVE));
                }
            }
            if (!containsModifierKey("model.modifier.conversionAlarmRate")) {
                addModifier(new Modifier("model.modifier.conversionAlarmRate", 2.0f,
                        Modifier.Type.PERCENTAGE));
            }
        }
        // end compatibility code
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (CONSUMES_TAG.equals(tag)) {
            GoodsType type = spec.getType(in, ID_ATTRIBUTE_TAG,
                                          GoodsType.class, (GoodsType)null);
            int amount = getAttribute(in, VALUE_TAG, UNDEFINED);
            if (type != null && amount != UNDEFINED) {
                if (consumption == null) {
                    consumption = new TypeCountMap<GoodsType>();
                }
                consumption.incrementCount(type, amount);
            }
            in.nextTag(); // close this element

        } else if (DEFAULT_EQUIPMENT_TAG.equals(tag)) {
            defaultEquipment = spec.getType(in, ID_ATTRIBUTE_TAG,
                EquipmentType.class, (EquipmentType)null);
            in.nextTag(); // close this element

        } else if (DOWNGRADE_TAG.equals(tag) || UPGRADE_TAG.equals(tag)) {
            if (getAttribute(in, DELETE_TAG, false)) {
                if (typeChanges != null) {
                    String unitId = getAttribute(in, UNIT_TAG, (String)null);
                    Iterator<UnitTypeChange> it = typeChanges.iterator();
                    while (it.hasNext()) {
                        if (unitId.equals(it.next().getNewUnitType().getId())) {
                            it.remove();
                            break;
                        }
                    }
                }
                in.nextTag();

            } else {
                UnitTypeChange change = new UnitTypeChange(in, spec);
                if (DOWNGRADE_TAG.equals(tag)
                    && change.getChangeTypes().isEmpty()) {
                    // add default downgrade type
                    change.getChangeTypes().put(ChangeType.CLEAR_SKILL, 100);
                }
                if (typeChanges == null) {
                    typeChanges = new ArrayList<UnitTypeChange>();
                }
                typeChanges.add(change);
            }

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getId();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unit-type".
     */
    public static String getXMLElementTagName() {
        return "unit-type";
    }
}
