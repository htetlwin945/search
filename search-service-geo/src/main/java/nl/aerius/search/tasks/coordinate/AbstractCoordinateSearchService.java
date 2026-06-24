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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Single;

import nl.aerius.search.domain.SearchResultBuilder;
import nl.aerius.search.domain.SearchSuggestion;
import nl.aerius.search.domain.SearchSuggestionBuilder;
import nl.aerius.search.domain.SearchSuggestionType;
import nl.aerius.search.domain.SearchTaskResult;
import nl.aerius.search.tasks.ReceptorUtils;
import nl.aerius.search.tasks.SearchTaskService;
import nl.overheid.aerius.shared.domain.geo.HexagonZoomLevel;
import nl.overheid.aerius.shared.domain.geo.ReceptorGridSettings;
import nl.overheid.aerius.shared.domain.v2.geojson.Point;
import nl.overheid.aerius.shared.geometry.ReceptorUtil;

public abstract class AbstractCoordinateSearchService implements SearchTaskService {
  private static final String COORDINATE_FORMAT = "x:%d y:%d";
  private static final String WKT_POINT_FORMAT = "POINT(%d %d)";

  // Regex used to identify a x:{x} y:{y} string
  private static final Pattern SEARCH_TERM_COORDINATE_REGEX = Pattern
      .compile("(x:)?([0-9]{1,6}\\.?[0-9]{1,6})\\s*[\\s,]\\s*(y:)?([0-9]{1,6}\\.?[0-9]{1,6})", Pattern.CASE_INSENSITIVE);

  // The relevant groups in the above regular expression that identify the X and Y
  // coordinates respectively.
  private static final int SEARCH_TERM_COORDINATE_REGEX_GROUP_X = 2;
  private static final int SEARCH_TERM_COORDINATE_REGEX_GROUP_Y = 4;

  private final ReceptorUtil util;
  private final HexagonZoomLevel zoomLevel1;

  protected AbstractCoordinateSearchService(final ReceptorGridSettings settings) {
    this.util = new ReceptorUtil(settings);
    this.zoomLevel1 = settings.getZoomLevel1();
  }

  @Override
  public Single<SearchTaskResult> retrieveSearchResults(final String query) {
    final Matcher coordinateMatch = SEARCH_TERM_COORDINATE_REGEX.matcher(query);

    if (coordinateMatch.matches()) {
      final int x = (int) Math.round(Double.parseDouble(coordinateMatch.group(SEARCH_TERM_COORDINATE_REGEX_GROUP_X)));
      final int y = (int) Math.round(Double.parseDouble(coordinateMatch.group(SEARCH_TERM_COORDINATE_REGEX_GROUP_Y)));

      final Point point = new Point(x, y);
      final int recId = util.getReceptorIdFromPoint(point);
      return Single.just(SearchResultBuilder.of(
          SearchSuggestionBuilder.create(String.format(COORDINATE_FORMAT, x, y), SearchSuggestion.MAX_SCORE, SearchSuggestionType.COORDINATE,
              String.format(WKT_POINT_FORMAT, x, y)),
          ReceptorUtils.getReceptorSuggestion(recId, util, zoomLevel1)));
    }

    return Single.just(SearchResultBuilder.empty());
  }
}
