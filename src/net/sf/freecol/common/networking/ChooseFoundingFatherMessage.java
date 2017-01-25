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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to choose a founding father.
 */
public class ChooseFoundingFatherMessage extends AttributeMessage {

    public static final String TAG = "chooseFoundingFather";
    private static final String FOUNDING_FATHER_TAG = "foundingFather";

    /**
     * Create a new {@code ChooseFoundingFatherMessage} with the specified
     * fathers.
     *
     * @param fathers The {@code FoundingFather}s to choose from.
     * @param ff The {@code FoundingFather} to select.
     */
    public ChooseFoundingFatherMessage(List<FoundingFather> fathers,
                                       FoundingFather ff) {
        super(TAG, FOUNDING_FATHER_TAG, (ff == null) ? null : ff.getId());

        setFatherAttributes(fathers);
    }

    /**
     * Create a new {@code ChooseFoundingFatherMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ChooseFoundingFatherMessage(Game game, Element element) {
        super(TAG, FOUNDING_FATHER_TAG, getStringAttribute(element, FOUNDING_FATHER_TAG));

        final Specification spec = game.getSpecification();
        setFatherAttributes(transform(FoundingFatherType.allKeys,
                k -> element.hasAttribute(k),
                k -> spec.getFoundingFather(getStringAttribute(element, k))));
    }


    /**
     * Set the attributes arising from a list of founding fathers.
     *
     * @param fathers A list of {@code FoundingFather}.
     */
    private void setFatherAttributes(List<FoundingFather> fathers) {
        setStringAttributes(transform(fathers, alwaysTrue(),
                Function.identity(),
                Collectors.toMap(ff -> ff.getType().getKey(),
                                 FoundingFather::getId)));
    }


    // Public interface

    /**
     * Get the chosen father.
     *
     * @param game The {@code Game} to lookup the father in.
     * @return The chosen {@code FoundingFather}, or null if none set.
     */
    public final FoundingFather getFather(Game game) {
        String id = getStringAttribute(FOUNDING_FATHER_TAG);
        return (id == null) ? null
            : game.getSpecification().getFoundingFather(id);
    }

    /**
     * Sets the chosen father.
     *
     * @param ff The {@code FoundingFather} to choose.
     * @return This message.
     */
    public final ChooseFoundingFatherMessage setFather(FoundingFather ff) {
        setStringAttribute(FOUNDING_FATHER_TAG, (ff == null) ? null : ff.getId());
        return this;
    }

    /**
     * Get the list of offered fathers.
     *
     * @param game The FreeCol game being played.
     * @return The offered {@code FoundingFather}s.
     */
    public final List<FoundingFather> getFathers(Game game) {
        final Specification spec = game.getSpecification();
        return transform(FoundingFatherType.allKeys, tid -> hasAttribute(tid),
                         tid -> spec.getFoundingFather(getStringAttribute(tid)));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final List<FoundingFather> offered = serverPlayer.getOfferedFathers();
        final FoundingFather ff = getFather(game);

        if (!serverPlayer.canRecruitFoundingFather()) {
            return serverPlayer.clientError("Player can not recruit fathers: "
                + serverPlayer.getId());
        } else if (ff == null) {
            return serverPlayer.clientError("No founding father selected");
        } else if (!offered.contains(ff)) {
            return serverPlayer.clientError("Founding father not offered: "
                + ff.getId());
        }

        serverPlayer.updateCurrentFather(ff);
        return null;
    }
}
