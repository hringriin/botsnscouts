/*
 * Created by dirk.
 * Date: Mar 10, 2002, 7:56:36 PM
 * Id: $Id$
 */

package de.botsnscouts.autobot;

import de.botsnscouts.board.Board;
import de.botsnscouts.board.Floor;
import de.botsnscouts.util.Location;
import de.botsnscouts.util.Directions;
import org.apache.log4j.Category;

import java.util.Map;
import java.util.HashMap;

public class AdvDistanceCalculator extends DistanceCalculator {
    static final Category CAT=Category.getInstance(AdvDistanceCalculator.class);

    /** Dimension 0: flag
     *            1: x-Coordinate
     *            2: y-Coordinate
     *            3: Facing
     */
    int[][][][] distances;

    private AdvDistanceCalculator(Board board) {
        super(board);
    }

    protected int getDistance(int x, int y, int flag, int facing) {
        return distances[flag-1][x][y][facing];
    }

    // if subclasses have larger "distances"
    int getScalingFactor() {
        return 4;
    }

    public void preCalculate() {
        if (distances == null){
            distances = new int[board.getFlags().length][board.getSizeX()+2]
                               [board.getSizeY()+2][4];
            CAT.debug("new distances Array: "+distances.length+"x"+
                    distances[0].length+"x"+distances[0][0].length+"x"+
                    distances[0][0][0].length);
        }

        Location[] flags=board.getFlags();
        for (int i = 0; i < flags.length; i++)
            preCalculate(i);
    }

    private void preCalculate(int flag) {
        CAT.debug("preC(int) called with "+flag);
        Location location = board.getFlags()[flag];
        initialize(distances[flag]);
        for (int facing=0;facing<4;facing++){
            distances[flag][location.getX()][location.getY()][facing]=0;
            preCalculate(distances[flag], location.getX(), location.getY(), facing);
        }
    }

    private void initialize(int[][][] dist) {
        for (int xx=0; xx<dist.length; xx++)
            for (int yy=0; yy<dist[xx].length; yy++)
                for (int facing=0; facing<4; facing++)
                    dist[xx][yy][facing]=8888;
    }

    private final static int TURN_COST=4;
    private final static int M1_COST=4;
    // Speed 1, speed 2
    private final static int[] BELT_COST_PRO = { 2 , 1 };
    private final static int[] BELT_COST_CONTRA = { 6, 8 };

    /** relaxes all neighbors of the given vertex */
    private void preCalculate(int[][][] dist, int x, int y, int facing) {
        // turns
        relax(dist, x, y, rotateleft(facing), x, y, facing, TURN_COST);
        relax(dist, x, y, rotateright(facing), x, y, facing, TURN_COST);

        // m1
        Floor target;
        // We have to look into the 'next square' to apply belt modifiers
        // properly.
        if (!hasNorthWall(x, y))
            checkSquare(floor(x,y+1), x, y+1, facing, dist, x, y,
                    Directions.SOUTH, Directions.NORTH);

        if (!hasSouthWall(x,y))
            checkSquare(floor(x,y-1), x, y-1, facing, dist, x, y,
                    Directions.NORTH, Directions.SOUTH);

        if (!hasEastWall(x,y))
            checkSquare(floor(x+1,y), x+1, y, facing, dist, x, y,
                    Directions.WEST, Directions.EAST);

        if (!hasWestWall(x,y))
            checkSquare(floor(x-1,y), x-1, y, facing, dist, x, y,
                    Directions.EAST, Directions.WEST);
    }

    private void checkSquare(Floor target, int x, int y, int facing,
                             int[][][] dist, int oldx, int oldy,
                             int good_direction, int bad_direction) {
        if (!target.isPit()) {
            if (target.isBelt()){
                if (target.getBeltDirection() == good_direction)
                    // Belt in coorect direction
                    relax(dist, x, y, facing, oldx, oldy, facing,
                            BELT_COST_PRO[target.getBeltSpeed()-1]);
                else if (target.getBeltDirection() == bad_direction)
                    // Belt in opposite direction
                    relax(dist, x, y, facing, oldx, oldy, facing,
                            BELT_COST_CONTRA[target.getBeltSpeed()-1]);
                else
                    // Belt in perpendicular direction: analogous to no belt
                    if (facing == good_direction)
                        relax(dist, x, y, facing, oldx, oldy, facing, M1_COST);
            } else {
                // no belt: only relax if facing the correct way
                if (facing == good_direction)
                    relax(dist, x, y, facing, oldx, oldy, facing, M1_COST);
            }
        }
    }

    private void relax(int[][][] dist, int x,    int y,    int facing,
                                       int oldx, int oldy, int oldfacing, int cost) {
        if (dist[x][y][facing] > dist[oldx][oldy][oldfacing]+cost) {
            dist[x][y][facing] = dist[oldx][oldy][oldfacing]+cost;
            preCalculate(dist, x, y, facing);
        }
    }


    private static Map uniqueInstances = new HashMap();

    /**
     * Returns a reference to a DirkDistanceCalculator. To be used instead
     * of constructor. Guarantees only one DDB (which is reusable) is initialized
     * per board per VM.
     */
    public static synchronized AdvDistanceCalculator getInstance(Board board) {
        CAT.debug("getInstance called with board "+board);
        if (uniqueInstances.get(board) == null)
            uniqueInstances.put(board, new AdvDistanceCalculator(board));
        CAT.debug("ok, returning");
        return (AdvDistanceCalculator) uniqueInstances.get(board);
    }
}
