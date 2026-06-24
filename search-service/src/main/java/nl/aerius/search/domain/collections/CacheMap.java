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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheMap<K, V> extends HashMap<K, V> {
  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(CacheMap.class);

  private static final int MILLISECONDS_IN_SECONDS = 1000;

  /**
   * Interval between each sweep. 30 seconds by default.
   */
  private static final int DEFAULT_INTERVAL = 30;

  /**
   * 1 hour is 60 times 60 seconds, most of the time.
   */
  private static final int DEFAULT_TIME_TO_LIVE = 60 * 60;

  private class Registration {
    private final Long time;
    private final K key;

    public Registration(final Long time, final K key) {
      super();
      this.time = time;
      this.key = key;
    }

    public Long getTime() {
      return time;
    }

    public K getKey() {
      return key;
    }
  }

  private final transient ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private final ArrayList<Registration> timeRegistry = new ArrayList<>();

  /**
   * Number of seconds to let elements in this Map live.
   */
  private final int timeToLive;

  /**
   * The logger to log to - can be overridden, which is useful to pin down whoever
   * owns this cachemap
   */
  private transient Logger logger;

  public CacheMap() {
    this(DEFAULT_TIME_TO_LIVE, DEFAULT_INTERVAL, LOG);
  }

  public CacheMap(final Logger logger) {
    this(DEFAULT_TIME_TO_LIVE, DEFAULT_INTERVAL, logger);
  }

  /**
   * @param timeToLive in seconds
   * @param interval   in seconds
   */
  public CacheMap(final int timeToLive, final int interval, final Logger logger) {
    this.timeToLive = timeToLive;
    this.logger = logger;

    scheduler.scheduleAtFixedRate(this::doSweep, interval, interval, TimeUnit.SECONDS);
  }

  @Override
  public V put(final K key, final V value) {
    if (logger.isTraceEnabled()) {
      logger.trace("Adding key {} to map.", key);
    }

    timeRegistry.add(new Registration(System.currentTimeMillis(), key));

    return super.put(key, value);
  }

  private void doSweep() {
    final long now = System.currentTimeMillis();
    final int startSize = size();

    // Mark time before which all records should be removed
    final long mark = now - timeToLive * MILLISECONDS_IN_SECONDS;

    // Iterate over the time register, which is assumed to be (at least roughly)
    // time-ordered naturally
    final Iterator<Registration> it = timeRegistry.iterator();
    while (it.hasNext()) {
      final Registration reg = it.next();

      // Instead of iterating over everything, take advantage of chronological
      // insertion being roughly sequential in the nominal case and break out
      // when the mark is exceeded - if by some miracle entries are not _exactly_
      // inserted in a sequentially chronological fashion, which is possible,
      // then the next sweep will catch them
      if (reg.getTime() > mark) {
        break;
      }

      if (logger.isTraceEnabled()) {
        logger.trace("Removing key {} from map. Added {} seconds ago.", reg.getKey(), (now - reg.getTime()) / MILLISECONDS_IN_SECONDS);
      }

      // Remove the key out of the underlying map and the time register
      remove(reg.getKey());
      it.remove();
    }

    if (logger.isTraceEnabled() || logger.isDebugEnabled()) {
      final int endSize = size();
      final long current = System.currentTimeMillis();

      // Only log to debug if size differs, otherwise log to trace
      if (logger.isDebugEnabled()) {
        if (endSize != startSize) {
          logger.debug("Performing cache sweep at {}. Removed {} entries in {}ms.", now / MILLISECONDS_IN_SECONDS, startSize - endSize,
              current - now);
        }
      } else {
        logger.trace("Performing cache sweep at {}. Removed {} entries in {}ms.", now / MILLISECONDS_IN_SECONDS, startSize - endSize, current - now);
      }
    }
  }
}
