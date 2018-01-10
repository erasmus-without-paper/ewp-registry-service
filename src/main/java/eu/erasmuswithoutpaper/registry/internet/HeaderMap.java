package eu.erasmuswithoutpaper.registry.internet;

import java.util.TreeMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A special Map implementation, good for storing headers (case-insensitive keys, etc.).
 */
@SuppressWarnings("serial")
@SuppressFBWarnings("SE_NO_SERIALVERSIONID")
public class HeaderMap extends TreeMap<String, String> {

  /**
   * Create an empty map.
   */
  public HeaderMap() {
    super(String.CASE_INSENSITIVE_ORDER);
  }

}
