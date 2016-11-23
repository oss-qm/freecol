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

package net.sf.freecol.common.option;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Specification;


/**
 * The super class of all options. GUI components making use of this
 * class can refer to its name and shortDescription properties.  The
 * complete keys of these properties consist of the id of the option
 * group (if any), followed by a "."  unless the option group is null,
 * followed by the id of the option object, followed by a ".",
 * followed by "name" or "shortDescription".
 */
abstract public class AbstractOption<T> extends FreeColObject
    implements Option<T> {

    private static Logger logger = Logger.getLogger(AbstractOption.class.getName());

    /** The option group prefix. */
    private String optionGroup = "";

    /**
     * Determine if the option has been defined.  When defined an
     * option won't change when a default value is read from an XML file.
     */
    protected boolean isDefined = false;


    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public AbstractOption(String id) {
        this(id, null);
    }

    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param specification The <code>Specification</code> this
     *     <code>Option</code> refers to.  This may be null, since only
     *     some options need access to the specification.
     */
    public AbstractOption(Specification specification) {
        this(null, specification);
    }

    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param id The identifier for this option.  This is used when the
     *     object should be found in an {@link OptionGroup}.
     * @param specification The <code>Specification</code> this
     *     <code>Option</code> refers to.  This may be null, since only
     *     some options need access to the specification.
     */
    public AbstractOption(String id, Specification specification) {
        setId(id);
        setSpecification(specification);
    }

    public abstract AbstractOption<T> clone() throws CloneNotSupportedException;

    /**
     * Sets the values from another option.
     *
     * @param source The other <code>AbstractOption</code>.
     */
    protected void setValues(AbstractOption<T> source) {
        setId(source.getId());
        setSpecification(source.getSpecification());
        setValue(source.getValue());
        setGroup(source.getGroup());
        isDefined = source.isDefined;
    }

    /**
     * Gets the string prefix that identifies the group of this
     * <code>Option</code>.
     *
     * @return The string prefix provided by the OptionGroup.
     */
    public String getGroup() {
        return optionGroup;
    }

    /**
     * Set the option group prefix.
     *
     * @param group The prefix to set.
     */
    public void setGroup(String group) {
        if (group == null) {
            optionGroup = "";
        } else {
            optionGroup = group;
        }
    }

    /**
     * Gets the value of this Option.
     *
     * @return The value of this Option
     */
    public abstract T getValue();

    /**
     * Sets the value of this Option.
     *
     * @param value The new value of this Option.
     */
    public abstract void setValue(T value);

    /**
     * Sets the value of this Option from the given string
     * representation.  Both parameters must not be null at the same
     * time.  This method does nothing.  Override it if the Option has
     * a suitable string representation.
     *
     * @param valueString The string representation of the value of
     *     this Option.
     * @param defaultValueString The string representation of the
     *     default value of this Option
     */
    protected void setValue(String valueString, String defaultValueString) {
        logger.warning("Unsupported method: setValue.");
    }

    /**
     * Is null an acceptable value for this Option?
     * Override it where necessary.
     *
     * @return False.
     */
    public boolean isNullValueOK() {
        return false;
    }

    /**
     * Generate the choices to provide to the UI.
     * Override if the Option needs to determine its choices dynamically.
     */
    public void generateChoices() {
        // do nothing
    }


    // Serialization

    private static final String ACTION_TAG = "action";
    private static final String DEFAULT_VALUE_TAG = "defaultValue";

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        readAttributes(in);

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        setId(getAttribute(in, ID_ATTRIBUTE_TAG, getId()));
        
        String defaultValue = getAttribute(in, DEFAULT_VALUE_TAG, (String)null);

        String value = getAttribute(in, VALUE_TAG, (String)null);

        if (!isNullValueOK() && defaultValue == null && value == null) {
            throw new XMLStreamException("invalid option " + getId()
                + ": no value nor default value found.");
        }

        setValue(value, defaultValue);
    }

    /**
     * General option reader routine.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @return An option.
     */
    protected AbstractOption readOption(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();
        AbstractOption option = null;

        if (ACTION_TAG.equals(tag)) {
            // TODO: load FreeColActions from client options?
            logger.finest("Skipping action "
                + getAttribute(in, ID_ATTRIBUTE_TAG, ""));
            in.nextTag();

        } else if (AbstractUnitOption.getXMLElementTagName().equals(tag)) {
            option = new AbstractUnitOption(spec);

        } else if (AudioMixerOption.getXMLElementTagName().equals(tag)) {
            option = new AudioMixerOption(spec);

        } else if (BooleanOption.getXMLElementTagName().equals(tag)) {
            option = new BooleanOption(spec);

        } else if (FileOption.getXMLElementTagName().equals(tag)) {
            option = new FileOption(spec);

        } else if (IntegerOption.getXMLElementTagName().equals(tag)) {
            option = new IntegerOption(spec);

        } else if (LanguageOption.getXMLElementTagName().equals(tag)) {
            option = new LanguageOption(spec);

        } else if (ModListOption.getXMLElementTagName().equals(tag)) {
            option = new ModListOption(spec);

        } else if (ModOption.getXMLElementTagName().equals(tag)) {
            option = new ModOption(spec);

        } else if (OptionGroup.getXMLElementTagName().equals(tag)) {
            option = new OptionGroup(spec);

        } else if (PercentageOption.getXMLElementTagName().equals(tag)) {
            option = new PercentageOption(spec);

        } else if (RangeOption.getXMLElementTagName().equals(tag)) {
            option = new RangeOption(spec);

        } else if (SelectOption.getXMLElementTagName().equals(tag)) {
            option = new SelectOption(spec);

        } else if (StringOption.getXMLElementTagName().equals(tag)) {
            option = new StringOption(spec);

        } else if (UnitListOption.getXMLElementTagName().equals(tag)) {
            option = new UnitListOption(spec);

        } else if (UnitTypeOption.getXMLElementTagName().equals(tag)) {
            option = new UnitTypeOption(spec);

        } else {
            logger.finest("Parsing of option type '" + tag
                + "' is not implemented yet");
            in.nextTag();
        }

        if (option != null) option.readFromXML(in);
        return option;
    }
}
