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
package nl.aerius.search.tasks.receptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Single;

import nl.aerius.search.domain.SearchSuggestion;
import nl.aerius.search.domain.SearchSuggestionType;
import nl.aerius.search.domain.SearchTaskResult;

class BNGReceptorSearchServiceTest {
  private BNGReceptorSearchService service;

  @BeforeEach
  void beforeEach() {
    service = new BNGReceptorSearchService();
  }

  @Test
  void testReceptorNonNull() {
    final Single<SearchTaskResult> single = service.retrieveSearchResults("123123");
    final SearchTaskResult result = single.blockingGet();

    assertEquals(1, result.getSuggestions().size(), "Result number should be 1");
    final SearchSuggestion suggestion = result.getSuggestions().get(0);
    assertEquals(SearchSuggestionType.RECEPTOR, suggestion.getType(), "Suggestion type should be receptor");
    assertEquals("Receptor 123123 - x:644445 y:11307", suggestion.getDescription(), "Description should match the receptor");
    assertEquals("POINT(644445.465822343 11307.075536400083)", suggestion.getCentroid(), "Centroid should match the receptor location");
    assertEquals("POLYGON((644508.0 11415.0,644570.0 11307.0,644508.0 11200.0,644383.0 11200.0,644321.0 11307.0,644383.0 11415.0,644508.0 11415.0))",
        suggestion.getGeometry(), "Geometry should be the receptor hexagon");
    assertNull(suggestion.getBbox(), "Receptor suggestion should have no bbox");
    assertEquals(100.0, suggestion.getScore(), "Receptor match should score 100");
  }

  @Test
  void testReceptorNull() {
    final Single<SearchTaskResult> single = service.retrieveSearchResults("nothing");
    final SearchTaskResult result = single.blockingGet();

    assertEquals(0, result.getSuggestions().size(), "Result number should be 0");
  }
}
