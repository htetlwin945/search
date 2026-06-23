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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.aerius.search.domain.SearchSuggestion;
import nl.aerius.search.domain.SearchSuggestionBuilder;
import nl.aerius.search.tasks.async.AsyncSearchTaskDelegator;
import nl.aerius.search.tasks.async.SearchResult;
import nl.aerius.search.tasks.sync.BlockingSearchTaskDelegator;

@ExtendWith(MockitoExtension.class)
class SearchTaskDelegatorImplTest {

  @Mock private BlockingSearchTaskDelegator blocking;
  @Mock private AsyncSearchTaskDelegator async;
  @InjectMocks private SearchTaskDelegatorImpl delegator;

  @Test
  void testDelegateBlockingSearch() {
    final List<SearchSuggestion> expected = List.of(SearchSuggestionBuilder.create("x", 1D));
    when(blocking.retrieveSearchResults(eq("q"), any())).thenReturn(expected);

    assertEquals(expected, delegator.retrieveSearchResults("q", Set.of()), "Blocking search should be delegated to the blocking delegator");
  }

  @Test
  void testDelegateAsyncSearch() {
    final SearchResult expected = mock(SearchResult.class);
    when(async.retrieveSearchResultsAsync(eq("q"), any())).thenReturn(expected);

    assertSame(expected, delegator.retrieveSearchResultsAsync("q", Set.of()), "Async search should be delegated to the async delegator");
  }

  @Test
  void testDelegateTaskRetrieval() {
    final SearchResult expected = mock(SearchResult.class);
    when(async.retrieveSearchTask("uuid")).thenReturn(expected);

    assertSame(expected, delegator.retrieveSearchTask("uuid"), "Task retrieval should be delegated to the async delegator");
  }

  @Test
  void testDelegateCancellation() {
    delegator.cancelSearchTask("uuid");

    verify(async).cancelSearchTask("uuid");
  }
}
