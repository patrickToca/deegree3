//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2011 by:
 - Department of Geography, University of Bonn -
 and
 - lat/lon GmbH -

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.filter.sql.mssql;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.deegree.cs.coordinatesystems.ICRS;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.io.WKBReader;
import org.deegree.geometry.io.WKBWriter;
import org.deegree.geometry.io.WKTReader;
import org.deegree.geometry.io.WKTWriter;
import org.deegree.geometry.utils.GeometryParticleConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.io.ParseException;

/**
 * {@link GeometryParticleConverter} for PostGIS databases.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
public class MSSQLGeometryConverter implements GeometryParticleConverter {

    private final String column;

    private final ICRS crs;

    private final String srid;

    private final boolean is2d;

    /**
     * Creates a new {@link MSSQLGeometryConverter} instance.
     * 
     * @param column
     *            (unqualified) column that stores the geometry, must not be <code>null</code>
     * @param crs
     *            CRS of the stored geometries, can be <code>null</code>
     * @param srid
     *            MSSQL spatial reference identifier, must not be <code>null</code>
     * @param is2D
     */
    public MSSQLGeometryConverter( String column, ICRS crs, String srid, boolean is2D ) {
        this.column = column;
        this.crs = crs;
        this.srid = srid;
        this.is2d = is2D;
    }

    public String getSelectSnippet( String tableAlias ) {
        if ( is2d )
            return ( tableAlias == null ? "" : ( tableAlias + "." ) ) + column + ".STAsBinary()";
        if ( tableAlias == null )
            return column + ".ToString()";
        return tableAlias + "." + column + ".ToString()";
    }

    public String getSetSnippet() {
        if ( is2d )
            return "geometry::STGeomFromWKB(?, " + srid + ")";
        return "geometry::Parse(?)";
    }

    @Override
    public Geometry toParticle( ResultSet rs, int colIndex )
                            throws SQLException {
        Object sqlValue = rs.getObject( colIndex );
        try {
            if ( sqlValue != null ) {
                if ( sqlValue instanceof byte[] ) {
                    if ( is2d ) {
                        return WKBReader.read( (byte[]) sqlValue, crs );
                    }
                    // hur hur, not using cp1252 any more, are we?
                    sqlValue = new String( (byte[]) sqlValue, "UTF-16LE" );
                }
                return new WKTReader( crs ).read( sqlValue.toString() );
            }
        } catch ( UnsupportedEncodingException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch ( ParseException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setParticle( PreparedStatement stmt, Geometry particle, int paramIndex )
                            throws SQLException {
        if ( particle == null ) {
            stmt.setObject( paramIndex, null );
        } else if ( is2d ) {
            try {
                stmt.setBytes( paramIndex, WKBWriter.write( particle ) );
            } catch ( ParseException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            stmt.setString( paramIndex, WKTWriter.write( particle ) );
        }
    }

    @Override
    public String getSrid() {
        return srid;
    }

    @Override
    public ICRS getCrs() {
        return crs;
    }
}