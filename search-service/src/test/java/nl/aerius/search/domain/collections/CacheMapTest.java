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
package nl.aerius.search.domain.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class CacheMapTest {

  // A very long sweep interval so the background scheduler never interferes; sweeping is exercised directly.
  private static final int NEVER_SWEEP = 3600;

  @Test
  void testStoreAndRetrieveValues() {
    final CacheMap<String, String> cache = new CacheMap<>(NEVER_SWEEP, NEVER_SWEEP, traceLogger());

    cache.put("a", "1");
    cache.put("b", "2");

    assertEquals(2, cache.size(), "Both entries should be stored");
    assertEquals("1", cache.get("a"), "Value for a should be retrievable");
    assertTrue(cache.containsKey("b"), "Key b should be present");
  }

  @Test
  void testRemoveExpiredEntriesOnSweep() throws Exception {
    // Time to live of 0 means every entry is immediately considered expired.
    final CacheMap<String, String> cache = new CacheMap<>(0, NEVER_SWEEP, traceLogger());
    cache.put("a", "1");
    cache.put("b", "2");

    invokeSweep(cache);

    assertEquals(0, cache.size(), "Expired entries should be removed on sweep");
    assertFalse(cache.containsKey("a"), "Expired key should no longer be present");
  }

  @Test
  void testKeepLivingEntriesOnSweep() throws Exception {
    // Long time to live: nothing is old enough to be removed, so the sweep should break out early.
    final CacheMap<String, String> cache = new CacheMap<>(NEVER_SWEEP, NEVER_SWEEP, debugLogger());
    cache.put("a", "1");

    invokeSweep(cache);

    assertEquals(1, cache.size(), "Living entries should be retained on sweep");
  }

  @Test
  void testConstructWithDefaults() {
    assertEquals(0, new CacheMap<>().size(), "Default constructed cache should start empty");
    assertEquals(0, new CacheMap<>(traceLogger()).size(), "Logger constructed cache should start empty");
  }

  private static void invokeSweep(final CacheMap<?, ?> cache) throws Exception {
    final Method doSweep = CacheMap.class.getDeclaredMethod("doSweep");
    doSweep.setAccessible(true);
    doSweep.invoke(cache);
  }

  private static Logger traceLogger() {
    final Logger logger = mock(Logger.class);
    when(logger.isTraceEnabled()).thenReturn(true);
    return logger;
  }

  private static Logger debugLogger() {
    final Logger logger = mock(Logger.class);
    when(logger.isDebugEnabled()).thenReturn(true);
    return logger;
  }
}
