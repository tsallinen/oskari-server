package org.oskari.service.mvt;

import java.util.Arrays;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.GeometryEditor.GeometryEditorOperation;

/**
 * This translates the real-world coordinates of a geometry to the inner
 * virtual-coordinate space of the tile. Inside the tile a coordinate consists
 * of two integers (x,y) both in the range of [0,extent] (usually extent=4096).
 * 
 * As a result of this snapping to integer grid, vertices of the original
 * geometry might snap to the same point thereby creating duplicate points,
 * which we don't want. Those are removed. This can make Points in MultiPoint
 * disappear, LineStrings to deprecate to a single Point and LinearRings to collapse.
 * 
 * In case the generated geometry is not valid null is returned.
 * For example when LineString deprecates to a Single Point, or Polygons exterior
 * ring collapses.
 */
public class ToMVTSpace implements GeometryEditorOperation {

    private final double tx;
    private final double ty;
    private final double sx;
    private final double sy;

    public ToMVTSpace(double tx, double ty, double sx, double sy) {
        this.tx = tx;
        this.ty = ty;
        this.sx = sx;
        this.sy = sy;
    }

    public Geometry edit(Geometry geometry, GeometryFactory factory) {
        if (geometry instanceof LinearRing) {
            Coordinate[] coords = ((LinearRing) geometry).getCoordinates();
            Coordinate[] edited = edit(coords, true, 4);
            if (edited == null) {
                // Too few points remaining
                return null;
            }
            return factory.createLinearRing(edited);
        }

        if (geometry instanceof LineString) {
            Coordinate[] coords = ((LineString) geometry).getCoordinates();
            Coordinate[] edited = edit(coords, false, 2);
            if (edited == null) {
                // Too few points remaining
                return null;
            }
            return factory.createLineString(edited);
        }
        
        if (geometry instanceof MultiPoint) {
            Coordinate[] points = ((MultiPoint) geometry).getCoordinates();
            Coordinate[] edited = edit(points, false, 0);
            if (edited == null) {
                return null;
            }
            if (edited.length == 1) {
                return factory.createPoint(edited[0]);
            }
            return factory.createMultiPoint(edited);
        }

        if (geometry instanceof Point) {
            Coordinate coord = ((Point) geometry).getCoordinate();
            int x = (int) Math.round(sx * (coord.x - tx));
            int y = (int) Math.round(sy * (coord.y - ty));
            return factory.createPoint(new Coordinate(x, y));
        }

        return geometry;
    }

    public Coordinate[] edit(Coordinate[] coords, boolean mustClose, int minNumPoints) {
        Coordinate[] edited = new Coordinate[coords.length];
        int n = 0;

        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        for (Coordinate coord : coords) {
            int x = (int) Math.round(sx * (coord.x - tx));
            int y = (int) Math.round(sy * (coord.y - ty));
            if (x == x0 && y == y0) {
                continue;
            }
            edited[n++] = new Coordinate(x, y);
            x0 = x;
            y0 = y;
        }

        if (n < minNumPoints) {
            return null;
        }
        if (n == edited.length) {
            return edited;
        }
        return Arrays.copyOf(edited, n);
    }

}
