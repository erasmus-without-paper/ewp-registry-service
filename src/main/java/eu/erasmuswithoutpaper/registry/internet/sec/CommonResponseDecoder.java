package eu.erasmuswithoutpaper.registry.internet.sec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * A helper class. It includes a couple of methods which are common for all types of
 * {@link ResponseCodingDecoder}s.
 */
public abstract class CommonResponseDecoder implements ResponseCodingDecoder {

  /**
   * Get the "outermost" Content-Encoding.
   *
   * @param response The response to peek at.
   * @return The identifier of the "outermost" of the Content-Encodings of this request, or
   *         <code>null</code> if the request has no encodings left.
   */
  protected String peekContentEncoding(Response response) {
    String value = response.getHeader("Content-Encoding");
    if (value == null) {
      return null;
    }
    String[] items = value.split(", *");
    return items[items.length - 1];
  }

  /**
   * Remove the "outermost" Content-Encoding.
   *
   * @param response The request to remove the Content-Encoding from. It MUST have at least one
   *        Content-Encoding left.
   */
  protected void popContentEncoding(Response response) {
    ArrayList<String> items =
        new ArrayList<>(Arrays.asList(response.getHeader("Content-Encoding").split(", *")));
    items.remove(items.size() - 1);
    response.putHeader("Content-Encoding", items.stream().collect(Collectors.joining(", ")));
  }

  /**
   * Take the "outermost" Content-Encoding, verify that it matches what we expect, <b>and remove
   * it</b>.
   *
   * @param response The request to process.
   * @param expectedCoding The value of the coding we expect to find at the outermost
   *        Content-Encoding.
   * @throws InvalidResponseError If the request's outermost coding didn't match what we expect.
   */
  protected void popContentEncodingAndExpect(Response response, String expectedCoding)
      throws InvalidResponseError {
    String actualCoding = this.peekContentEncoding(response);
    if (actualCoding == null) {
      throw new InvalidResponseError(
          "Expecting Content-Encoding to be " + expectedCoding + ", but no encoding found.");
    }
    if (!actualCoding.equalsIgnoreCase(expectedCoding)) {
      throw new InvalidResponseError("Expecting Content-Encoding to be " + expectedCoding + ", but "
          + actualCoding + " found instead.");
    }
    this.popContentEncoding(response);
  }


}
