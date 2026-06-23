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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.aerius.search.domain.SearchTaskResult;

/**
 * Unit test for the no-api-key path, which can run without a remote Bing service (unlike the integration test in
 * {@link BingSearchServiceIT}, which is skipped when no api key is configured).
 */
class BingSearchServiceTest {

  @Test
  void testReturnEmptyResultWithoutApiKey() {
    final BingSearchService service = new BingSearchService();

    final SearchTaskResult result = service.retrieveSearchResults("anything").blockingGet();

    assertNotNull(result, "A result should always be returned");
    assertTrue(result.getSuggestions().isEmpty(), "Without an api key there should be no suggestions");
  }
}
