package eu.erasmuswithoutpaper.registry.internet;

import java.util.TreeMap;

/**
 * A special Map implementation, good for storing headers (case-insensitive keys, etc.).
 */
public class HeaderMap extends TreeMap<String, String> {
  private static final long serialVersionUID = 8754027427076578401L;

  /**
   * Create an empty map.
   */
  public HeaderMap() {
    super(String.CASE_INSENSITIVE_ORDER);
  }

  /**
   * Construct a new {@link HeaderMap} with keys and values copied from the other map.
   *
   * @param headers The map whose mappings are to be placed in this map.
   */
  public HeaderMap(HeaderMap headers) {
    super(headers);
  }

}
