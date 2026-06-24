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
package nl.aerius.search.wui.component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.axellience.vuegwt.core.annotations.component.Component;
import com.axellience.vuegwt.core.annotations.component.Computed;
import com.axellience.vuegwt.core.annotations.component.Data;
import com.axellience.vuegwt.core.annotations.component.Emit;
import com.axellience.vuegwt.core.annotations.component.Prop;
import com.axellience.vuegwt.core.annotations.component.PropDefault;
import com.axellience.vuegwt.core.annotations.component.Ref;
import com.axellience.vuegwt.core.annotations.component.Watch;
import com.axellience.vuegwt.core.client.Vue;
import com.axellience.vuegwt.core.client.component.IsVueComponent;
import com.axellience.vuegwt.core.client.component.hooks.HasCreated;
import com.axellience.vuegwt.core.client.component.hooks.HasDestroyed;
import com.axellience.vuegwt.core.client.component.hooks.HasMounted;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;

import elemental2.dom.ClientRect;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;

import jsinterop.annotations.JsMethod;

import nl.aerius.search.wui.command.SearchTextCommand;
import nl.aerius.search.wui.context.SearchContext;
import nl.aerius.search.wui.domain.SearchSuggestion;
import nl.aerius.search.wui.event.SearchSuggestionSelectionEvent;
import nl.aerius.search.wui.i18n.SearchM;
import nl.aerius.search.wui.i18n.SearchMessages;
import nl.aerius.wui.vue.transition.HorizontalCollapse;
import nl.aerius.wui.vue.transition.VerticalCollapse;
import nl.aerius.wui.vue.transition.VerticalCollapseGroup;

@Component(components = {
    VerticalCollapse.class,
    VerticalCollapseGroup.class,
    HorizontalCollapse.class
})
public class MapSearchComponent implements IsVueComponent, HasCreated, HasMounted, HasDestroyed {
  private static final String BOLD_CHARACTER = "b";

  private static final String FALLBACK_SEARCH_TYPE = "TEXT";

  @Prop boolean auto;

  @Prop EventBus eventBus;

  @Data SearchMessages i18n = SearchM.messages();

  @Data String maxHeight;

  @Ref HTMLElement resultsContainer;
  @Ref HTMLElement input;

  @Data String search;

  @Inject @Data SearchContext context;

  private HandlerRegistration resizeHandler;

  @Computed
  public Map<String, List<SearchSuggestion>> getResults() {
    final Map<String, List<SearchSuggestion>> res = context.getResults().values().stream()
        .collect(Collectors.groupingBy(v -> v.type, LinkedHashMap::new, Collectors.toList()));

    res.values().forEach(v -> v.sort((o1, o2) -> -Double.compare(o1.score, o2.score)));

    return res;
  }

  @JsMethod
  public String highlight(final String plain) {
    String result = plain;
    if (hasQuery()) {
      final String[] split = search.split(" ");
      for (int i = 0; i < split.length; i++) {
        final String sample = split[i];
        if (sample.isEmpty()
            || BOLD_CHARACTER.equalsIgnoreCase(sample)) {
          continue;
        }

        result = boldenText(result, sample);
      }
    }
    return result;
  }

  /**
   * Escape statement taken from https://stackoverflow.com/a/6969486
   */
  public static native String boldenText(String txt, String query) /*-{
    var escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    var reg = new RegExp('(' + escapedQuery + ')', 'gi');
    return txt.replace(reg, '<b>$1</b>');
  }-*/;

  @JsMethod
  public String getAriaLabel(final SearchSuggestion sug, final String group, final int idx) {
    return group + " result " + (idx + 1) + ":" + sug.description;
  }

  @Computed("hasQuery")
  public boolean hasQuery() {
    return search != null && !search.isEmpty();
  }

  @PropDefault("auto")
  boolean autoDefault() {
    return true;
  }

  @JsMethod
  @SuppressWarnings("rawtypes")
  public boolean isEmpty(final Object val) {
    return ((Collection) val).isEmpty();
  }

  @JsMethod
  @SuppressWarnings("rawtypes")
  public String getEntryGroup(final Entry entry) {
    return i18n.searchSuggestionTypeName(Optional.ofNullable(entry.getKey())
        .map(String::valueOf)
        .orElse(FALLBACK_SEARCH_TYPE));
  }

  @Computed
  public boolean isSearching() {
    return context.isSearching();
  }

  @JsMethod
  public boolean hasResults() {
    return !context.getResults().isEmpty();
  }

  @JsMethod
  @Emit
  public void selectSuggestion(final SearchSuggestion value) {
    if (auto) {
      eventBus.fireEvent(new SearchSuggestionSelectionEvent(value));
    }
  }

  @Computed("isShowing")
  public boolean isShowing() {
    return context.isSearching()
        || hasResults();
  }

  @Watch("isShowing()")
  public void onShowingChange() {
    resize();
  }

  @Override
  public void created() {
    DomGlobal.window.addEventListener("resize", null);

    resizeHandler = Window.addResizeHandler(r -> resize());
  }

  @Override
  public void mounted() {
    resize();
  }

  private void resize() {
    final ClientRect rect = resultsContainer.getBoundingClientRect();
    maxHeight = "calc(100vh - " + rect.top + "px - 10px)";
  }

  @Override
  public void destroyed() {
    resizeHandler.removeHandler();
  }

  @Watch("search")
  public void onSearchChange(final String neww) {
    eventBus.fireEvent(new SearchTextCommand(neww));
  }

  public void focus() {
    // Delay setting focus by a tick because we are (probably) still in a click
    // handler (which takes focus)
    Vue.nextTick(() -> input.focus());
  }
}
