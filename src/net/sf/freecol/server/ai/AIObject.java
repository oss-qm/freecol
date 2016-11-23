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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Element;


/**
 * An <code>AIObject</code> contains AI-related information and methods.
 * Each <code>FreeColGameObject</code>, that is owned by an AI-controlled
 * player, can have a single <code>AIObject</code> attached to it.
 */
public abstract class AIObject extends FreeColObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColObject.class.getName());

    /** The AI this object exists within. */
    private final AIMain aiMain;

    /** Whether the object is uninitialized. */
    protected boolean uninitialized = false;


    /**
     * Creates a new uninitialized <code>AIObject</code>.
     *
     * @param aiMain The main AI-object.
     */
    public AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
        uninitialized = true;
    }

    /**
     * Creates a new uninitialized <code>AIObject</code> with a registerable
     * AI id.
     *
     * @param aiMain The main AI-object.
     * @param id The unique identifier.
     * @see AIMain#addAIObject(String, AIObject)
     */
    public AIObject(AIMain aiMain, String id) {
        this(aiMain);

        if (id != null) {
            setId(id);
            aiMain.addAIObject(id, this);
        }
        uninitialized = true;
    }

    /**
     * Creates a new <code>AIObject</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public AIObject(AIMain aiMain, Element element) {
        this(aiMain);

        readFromXMLElement(element);
        addAIObjectWithId();
    }

    /**
     * Creates a new <code>AIObject</code>.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     * @see AIObject#readFromXML
     */
    public AIObject(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        this(aiMain);

        readFromXML(in);
        addAIObjectWithId();
    }

    /**
     * Disposes this <code>AIObject</code> by removing the reference
     * to this object from the enclosing AIMain.
     */
    public void dispose() {
        getAIMain().removeAIObject(getId());
    }

    /**
     * Has this AIObject been disposed?
     *
     * @return True if this AIObject was disposed.
     */
    public boolean isDisposed() {
        return getAIMain().getAIObject(getId()) == null;
    }

    /**
     * Convenience accessor for the main AI-object.
     *
     * @return The <code>AIMain</code>.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Convenience accessor for the game.
     *
     * @return The <code>Game</code>.
     */
    public Game getGame() {
        return aiMain.getGame();
    }

    /**
     * Convenience accessor for the specification.
     *
     * @return The <code>Specification</code>.
     */
    public Specification getSpecification() {
        return getGame().getSpecification();
    }

    /**
     * Checks if this <code>AIObject</code>
     * is uninitialized. That is: it has been referenced
     * by another object, but has not yet been updated with
     * {@link #readFromXML}.
     *
     * @return <code>true</code> if this object is not initialized.
     */
    public boolean isUninitialized() {
        return uninitialized;
    }

    /**
     * Checks the integrity of this AI object.
     * Subclasses should override.
     *
     * @return True if the object is valid.
     */
    public boolean checkIntegrity() {
        return !isUninitialized();
    }

    /**
     * Fixes integrity problems with this AI object.
     * This simplest solution is just to dispose of the object.
     */
    public void fixIntegrity() {
        dispose();
    }

    /**
     * Adds this object to the AI main if it has a non-null id.
     */
    protected void addAIObjectWithId() {
        if (getId() != null) aiMain.addAIObject(getId(), this);
    }

    /**
     * Gets a location's settlement if it has one, or failing that at
     * least make sure the location is not just a carrier.  The intent
     * is to increase the relevance of a location used as a target.
     *
     * @param loc The <code>Location</code> to test.
     * @return The location settlement if any, otherwise, the original
     *     location.
     */
    public static Location upLoc(Location loc) {
        if (loc instanceof Unit) loc = ((Unit)loc).getLocation();
        return (loc == null) ? null
            : (loc.getSettlement() != null) ? loc.getSettlement()
            : loc;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE, getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void readFromXML(XMLStreamReader in) throws XMLStreamException {
        super.readFromXML(in);

        uninitialized = false;
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "AIObject".
     */
    public static String getXMLElementTagName() {
        return "AIObject";
    }
}
