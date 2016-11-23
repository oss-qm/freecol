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

package net.sf.freecol.server.generator;

//import java.util.logging.Logger;

import java.util.EnumMap;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.TileImprovement;


/**
 * This class facilitates building, editing the TileImprovement style
 * for rivers Rivers on the Map are composed of many individual
 * TileImprovements displayed on each Tile the river flows through The
 * river TileImprovement on a Tile has a style which represents the
 * inputs/outputs of water to/from neighboring Tiles This class allows
 * manipulation of individual stream(s) to neighboring Tiles (there
 * are many in case of confluence)
 */
public class RiverSection {

//    private static final Logger logger = Logger.getLogger(RiverImprovementBuilder.class.getName());

    private static final char[] template = new char[] {
        '0', '1', '2', '3'
    };

    /**
     * River magnitude (size) for each direction toward the edges of the tile
     */
    private java.util.Map<Direction, Integer> branches =
        new EnumMap<Direction, Integer>(Direction.class);

    /**
     * River magnitude (size) at the center of the tile
     */
    private int size = TileImprovement.SMALL_RIVER;

    /**
     * Direction the river is flowing toward, at the current section
     */
    public Direction direction;

    /**
     * Position of the current river section
     */
    private Map.Position position;

    /**
     * Creates a new RiverSection with the given branches. This
     * constructor is used by the MapEditor.
     *
     * @param branches The encoded style
     */
    public RiverSection(java.util.Map<Direction, Integer> branches) {
        this.branches = branches;
    }

    /**
     * Constructor used to automatically generate rivers.
     *
     * @param position The map position
     * @param direction The direction the river is flowing toward
     */
    public RiverSection(Map.Position position, Direction direction) {
        this.position = position;
        this.direction = direction;
        setBranch(direction, TileImprovement.SMALL_RIVER);
    }

    /**
     * Returns the position
     * @return position
     */
    public Map.Position getPosition() {
        return position;
    }

    /**
     * Returns the size
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size of a branch
     */
    public void setBranch(Direction direction, int size) {
        if (size != TileImprovement.SMALL_RIVER) {
            size = TileImprovement.LARGE_RIVER;
        }
        branches.put(direction, size);
    }

    /**
     * Gets the size of a branch
     */
    public int getBranch(Direction direction) {
        if (branches.containsKey(direction)) {
            return branches.get(direction);
        } else {
            return TileImprovement.NO_RIVER;
        }
    }

    /**
     * Removes a branch
     */
    public void removeBranch(Direction direction) {
        branches.remove(direction);
    }

    /**
     * Increases the size a branch
     */
    public void growBranch(Direction direction, int increment) {
        int newSize = Math.min(TileImprovement.LARGE_RIVER,
                               Math.max(TileImprovement.NO_RIVER,
                                        getBranch(direction) + increment));
        setBranch(direction, newSize);
    }

    /**
     * Increases the size of this section by one.
     */
    public void grow() {
        this.size++;
        setBranch(direction, TileImprovement.LARGE_RIVER);
    }


    public String encodeStyle() {
        String result = new String();
        for (Direction direction : Direction.longSides) {
            result = result.concat(Integer.toString(getBranch(direction), Character.MAX_RADIX));
        }
        return result;
    }


}
