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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.dom4j.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Class to test if we can interpret the WFS results correctly.
 */
class Natura2000WfsInterpreterTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "wfs_result_pdok.xml",
  })
  void testParseAreas(final String fileName) throws IOException {
    final Natura2000WfsInterpreter interpreter = new Natura2000WfsInterpreter();
    final String resourceLocation = "/example_wfs/" + fileName;
    Document document;
    try (InputStream inputStream = getClass().getResourceAsStream(resourceLocation)) {
      document = interpreter.readDocument(inputStream);
    }

    final Map<String, Nature2000Area> mappedAreas = interpreter.parseAreas(document);

    assertEquals(2, mappedAreas.size(), "Should be 2 areas");
    assertTrue(mappedAreas.containsKey("lepelaarplassen"), "Should contain lepelerplassen area");
    assertTrue(mappedAreas.containsKey("binnenveld"), "Should contain binnenveld area");

    final Nature2000Area lepelaarplassen = mappedAreas.get("lepelaarplassen");
    assertArea(lepelaarplassen, "L_ff79e30b-f9d5-406b-94c0-7b3222ec4204", "Lepelaarplassen", "lepelaarplassen", "POLYGON ((",
        "POINT (142831.323 491155.183)", 3564691);

    final Nature2000Area binnenveld = mappedAreas.get("binnenveld");
    assertArea(binnenveld, "L_7b89a66e-7f44-40f0-9d98-c7245f2c04c1", "Binnenveld", "binnenveld", "MULTIPOLYGON ((",
        "POINT (168590.922 446854.766)", 1114151);
  }

  private void assertArea(final Nature2000Area naturea2000Area, final String id, final String name, final String normalizedName,
      final String geometryPrefix, final String centroid, final double area) {
    assertEquals(id, naturea2000Area.getId(), "ID should be ID in the WFS, but wasn't for " + name);
    assertEquals(name, naturea2000Area.getName(), "Name should be the name from the WFS, but wasn't for " + name);
    assertEquals(normalizedName, naturea2000Area.getNormalizedName(),
        "Normalized name should be name normalized and in lowercase, but wasn't for " + name);
    assertTrue(naturea2000Area.getWktGeometry().startsWith(geometryPrefix),
        "Geometry should start with a geometry-specific prefix, but wasn't for " + name);
    assertEquals(centroid, naturea2000Area.getWktCentroid(),
        "Centroid should be correct, but wasn't for " + name);
    assertEquals(area, naturea2000Area.getArea(), 1, "Area should be more or less correct, but wasn't for " + name);
  }

}
