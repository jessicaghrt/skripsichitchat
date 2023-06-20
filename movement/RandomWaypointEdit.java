/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.Coord;
import core.Settings;

/**
 * Random waypoint movement model. Creates zig-zag paths within the simulation
 * area.
 */
public class RandomWaypointEdit extends MovementModel {

    /**
     * how many waypoints should there be per path
     */
    private static final int PATH_LENGTH = 1;
    private Coord lastWaypoint;

    

    public RandomWaypointEdit(Settings settings) {
        super(settings);
    }

    protected RandomWaypointEdit(RandomWaypointEdit rwp) {
        super(rwp);
    }

    /**
     * Returns a possible (random) placement for a host
     *
     * @return Random position on the map
     */
    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        Coord c = randomCoord();

        this.lastWaypoint = c;
        return c;
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());
        Coord c = lastWaypoint;

        for (int i = 0; i < PATH_LENGTH; i++) {
            c = randomCoord();
            p.addWaypoint(c);
        }

        this.lastWaypoint = c;
        return p;
    }

    @Override
    public RandomWaypointEdit replicate() {
        return new RandomWaypointEdit(this);
    }

    protected Coord randomCoord() {
        double x = 0, y = 0;

        switch (this.moveArea) {
            case 1:
                x = rng.nextDouble() * (getMaxX() * 1/3 - 0) + 0;
                y = rng.nextDouble() * (getMaxY() - getMaxY() * 2/3) + getMaxY() * 2/3;
                return new Coord(x, y);
            case 2:
                x = rng.nextDouble() * (getMaxX() * 2/3 - getMaxX() * 1/3) + getMaxX() * 1/3;
                y = rng.nextDouble() * (getMaxY() - getMaxY() * 2/3) + getMaxY() * 2/3;
                return new Coord(x, y);
            case 3:
                x = rng.nextDouble() * (getMaxX() - getMaxX() * 2/3) + getMaxX() * 2/3;
                y = rng.nextDouble() * (getMaxY() - getMaxY() * 2/3) + getMaxY() * 2/3;
                return new Coord(x, y);
            case 4:
                x = rng.nextDouble() * (getMaxX() * 1/3 - 0) + 0;
                y = rng.nextDouble() * (getMaxY() * 2/3 - getMaxY() * 1/3) + getMaxY() * 1/3;
                return new Coord(x, y);
            case 5:
                x = rng.nextDouble() * (getMaxX() * 2/3 - getMaxX() * 1/3) + getMaxX() * 1/3;
                y = rng.nextDouble() * (getMaxY() * 2/3 - getMaxY() * 1/3) + getMaxY() * 1/3;
                return new Coord(x, y);
            case 6:
                x = rng.nextDouble() * (getMaxX() - getMaxX() * 2/3) + getMaxX() * 2/3;
                y = rng.nextDouble() * (getMaxY() * 2/3 - getMaxY() * 1/3) + getMaxY() * 1/3;
                return new Coord(x, y);
            case 7:
                x = rng.nextDouble() * (getMaxX() * 1/3 - 0) + 0;
                y = rng.nextDouble() * (getMaxY() * 1/3 - 0) + 0;
                return new Coord(x, y);
            case 8:
                x = rng.nextDouble() * (getMaxX() * 2/3 - getMaxX() * 1/3) + getMaxX() * 1/3;
                y = rng.nextDouble() * (getMaxY() * 1/3 - 0) + 0;
                return new Coord(x, y);
            case 9:
                x = rng.nextDouble() * (getMaxX() - getMaxX() * 2/3) + getMaxX() * 2/3;
                y = rng.nextDouble() * (getMaxY() * 1/3 - 0) + 0;
                return new Coord(x, y);
            default:
                return new Coord(rng.nextDouble() * getMaxX(),
                                rng.nextDouble() * getMaxY());
        }
    }
}
