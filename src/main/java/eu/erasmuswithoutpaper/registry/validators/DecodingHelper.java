package eu.erasmuswithoutpaper.registry.validators;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.InvalidResponseError;
import eu.erasmuswithoutpaper.registry.internet.sec.ResponseCodingDecoder;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;

import com.google.common.collect.Lists;

public class DecodingHelper {

  private final Map<String, ResponseCodingDecoder> decoders;
  private Set<String> requiredCodings = new HashSet<>();
  private Set<String> acceptableCodings = new HashSet<>();

  public DecodingHelper() {
    this.decoders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  private ResponseCodingDecoder getDecoder(String coding) throws InvalidResponseError {
    if (!this.decoders.containsKey(coding)) {
      throw new InvalidResponseError("Unsupported Content-Encoding: " + coding);
    }
    return this.decoders.get(coding);
  }

  private List<String> getOrderedCodings(Response response) {
    return Lists.reverse(Utils.commaSeparatedTokens(response.getHeader("Content-Encoding")));
  }

  void addDecoder(ResponseCodingDecoder decoder) {
    if (this.decoders.containsKey(decoder.getContentEncoding())) {
      throw new RuntimeException(
          "Expecting unique Content-Encoding handlers, but this one is repeated: "
              + decoder.getContentEncoding());
    }
    this.decoders.put(decoder.getContentEncoding(), decoder);
  }

  /**
   * Decodes a response.
   * @param step Validation step related to this operation.
   * @param response Response to decode.
   * @throws Failure When response cannot be decoded.
   */
  public void decode(InlineValidationStep step, Response response) throws Failure {
    Set<String> unsatisfied = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    unsatisfied.addAll(this.requiredCodings);
    for (String coding : this.getOrderedCodings(response)) {
      try {
        ResponseCodingDecoder decoder = this.getDecoder(coding);
        decoder.decode(response);
      } catch (InvalidResponseError e) {
        throw new Failure(e.getMessage(), Status.FAILURE, response);
      }
      step.addResponseSnapshot(response);
      unsatisfied.remove(coding);
      if (!this.acceptableCodings.contains(coding)) {
        throw new Failure("The response was (successfully) encoded with the '" + coding
            + "' coding, but the client didn't declare this encoding as acceptable "
            + "(it wasn't allowed in the Accept-Encoding header).", Status.FAILURE, response);
      }
    }
    if (this.getOrderedCodings(response).size() > 0) {
      throw new RuntimeException("One of the decoders failed to pop its own coding from "
          + "response's Content-Encoding header.");
    }
    if (unsatisfied.size() > 0) {
      throw new Failure(
          "Expecting the response to be encoded with "
              + unsatisfied.stream().collect(Collectors.joining(" and ")),
          Status.FAILURE, response);
    }
  }

  public void setAcceptEncodingHeader(String acceptEncodingHeader) {
    this.acceptableCodings = Utils.extractAcceptableCodings(acceptEncodingHeader);
  }

  public void setRequiredCodings(Collection<String> requiredCodings) {
    this.requiredCodings = new HashSet<>(requiredCodings);
  }
}
