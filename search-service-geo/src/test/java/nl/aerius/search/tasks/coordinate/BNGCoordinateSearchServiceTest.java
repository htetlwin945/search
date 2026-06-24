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
package nl.aerius.search.tasks.coordinate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Single;

import nl.aerius.search.domain.SearchSuggestion;
import nl.aerius.search.domain.SearchSuggestionType;
import nl.aerius.search.domain.SearchTaskResult;

class BNGCoordinateSearchServiceTest {
  private BNGCoordinateSearchService service;

  @BeforeEach
  void beforeEach() {
    service = new BNGCoordinateSearchService();
  }

  @Test
  void testCoordinateNonNull() {
    final Single<SearchTaskResult> single = service.retrieveSearchResults("x:123123 y:456456");
    final SearchTaskResult result = single.blockingGet();

    assertEquals(2, result.getSuggestions().size(), "Result number should be 2");
    assertEquals(1, result.getSuggestions().stream()
        .map(SearchSuggestion::getType)
        .filter(v -> v == SearchSuggestionType.COORDINATE)
        .count(), "Expecting 1 coordinate result.");
    assertEquals(1, result.getSuggestions().stream()
        .map(SearchSuggestion::getType)
        .filter(v -> v == SearchSuggestionType.RECEPTOR)
        .count(), "Expecting 1 receptor result.");

    final SearchSuggestion coordinateSuggestion = result.getSuggestions().stream()
        .filter(v -> v.getType() == SearchSuggestionType.COORDINATE)
        .findFirst().orElseThrow();
    assertEquals("x:123123 y:456456", coordinateSuggestion.getDescription(), "Coordinate description should match the query");
    assertEquals("POINT(123123 456456)", coordinateSuggestion.getCentroid(), "Coordinate centroid should match the query");
    assertNull(coordinateSuggestion.getGeometry(), "Coordinate suggestion should have no geometry");
    assertNull(coordinateSuggestion.getBbox(), "Coordinate suggestion should have no bbox");
    assertEquals(100.0, coordinateSuggestion.getScore(), "Coordinate match should score 100");

    final SearchSuggestion receptorSuggestion = result.getSuggestions().stream()
        .filter(v -> v.getType() == SearchSuggestionType.RECEPTOR)
        .findFirst().orElseThrow();
    assertEquals("Receptor 7516977 - x:123121 y:456501", receptorSuggestion.getDescription(), "Receptor description should match the nearest receptor");
    assertEquals("POINT(123120.62375334681 456501.39829089347)", receptorSuggestion.getCentroid(), "Receptor centroid should match the nearest receptor");
    assertEquals(
        "POLYGON((123183.0 456609.0,123245.0 456501.0,123183.0 456394.0,123059.0 456394.0,122997.0 456501.0,123059.0 456609.0,123183.0 456609.0))",
        receptorSuggestion.getGeometry(), "Receptor geometry should be the receptor hexagon");
    assertNull(receptorSuggestion.getBbox(), "Receptor suggestion should have no bbox");
    assertEquals(100.0, receptorSuggestion.getScore(), "Receptor match should score 100");
  }

  @Test
  void testCoordinateNull() {
    final Single<SearchTaskResult> single = service.retrieveSearchResults("4277497");
    final SearchTaskResult result = single.blockingGet();

    assertEquals(0, result.getSuggestions().size(), "Result number should be 0: " + result.getSuggestions());
  }
}
