package eu.erasmuswithoutpaper.registry.internet.sec;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;

import com.google.common.collect.Lists;

/**
 * This {@link RequestDecoder} keeps a map of a set of {@link RequestCodingDecoder}s (each of which
 * decodes a single Content-Encoding), and attempt to map the request's Content-Encoding header to
 * each of those.
 */
public class MultipleCodingsRequestDecoder implements RequestDecoder {

  private final Map<String, RequestCodingDecoder> decoders;

  /**
   * @param decoders The list of decoders which {@link MultipleCodingsRequestDecoder} should try
   *        decoding with. The list MUST NOT contain conflicting implementations - a single
   *        Content-Encoding must be decoded by a single decoder only.
   */
  public MultipleCodingsRequestDecoder(List<RequestCodingDecoder> decoders) {
    this.decoders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (RequestCodingDecoder decoder : decoders) {
      if (this.decoders.containsKey(decoder.getContentEncoding())) {
        throw new RuntimeException("Repeated coding: " + decoder.getContentEncoding());
      }
      this.decoders.put(decoder.getContentEncoding(), decoder);
    }
  }

  @Override
  public void decode(Request request) throws Http4xx {
    List<String> codings = this.getReversedContentEncodings(request);
    for (String coding : codings) {
      RequestCodingDecoder decoder = this.decoders.get(coding);
      if (decoder == null) {
        throw new Http4xx(415,
            "Could decode your request. Unsupported Content-Encoding: " + coding);
      }
      decoder.decode(request);
    }
    // Sanity check
    codings = this.getReversedContentEncodings(request);
    if (codings.size() > 0) {
      throw new RuntimeException("One of the decoders didn't pop its own coding "
          + "from the request's Content-Encoding header.");
    }
  }

  /**
   * Get the list of Content-Encodings from the request, in the order in which they should be
   * decoded.
   *
   * @param request The request to parse.
   * @return The list of tokens extracted from the request's Content-Encoding header (it's a
   *         reversed list, the last used Content-Encoding will be the first returned).
   */
  protected List<String> getReversedContentEncodings(Request request) {
    return Lists.reverse(Utils.commaSeparatedTokens(request.getHeader("Content-Encoding")));
  }
}
