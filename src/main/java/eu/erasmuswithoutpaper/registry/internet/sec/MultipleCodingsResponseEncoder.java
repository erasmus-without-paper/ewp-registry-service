package eu.erasmuswithoutpaper.registry.internet.sec;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;


/**
 * This {@link ResponseEncoder} keeps a list of {@link ResponseCodingEncoder}s (each of which
 * encodes a single Content-Encoding), and attempts to encode the request with all of them, as long
 * as the clients supports them.
 */
public class MultipleCodingsResponseEncoder implements ResponseEncoder {

  private final List<ResponseCodingEncoder> encodersList;
  private final Map<String, ResponseCodingEncoder> encodersMap;
  private final Collection<ResponseCodingEncoder> requiredEncoders;

  /**
   * @param encoders The list of encoders to try encoding with. The order is significant. If you
   *        want the response to be first encoded by encoder X, and later by encoder Y, then X must
   *        precede Y on this list.
   * @param requiredEncoders A collection of encoders which the client is required to support. The
   *        {@link #encode(Request, Response)} method will verify if these encoders are indeed used
   *        - if the client doesn't support them (i.e. it doesn't list them in the request's
   *        Accept-Encoding header), then {@link #encode(Request, Response)} will throw a
   *        {@link Http4xx} exception. All encoders in this set MUST also be present on the list of
   *        encoders provided in the previous argument
   */
  public MultipleCodingsResponseEncoder(List<ResponseCodingEncoder> encoders,
      Collection<ResponseCodingEncoder> requiredEncoders) {
    this.encodersList = encoders;
    this.encodersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    this.requiredEncoders = requiredEncoders;
    for (ResponseCodingEncoder encoder : encoders) {
      if (this.encodersMap.containsKey(encoder.getContentEncoding())) {
        throw new RuntimeException("Repeated coding: " + encoder.getContentEncoding());
      }
      this.encodersMap.put(encoder.getContentEncoding(), encoder);
    }
  }

  @Override
  public void encode(Request request, Response response) throws Http4xx {
    Set<String> acceptedCodings = this.getAcceptedResponseEncodings(request);
    this.verifyIfMatchesRequiredCodings(acceptedCodings);
    boolean nonIdentityMatched = false;
    for (ResponseCodingEncoder encoder : this.encodersList) {
      if (acceptedCodings.contains(encoder.getContentEncoding())) {
        try {
          encoder.encode(request, response);
          if (!encoder.getContentEncoding().equals("identity")) {
            nonIdentityMatched = true;
          }
        } catch (Http4xx e) {
          if (this.requiredEncoders.contains(encoder)) {
            throw e;
          } else {
            /*
             * Ignore. Something went wrong, but we are not required to encode with this encoder.
             * It's probably better to not encode it, that to return an error response (BTW, it's
             * quite possible that the response we are encoding already contains a different error
             * response).
             */
          }
        }
      }
    }
    if ((!nonIdentityMatched) && (!acceptedCodings.contains("identity"))) {
      throw new Http4xx(406,
          "The 'identity' Content-Encoding was explicitly forbidden, "
              + "but we are unable to generate content in any other encoding. Allow identity "
              + "to see the proper error message.");
    }
  }

  /**
   * Retrieve the list of response content codings accepted by the client.
   *
   * @param request The client's request.
   * @return The list of response content codings which the client will accept. This should
   *         explicitly contain "identity", if the client will accept it.
   */
  protected Set<String> getAcceptedResponseEncodings(Request request) {
    return Utils.extractAcceptableCodings(request.getHeader("Accept-Encoding"));
  }

  /**
   * Verify if the given list of codings contains all the codings which this encoder requires all
   * clients to support.
   *
   * @param codings The list of codings (usually taken from the request's Accept-Encoding header).
   * @throws Http4xx If the list does not contain at least one of the codings, which this encoder
   *         requires all clients to support.
   */
  protected void verifyIfMatchesRequiredCodings(Set<String> codings) throws Http4xx {
    for (ResponseCodingEncoder encoder : this.requiredEncoders) {
      String coding = encoder.getContentEncoding();
      if (!codings.contains(coding)) {
        throw new Http4xx(400,
            "This endpoint requires all requests to accept the " + coding + " Content-Encoding.");
      }
    }
  }
}

