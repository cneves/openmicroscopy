/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.roi;

import static omero.rtypes.rdouble;
import static omero.rtypes.rint;
import static omero.rtypes.rlong;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import ome.conditions.ApiUsageException;
import ome.model.IObject;
import ome.model.core.Pixels;
import ome.services.messages.ShapeChangeMessage;
import ome.services.util.Executor;
import ome.tools.hibernate.SessionFactory;
import ome.util.Filterable;
import ome.util.SqlAction;
import ome.util.SqlAction.IdRowMapper;
import omero.RBool;
import omero.RInt;
import omero.api.RoiOptions;
import omero.api.RoiStats;
import omero.api.ShapePoints;
import omero.api.ShapeStats;
import omero.model.Ellipse;
import omero.model.Line;
import omero.model.Point;
import omero.model.Rect;
import omero.model.Shape;
import omero.model.SmartEllipseI;
import omero.model.SmartLineI;
import omero.model.SmartPointI;
import omero.model.SmartRectI;
import omero.model.SmartShape;
import omero.util.IceMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.context.ApplicationListener;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Strategy for handling the conversion between {@link Shape shapes} and
 * database-specific geometries.
 * 
 * Implements {@link ApplicationListener} in order to keep the strategy-specific
 * geometry columns in sync when a {@link ShapeChangeMessage} is published.
 * 
 * @since Beta4.1
 */
public class GeomTool {

    protected Log log = LogFactory.getLog(GeomTool.class);

    protected final AtomicBoolean hasShapes = new AtomicBoolean(true);

    protected final SqlAction sql;

    protected final SessionFactory factory;

    protected final PixelData data;

    protected final Executor ex;

    protected final String uuid;

    public GeomTool(PixelData data, SqlAction sql,
            SessionFactory factory) {
        this(data, sql, factory, null, null);
    }

    public GeomTool(PixelData data, SqlAction sql,
            SessionFactory factory, Executor ex, String uuid) {
        this.data = data;
        this.sql = sql;
        this.factory = factory;
        this.ex = ex;
        this.uuid = uuid;
    }

    /**
     * Loads just the shape and no other relationships. This
     * 
     * @param shapeId
     * @param session
     * @return
     */
    private Shape justShapeById(long shapeId, Session session) {
        Query q = session.createQuery("select s from Shape s where s.id = :id");
        q.setParameter("id", shapeId);
        ome.model.roi.Shape shape = (ome.model.roi.Shape) q.uniqueResult();
        return (Shape) new ShapeMapper().map(shape);
    }

    //
    // Factory methods
    //

    public List<Shape> random(int count) {
        if (count < 1 || count > 100000) {
            throw new RuntimeException("Count out of bounds: " + count);
        }

        Map<Class, RoiTypes.ObjectFactory> map = RoiTypes.ObjectFactories;
        List<Class> types = new ArrayList<Class>(map.keySet());
        List<Shape> shapes = new ArrayList<Shape>();
        Random r = new Random();

        try {
            while (shapes.size() < count) {
                int which = r.nextInt(types.size());
                Class type = types.get(which);
                Method m = type.getMethod("randomize", Random.class);
                RoiTypes.ObjectFactory of = map.get(type);
                SmartShape s = (SmartShape) of.create("");
                m.invoke(s, r);
                shapes.add((Shape) s);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failure on creating shape "
                    + shapes.size(), e);
        }

        return shapes;
    }

    public Line ln(double x1, double y1, double x2, double y2) {
        SmartLineI rect = new SmartLineI();
        rect.setX1(rdouble(x1));
        rect.setY1(rdouble(y1));
        rect.setX2(rdouble(x2));
        rect.setY2(rdouble(y2));
        return rect;
    }

    public Rect rect(double x, double y, double w, double h) {
        SmartRectI rect = new SmartRectI();
        rect.setX(rdouble(x));
        rect.setY(rdouble(y));
        rect.setWidth(rdouble(w));
        rect.setHeight(rdouble(h));
        return rect;
    }

    public Point pt(double x, double y) {
        SmartPointI pt = new SmartPointI();
        pt.setCx(rdouble(x));
        pt.setCy(rdouble(y));
        return pt;
    }

    public Ellipse ellipse(double cx, double cy, double rx, double ry) {
        SmartEllipseI ellipse = new SmartEllipseI();
        ellipse.setCx(rdouble(cx));
        ellipse.setCy(rdouble(cy));
        ellipse.setRx(rdouble(rx));
        ellipse.setRy(rdouble(ry));
        return ellipse;
    }

    public Ellipse ellipse(double cx, double cy, double rx, double ry, int t,
            int z) {
        Ellipse ellipse = ellipse(cx, cy, rx, ry);
        ellipse.setTheT(rint(t));
        ellipse.setTheZ(rint(z));
        return ellipse;
    }

    //
    // Conversion methods
    //

    public String dbPath(Shape shape) {

        if (shape == null) {
            return null;
        }

        SmartShape ss = assertSmart(shape);
        List<Point> points = ss.asPoints();
        // ticket:1652 - to prevent the objects from being repeatedly
        // checked, they must be set to something. Here we are using
        // the top-left point as a default (like SmartText)
        if (points == null) {
            return "'(0,0)'";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("'(");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            SmartShape.Util.appendDbPoint(sb, points.get(i));
        }
        sb.append(")'");
        return sb.toString();
    }

    //
    // Database access methods
    //

    public ShapePoints getPoints(long shapeId, Session session) {
        Shape shape = justShapeById(shapeId, session);
        SmartShape smart = assertSmart(shape);
        final List<Integer> xs = new ArrayList<Integer>(); // This is not good
        final List<Integer> ys = new ArrayList<Integer>(); // nor is this.
        smart.areaPoints(new SmartShape.PointCallback() {
            public void handle(int x, int y) {
                xs.add(x);
                ys.add(y);
            }
        });
        ShapePoints sp = new ShapePoints();
        sp.x = new int[xs.size()];
        sp.y = new int[ys.size()];
        for (int i = 0; i < sp.x.length; i++) {
            sp.x[i] = xs.get(i);
            sp.y[i] = ys.get(i);
        }
        return sp;
    }

    public RoiStats getStats(List<Long> shapeIds) {

        if (shapeIds == null) {
            return null; // EARLY EXIT
        }

        final Session session = factory.getSession();
        final RoiStats rs = new RoiStats();
        rs.perShape = new ShapeStats[shapeIds.size()];

        for (int i = 0; i < shapeIds.size(); i++) {

            final long shapeId = shapeIds.get(i);

            final ome.model.roi.Shape shape = (ome.model.roi.Shape) session
                    .createQuery(
                            "select s from Shape s "
                                    + "left outer join fetch s.channels selected " // optional
                                    + "join fetch s.roi r join fetch r.image i "
                                    + "join fetch i.pixels p join fetch p.channels c "
                                    + "join fetch c.logicalChannel lc "
                                    + "where s.id = :id").setParameter("id",
                            shapeId).uniqueResult();
            final SmartShape smartShape = (SmartShape) new ShapeMapper()
                    .map(shape);

            final ome.model.roi.Roi roi = shape.getRoi();
            final ome.model.core.Image img = roi.getImage();
            final ome.model.core.Pixels pix = img.getPrimaryPixels();

            final long roiId = roi.getId();
            final long imgId = img.getId();
            final long pixId = pix.getId();

            final int maxZ = pix.getSizeZ();
            final int maxT = pix.getSizeT();

            // We only take the values for the first Shape. If this call is
            // being made with different shapes, then the user will know as
            // much.
            if (rs.combined == null) {
                rs.roiId = roiId;
                rs.imageId = imgId;
                rs.pixelsId = pixId;

                int ch = pix.sizeOfChannels();
                rs.combined = makeStats(ch);
                rs.combined.shapeId = -1;
                rs.combined.channelIds = new long[ch];
                for (int w = 0; w < ch; w++) {
                    rs.combined.channelIds[w] = pix.getChannel(w)
                            .getLogicalChannel().getId();
                }
            }
            ShapeStats agg = rs.combined;

            final ShapeStats stats = makeStats(pix, shape);
            stats.shapeId = shape.getId();

            final int ch = stats.channelIds.length;
            final double[] sumOfSquares = new double[ch];

            final Integer theZ = shape.getTheZ(); // May be null
            final Integer theT = shape.getTheT(); // May be null

            final int startZ = (theZ == null) ? 0 : theZ.intValue();
            final int startT = (theT == null) ? 0 : theT.intValue();

            final int endZ = (theZ == null) ? (maxZ - 1) : theZ.intValue();
            final int endT = (theT == null) ? (maxT - 1) : theT.intValue();

            SmartShape.PointCallback cb = new SmartShape.PointCallback() {

                public void handle(int x, int y) {

                    for (int w = 0; w < ch; w++) {

                        for (int z = startZ; z <= endZ; z++) {
                            for (int t = startT; t <= endT; t++) {

                                // WHAT TO DO ABOUT THE CHANNELS IN AGGREGATION?
                                stats.pointsCount[w]++;
                                double value = data.get(pixId, x, y, z, w, t);
                                stats.min[w] = Math.min(value, stats.min[w]);
                                stats.max[w] = Math.max(value, stats.max[w]);
                                stats.sum[w] += value;
                                sumOfSquares[w] += value * value;

                            }
                        }

                    }
                }
            };

            smartShape.areaPoints(cb);
            for (int w = 0; w < ch; w++) {

                stats.mean[w] = stats.sum[w] / stats.pointsCount[w];
                if (stats.pointsCount[w] > 1) {
                    double sigmaSquare = (sumOfSquares[w] - stats.sum[w]
                            * stats.sum[w] / stats.pointsCount[w])
                            / (stats.pointsCount[w] - 1);
                    if (sigmaSquare > 0) {
                        stats.stdDev[w] = Math.sqrt(sigmaSquare);
                    }
                }
            }

            rs.perShape[i] = stats;
        }

        return rs;

    }

    /**
     * Maps from multiple possible user-provided names of shapes (e.g.
     * "::omero::model::Text", "Text", "TextI", "omero.model.TextI",
     * "ome.model.roi.Text", ...) to the definitive database discriminator.
     * 
     * @param string
     * @return
     */
    public Object discriminator(String string) {
        if (string == null || string.length() == 0) {
            throw new ApiUsageException("Empty string");
        }
        String[] s = string.split("[.:]");
        string = s[s.length - 1];
        string = string.toLowerCase();
        if (string.endsWith("i")) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    //
    // helpers
    //

    private ShapeStats makeStats(int ch) {
        ShapeStats stats = new ShapeStats();
        stats.channelIds = new long[ch];
        stats.min = new double[ch];
        stats.max = new double[ch];
        stats.sum = new double[ch];
        stats.mean = new double[ch];
        stats.stdDev = new double[ch];
        stats.pointsCount = new long[ch];
        Arrays.fill(stats.min, 0, ch, Double.MAX_VALUE);
        return stats;
    }

    private ShapeStats makeStats(Pixels pix, ome.model.roi.Shape shape) {

        // If the shape does not explicitly list any channels, then we will
        // need to take all the available channels from the pixels.

        boolean shapeChannels = true;
        int ch = shape.sizeOfChannels();
        if (ch == 0) {
            shapeChannels = false;
            ch = pix.sizeOfChannels();
        }

        ShapeStats stats = makeStats(ch);

        for (int w = 0; w < ch; w++) {
            if (shapeChannels) {
                stats.channelIds[w] = -1; // FIXME
            } else {
                stats.channelIds[w] = pix.getChannel(w).getLogicalChannel()
                        .getId();
            }

        }

        return stats;
    }

    private SmartShape assertSmart(Shape shape) {
        if (!SmartShape.class.isAssignableFrom(shape.getClass())) {
            throw new RuntimeException(
                    "Internally only SmartShapes should be used! not "
                            + shape.getClass());
        }

        SmartShape ss = (SmartShape) shape;
        return ss;
    }

    private static class ShapeMapper extends IceMapper {

        boolean called = false;

        /**
         * Overrides {@link IceMapper#filter(String, Filterable)} in order to
         * only allow descending one level deep.
         */
        @Override
        public Filterable filter(String fieldId, Filterable source) {

            if (!called) {
                called = true;
                return super.filter(fieldId, source);
            }

            Object o = findTarget(source);
            if (o instanceof omero.model.IObject) {
                IObject iobj = (IObject) source;
                omero.model.IObject robj = (omero.model.IObject) o;
                robj.setId(rlong(iobj.getId()));
                robj.unload();
            }

            return source;

        }

    }

}
