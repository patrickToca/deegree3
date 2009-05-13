//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------
 This file is part of deegree.
 Copyright (C) 2001-2009 by:
 Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 Contact:

 Andreas Poth
 lat/lon GmbH
 Aennchenstr. 19
 53177 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de
 ---------------------------------------------------------------------------*/

package org.deegree.rendering.r3d.opengl.rendering.managers;

import java.util.List;

import javax.vecmath.Point3d;

import org.deegree.commons.utils.Pair;
import org.deegree.commons.utils.math.Vectors3f;
import org.deegree.rendering.r3d.ViewParams;

/**
 * The <code>SwitchLevels</code> class TODO add class documentation here.
 * 
 * @author <a href="mailto:bezema@lat-lon.de">Rutger Bezema</a>
 * @author last edited by: $Author$
 * @version $Revision$, $Date$
 * 
 */
public class SwitchLevels {

    private final static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger( SwitchLevels.class );

    /**
     * Array containing min,max for each level.
     */
    private final double[][] levels;

    /**
     * Create a new lod switch containing no levels.
     * 
     */
    public SwitchLevels() {
        this( null );
    }

    /**
     * Create a new level
     * 
     * @param levels
     */
    public SwitchLevels( List<Pair<Double, Double>> levels ) {
        if ( levels == null ) {
            this.levels = new double[0][0];
        } else {
            this.levels = new double[levels.size()][2];
            int i = 0;
            for ( Pair<Double, Double> p : levels ) {
                if ( p != null ) {
                    double min = p.first;
                    double max = p.second;
                    if ( min > max ) {
                        LOG.warn( "Min is larger as max, inversing min and max." );
                        min = max;
                        max = p.first;
                    }
                    if ( i > 0 ) {
                        if ( Math.abs( min - this.levels[i - 1][1] ) > 1E-3 ) {
                            LOG.warn( "Min (" + min + ") does not match previous max interval value ("
                                      + this.levels[i - 1][1] + ") adjusting to this value." );
                            min = this.levels[i - 1][1] + 0.0001;
                            if ( min > max ) {
                                LOG.warn( "Min is larger as max, inversing min and max." );
                                double t = min;
                                min = max;
                                max = t;
                            }
                        }
                    }
                    this.levels[i][0] = Math.max( 0, min );
                    this.levels[i++][1] = Math.max( 0, max );
                }
            }
        }
    }

    /**
     * 
     * @param viewParams
     * @param position
     * @param maxNumberOfLevels
     * @param sizeInUnits
     *            of the building (normally the length of the min-max points of the bbox)
     * @return the level of detail calculated from the configured level switches
     */
    public int calcLevel( ViewParams viewParams, float[] position, int maxNumberOfLevels, float sizeInUnits ) {
        int level = Math.min( maxNumberOfLevels, levels.length - 1 );
        int result = -1;

        Point3d e = viewParams.getViewFrustum().getEyePos();
        float[] eye = new float[] { (float) e.x, (float) e.y, (float) e.z };
        double distance = Vectors3f.distance( eye, position );

        double worldSize = viewParams.estimatePixelSizeForSpaceUnit( distance );
        for ( ; level >= 0 && result == -1; --level ) {
            if ( worldSize >= max( level ) ) {
                result = level;
            }
            if ( worldSize >= min( level ) && worldSize < max( level ) ) {
                // if the level was the maximum level, return the maximum quality level.
                if ( level == levels.length - 1 ) {
                    result = maxNumberOfLevels;
                } else {
                    result = level;
                }
            }
        }
        return result;
    }

    private double min( int level ) {
        return levels[level][0];
    }

    private double max( int level ) {
        return levels[level][1];
    }
}
