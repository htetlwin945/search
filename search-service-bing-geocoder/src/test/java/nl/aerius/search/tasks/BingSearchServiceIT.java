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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import io.reactivex.rxjava3.core.Single;

import nl.aerius.search.domain.SearchTaskResult;

@SpringBootTest
class BingSearchServiceIT {
  @Autowired BingSearchService delegator;

  @Value("${nl.aerius.bing.apiKey:#{null}}") private String apiKey;

  @Test
  void testWorksAtAll() {
    // Don't do anything if there is no apiKey
    assumeFalse(apiKey == null, "No Bing API key available, skipping test");

    final Single<SearchTaskResult> result = delegator.retrieveSearchResults("edin");

    final SearchTaskResult suggestions = result.blockingGet();

    assertEquals(5, suggestions.getSuggestions().size(), "Expected number of results for 'edin' (should include 'edinburgh')");

    final Single<SearchTaskResult> resultYork = delegator.retrieveSearchResults("york");

    final SearchTaskResult suggestionsYork = resultYork.blockingGet();

    assertEquals(4, suggestionsYork.getSuggestions().size(), "Expected number of results for 'york'");
  }
}
