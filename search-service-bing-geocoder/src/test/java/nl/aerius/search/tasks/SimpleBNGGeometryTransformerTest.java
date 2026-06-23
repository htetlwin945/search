/*
 * Crown copyright
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimpleBNGGeometryTransformerTest {

  private final SimpleBNGGeometryTransformer transformer = new SimpleBNGGeometryTransformer();

  @Test
  void testTransformWgsPointToBng() {
    // A point in the UK (WGS84 lat/lon) should transform to a British National Grid point.
    final String result = transformer.toBNGWKT("POINT (51.5074 -0.1278)");

    assertTrue(result.startsWith("POINT"), "Transformed geometry should remain a point, but was: " + result);
  }

  @Test
  void testTransformWgsPolygonToBng() {
    final String result = transformer.toBNGWKT("POLYGON ((51.5 -0.13, 51.6 -0.13, 51.6 -0.10, 51.5 -0.10, 51.5 -0.13))");

    assertTrue(result.startsWith("POLYGON"), "Transformed geometry should remain a polygon, but was: " + result);
  }

  @Test
  void testThrowOnInvalidWkt() {
    assertThrows(InterpretationRuntimeException.class, () -> transformer.toBNGWKT("not a geometry"),
        "Invalid WKT should raise an InterpretationRuntimeException");
  }
}
