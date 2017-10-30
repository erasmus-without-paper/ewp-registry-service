package eu.erasmuswithoutpaper.registry.repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

/**
 * Terribly simple in-memory key-value cache, which is completely cleared whenever a new (and
 * <i>different</i>) catalogue is generated.
 *
 * <p>
 * We can use this to keep cached values which are dependent <b>solely</b> on the catalogue
 * contents. E.g. the HTML contents of the API/HEI coverage matrix.
 * </p>
 */
@Service
public class CatalogueDependantCache {

  private static final Integer KEY_CMATRIX_HTML = 1;

  private final ConcurrentMap<Integer, Object> cache = new ConcurrentHashMap<>();

  /**
   * @return Cached coverage matrix HTML.
   */
  public String getCoverageMatrixHtml() {
    return (String) this.cache.get(KEY_CMATRIX_HTML);
  }

  /**
   * @param value New coverage matrix HTML.
   */
  public void putCoverageMatrixHtml(String value) {
    this.cache.put(KEY_CMATRIX_HTML, value);
  }

  synchronized void clear() {
    this.cache.clear();
  }
}
