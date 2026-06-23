/*
 * Copyright the State of the Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package nl.aerius.search.tasks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import kong.unirest.core.Unirest;

/**
 * Rinky dink wfs interpreter that fetches natura 2000 areas and geometries from
 * an open data resource.
 *
 * Should be replaced over the medium term.
 */
@Component
public class Natura2000WfsInterpreter {
  private static final double DOUGLAS_PEUCKER_TOLERANCE = 5D;
  private static final int SRID_RDNEW = 28992;
  // Snap coordinates to mm so reprojection stays deterministic across CPU architectures.
  private static final double RD_NEW_PRECISION_SCALE = 1000D;

  private static final Logger LOG = LoggerFactory.getLogger(Natura2000WfsInterpreter.class);

  private final GeometryFactory rdNewGeometryFactory;
  private final MathTransform wgsToRdNewtransform;

  // @formatter:off
  /**
   * @see <a href='https://www.pdok.nl/introductie/-/article/natura2000-inspire-geharmoniseerd-">pdok page</a>
   */
  @Value("${nl.aerius.wfs.n2000:https://service.pdok.nl/rvo/beschermde-gebieden/natura2000/wfs/v2_0?"
      + "request=GetFeature&service=WFS&version=2.0.0&typeName=beschermde-gebieden:protectedsite&outputFormat=application%2Fgml%2Bxml%3B%20version%3D3.2}")
  private String wfsNatura2000Url;
  // @formatter:on

  public Natura2000WfsInterpreter() {
    this.rdNewGeometryFactory = new GeometryFactory(new PrecisionModel(RD_NEW_PRECISION_SCALE), SRID_RDNEW);
    this.wgsToRdNewtransform = createCRSTransform();
  }

  public Map<String, Nature2000Area> retrieveAreas() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Retrieving from {}", wfsNatura2000Url.split("\\?")[0]);
    }
    final byte[] body = Unirest.get(wfsNatura2000Url)
        .asBytes()
        .getBody();
    Document document;
    try (InputStream wfsStream = new ByteArrayInputStream(body)) {
      document = readDocument(wfsStream);
    } catch (final IOException e1) {
      throw new InterpretationRuntimeException(e1);
    }

    return parseAreas(document);
  }

  protected Document readDocument(final InputStream inputStream) {
    final SAXReader reader = new SAXReader();
    try {
      // https://sonarcloud.io/organizations/aerius/rules?open=java%3AS2755&rule_key=java%3AS2755
      reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    } catch (final SAXException e1) {
      // Crash hard
      throw new InterpretationRuntimeException("Could not set feature disallow-doctype-decl", e1);
    }

    try {
      return reader.read(inputStream);
    } catch (final DocumentException e1) {
      LOG.error("Could not interpret WFS response; assessment areas will not be searchable as a result.");
      throw new InterpretationRuntimeException("Could not interpret WFS document", e1);
    }
  }

  protected Map<String, Nature2000Area> parseAreas(final Document document) {
    final Map<String, Nature2000Area> areas = new HashMap<>();

    final Element rootElem = document.getRootElement();
    final List<?> elements = rootElem.elements();
    elements.forEach(elem -> {
      if (elem instanceof DefaultElement && ((DefaultElement) elem).element("protectedsite") != null) {
        final Nature2000Area area = processArea(((DefaultElement) elem).element("protectedsite"));
        areas.merge(area.getNormalizedName(), area, Natura2000WfsInterpreter::merge);
      }
    });

    return areas;
  }

  private static Nature2000Area merge(final Nature2000Area a, final Nature2000Area b) {
    final String geometryAWkt = a.getWktGeometry();
    final String geometryBWkt = b.getWktGeometry();

    final WKTReader rdr = new WKTReader();
    try {
      final Geometry geometryA = rdr.read(geometryAWkt);
      final Geometry geometryB = rdr.read(geometryBWkt);

      final WKTWriter writer = new WKTWriter();

      final Geometry union = geometryA.union(geometryB);
      final String unionWkt = writer.write(union);

      final Geometry unionCentroid = union.getCentroid();
      final String centroidWkt = writer.write(unionCentroid);
      a.setWktGeometry(unionWkt);
      a.setWktCentroid(centroidWkt);

    } catch (final ParseException e) {
      throw new InterpretationRuntimeException("Cannot read WKT geometries.", e);
    }

    return a;
  }

  private Nature2000Area processArea(final Element protectedSite) {
    final String id = protectedSite
        .elementTextTrim("inspireidIdentifierLocalid");
    final String name = protectedSite
        .elementTextTrim("sitenameGeographicalnameSpellingSpellingofnameText");

    final String normalizedName = normalize(name);

    final Geometry resultGeometry = readGeometry(protectedSite);

    LOG.debug("Area: {} - {} - {} - {}m2", id, normalizedName, name, resultGeometry.getArea());

    final WKTWriter wktWriter = new WKTWriter();
    final String wktGeometry = wktWriter.write(resultGeometry);
    final String wktCentroid = wktWriter.write(resultGeometry.getCentroid());

    return new Nature2000Area(id, name, normalizedName, wktGeometry, wktCentroid, resultGeometry.getArea());
  }

  private Geometry readGeometry(final Element protectedSite) {
    // Service has returned both MultiSurface and MultiGeometry. To ensure it works, test for both
    final Optional<Element> geometryElement = Optional.ofNullable(protectedSite.element("geom"));
    final List<?> members = geometryElement
        .map(e -> e.element("MultiGeometry"))
        .map(e -> e.elements("geometryMember"))
        .orElseGet(() -> geometryElement
            .map(e -> e.element("MultiSurface"))
            .map(e -> e.elements("surfaceMember"))
            .orElseGet(() -> geometryElement.map(List::of).orElseThrow()));
    Geometry finalGeometry = null;
    for (int i = 0; i < members.size(); i++) {
      final Element member = (Element) members.get(i);
      final String geometry = member
          .element("Polygon")
          .element("exterior")
          .element("LinearRing")
          .elementTextTrim("posList");

      final Coordinate[] coords = coordinatesFromString(geometry);
      final Polygon ext = rdNewGeometryFactory.createPolygon(coords);

      if (finalGeometry == null) {
        finalGeometry = ext;
      } else {
        finalGeometry = finalGeometry.union(ext);
      }
    }

    final DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(finalGeometry);
    simplifier.setDistanceTolerance(DOUGLAS_PEUCKER_TOLERANCE);

    return simplifier.getResultGeometry();
  }

  public String normalize(final String name) {
    return Normalizer.normalize(name, Form.NFC)
        .toLowerCase(Locale.ROOT);
  }

  private Coordinate[] coordinatesFromString(final String feature) {
    final String[] parts = feature.split(" ");

    final Coordinate[] coordinates = new Coordinate[parts.length / 2];

    for (int i = 0; i < parts.length; i += 2) {
      try {
        final Coordinate coord = new Coordinate(Double.parseDouble(parts[i]), Double.parseDouble(parts[i + 1]));

        final Envelope env = extractEnvelope(coord, wgsToRdNewtransform);
        final Coordinate centre = env.centre();
        rdNewGeometryFactory.getPrecisionModel().makePrecise(centre);
        coordinates[i / 2] = centre;
      } catch (final NumberFormatException e) {
        LOG.info("Parsing: {} > {}", parts[i], parts[i + 1]);
      }
    }

    return coordinates;
  }

  private static Envelope extractEnvelope(final Coordinate coord, final MathTransform transform) {
    try {
      return JTS.transform(new Envelope(coord), transform);
    } catch (final TransformException e) {
      throw new InterpretationRuntimeException("Failed transform", e);
    }
  }

  private static MathTransform createCRSTransform() {
    final CoordinateReferenceSystem targetCRS;
    try {
      final CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:3035");
      targetCRS = CRS.decode("EPSG:28992");

      return CRS.findMathTransform(sourceCRS, targetCRS);
    } catch (final FactoryException e) {
      LOG.error("Could not initialize coordinate reference system");
      throw new InterpretationRuntimeException(e);
    }
  }
}
