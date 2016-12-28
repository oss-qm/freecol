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

package net.sf.freecol.common.model;

import java.util.List;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.ObjectWithId;

import javax.swing.ImageIcon;

/**
 * A place where a {@code Locatable} can be put.
 *
 * @see Locatable
 */
public class LocationUtil {


    /**
     * Static frontend to up().
     *
     * @param loc The {@code Location} to improve.
     * @return The improved {@code Location}.
     */
    public static Location upLoc(Location loc) {
        return (loc == null) ? null : loc.up();
    }

    /**
     * Static front end to getRank.
     *
     * @param loc A {@code Location} to check.
     * @return The integer rank of the given location.
     */
    public static int getRank(Location loc) {
        return (loc == null) ? Location.LOCATION_RANK_NOWHERE : loc.getRank();
    }
}
