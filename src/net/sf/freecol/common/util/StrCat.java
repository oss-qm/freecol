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

package net.sf.freecol.common.util;


import java.lang.StringBuilder;


public class StrCat {
    private String delimiter = ", ";
    private int counter = 0;
    private StringBuilder sb = new StringBuilder();

    public StrCat(String d) {
        delimiter = d;
    }

    public final boolean isEmpty() {
        return counter == 0;
    }

    /**
     * directly add a string to the underlying buffer, w/o any
     * any delimiter. also dont count that as an element.
     */
    public final StrCat append(String s) {
        sb.append(s);
        return this;
    }

    /**
     * add another element and fill the delimiter
     * in between it's not the first one.
     */
    public final void add(String s) {
        if ((s == null) || s.equals("")) return;

        if (counter == 0) {
            sb.append(s);
        } else {
            sb.append(delimiter);
            sb.append(s);
        }
        counter++;
    }

    public final String toString() {
        return sb.toString();
    }
}
