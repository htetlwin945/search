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
package nl.aerius.search.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import nl.aerius.search.domain.SearchSuggestion;
import nl.aerius.search.domain.SearchSuggestionBuilder;
import nl.aerius.search.tasks.sync.BlockingSearchTaskDelegator;

@ExtendWith(MockitoExtension.class)
class SearchViewControllerTest {

  @Mock private BlockingSearchTaskDelegator delegator;
  @InjectMocks private SearchViewController controller;

  @Test
  void testServeSynchronousForm() {
    assertEquals("synchronous-form", controller.searchForm(mock(Model.class)), "Root should serve the synchronous form");
  }

  @Test
  void testServeAsynchronousForm() {
    assertEquals("asynchronous-form", controller.searchFormAsync(mock(Model.class)), "/async should serve the asynchronous form");
  }

  @Test
  void testPopulateModelAndServeResults() {
    final List<SearchSuggestion> results = List.of(SearchSuggestionBuilder.create("amsterdam", 90D));
    when(delegator.retrieveSearchResults(eq("ams"), any())).thenReturn(results);

    final Model model = mock(Model.class);
    final String view = controller.search("ams", List.of("RECEPTOR"), "NL", model);

    assertEquals("synchronous-results", view, "Search should serve the results view");
    verify(delegator).retrieveSearchResults(eq("ams"), any());
    verify(model).addAttribute("query", "ams");
    verify(model).addAttribute("region", "NL");
    verify(model).addAttribute("results", results);
  }
}
