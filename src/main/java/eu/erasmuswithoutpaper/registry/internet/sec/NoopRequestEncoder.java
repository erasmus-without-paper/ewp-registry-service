package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * This encoder does nothing.
 */
public class NoopRequestEncoder implements RequestEncoder {

  @Override
  public void encode(Request request) {
  }

}
