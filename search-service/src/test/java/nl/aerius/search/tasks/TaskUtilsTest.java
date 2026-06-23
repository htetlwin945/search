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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import nl.aerius.search.domain.SearchCapability;
import nl.aerius.search.domain.SearchRegion;
import nl.aerius.search.domain.SearchSuggestion;
import nl.aerius.search.domain.SearchSuggestionBuilder;

class TaskUtilsTest {

  @Test
  void testParseKnownCapabilities() {
    final Set<CapabilityKey> keys = TaskUtils.parseCapabilities(List.of("RECEPTOR", "COORDINATE"), "NL");

    assertEquals(2, keys.size(), "Both known capabilities should be parsed");
    assertTrue(keys.contains(CapabilityKey.of(SearchCapability.RECEPTOR, SearchRegion.NL)), "Receptor capability should be present");
    assertTrue(keys.contains(CapabilityKey.of(SearchCapability.COORDINATE, SearchRegion.NL)), "Coordinate capability should be present");
  }

  @Test
  void testReturnEmptyForUnknownRegion() {
    final Set<CapabilityKey> keys = TaskUtils.parseCapabilities(List.of("RECEPTOR"), "ATLANTIS");

    assertTrue(keys.isEmpty(), "An unknown region should yield no capabilities");
  }

  @Test
  void testStillKeyUnknownCapability() {
    // An unknown capability resolves to a null capability but a valid region, so a key is still produced.
    final Set<CapabilityKey> keys = TaskUtils.parseCapabilities(List.of("DOES_NOT_EXIST"), "NL");

    assertEquals(1, keys.size(), "An unknown capability with a valid region still yields a key");
  }

  @Test
  void testOrderSuggestionsByScoreDescending() {
    final SearchSuggestion low = SearchSuggestionBuilder.create("low", 10D);
    final SearchSuggestion high = SearchSuggestionBuilder.create("high", 90D);

    final List<SearchSuggestion> sorted = List.of(low, high).stream()
        .sorted(TaskUtils.getResultComparator())
        .toList();

    assertEquals("high", sorted.get(0).getDescription(), "Higher scoring suggestion should sort first");
    assertEquals("low", sorted.get(1).getDescription(), "Lower scoring suggestion should sort last");
  }
}
