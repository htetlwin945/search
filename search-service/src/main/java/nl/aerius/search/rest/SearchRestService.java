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
package nl.aerius.search.rest;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nl.aerius.search.domain.SearchSuggestion;
import nl.aerius.search.tasks.SearchTaskDelegator;
import nl.aerius.search.tasks.TaskUtils;
import nl.aerius.search.tasks.async.SearchResult;

@CrossOrigin
@RestController
public class SearchRestService {
  private static final String DEFAULT_CAPABILITIES = "MOCK_0,MOCK_01,MOCK_05,RECEPTOR";
  private static final String DEFAULT_REGION = "NL";
  private static final Pattern SCRUB_PATTERN = Pattern.compile("[\n\r\t]");

  @Autowired SearchTaskDelegator taskDelegator;

  /**
   * Retrieve search results based on the given query and capabilities
   *
   * Accept both GET and POST requests
   */
  @RequestMapping(value = "/api/search", method = { RequestMethod.GET, RequestMethod.POST })
  public List<SearchSuggestion> retrieveSearchResults(final String query,
      @RequestParam(defaultValue = DEFAULT_CAPABILITIES) final List<String> capabilities,
      @RequestParam(defaultValue = DEFAULT_REGION) final String region) {
    return taskDelegator.retrieveSearchResults(scrub(query), TaskUtils.parseCapabilities(capabilities, region));
  }

  /**
   * Retrieve search results based on the given query and capabilities
   * asynchronously
   *
   * Accept both GET and POST requests
   */
  @RequestMapping(value = "/api/search-async", method = { RequestMethod.GET, RequestMethod.POST })
  public SearchResult retrieveSearchResultsAsync(final String query,
      @RequestParam(defaultValue = DEFAULT_CAPABILITIES) final List<String> capabilities,
      @RequestParam(defaultValue = DEFAULT_REGION) final String region, @RequestParam(required = false) final String cancel) {
    if (cancel != null) {
      taskDelegator.cancelSearchTask(cancel);
    }

    return taskDelegator.retrieveSearchResultsAsync(scrub(query), TaskUtils.parseCapabilities(capabilities, region));
  }

  /**
   * Remove newlines, carriage returns, and tabs
   */
  private static String scrub(final String query) {
    return SCRUB_PATTERN.matcher(query).replaceAll("");
  }

  @GetMapping(value = "/api/results/{uuid}")
  public SearchResult retrieveSearchResultsAsync(final @PathVariable("uuid") String uuid) {
    return taskDelegator.retrieveSearchTask(uuid);
  }
}
