package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.util.encoders.Base64;
import org.joox.Match;

public class HtmlValidationReportFormatter {
  private final EwpDocBuilder docBuilder;

  public HtmlValidationReportFormatter(EwpDocBuilder docBuilder) {
    this.docBuilder = docBuilder;
  }

  public Map<String, Object> getPebbleContext(
      List<ValidationStepWithStatus> steps,
      ValidationInfoParameters validationInfoParameters) {
    return createValidationReportContext(steps, validationInfoParameters);
  }

  public static class ValidationInfoParameters {
    private final String apiName;
    private final String url;
    private final String version;
    private final HttpSecurityDescription httpSecurityDescription;
    private final Date validationStartedDate;
    private Date clientKeysRegenerationDate;

    /**
     * Basic info about run tests.
     * @param apiName
     *      Name of tested api.
     * @param url
     *      Url of tested api.
     * @param version
     *      Version of tested api.
     * @param validationStartedDate
     *      Date when tests started.
     * @param httpSecurityDescription
     *      Description of used HTTP Security.
     * @param clientKeysRegenerationDate
     *      Date of client keys generation.
     */
    public ValidationInfoParameters(
        String apiName, String url, String version,
        HttpSecurityDescription httpSecurityDescription, Date validationStartedDate,
        Date clientKeysRegenerationDate) {
      this.apiName = apiName;
      this.url = url;
      this.version = version;
      this.httpSecurityDescription = httpSecurityDescription;
      // cloning to prevent EI_EXPOSE_REP2 FindBugs warning.
      this.validationStartedDate = (Date) validationStartedDate.clone();
      this.clientKeysRegenerationDate = null;
      if (clientKeysRegenerationDate != null) {
        this.clientKeysRegenerationDate = (Date) clientKeysRegenerationDate.clone();
      }
    }
  }

  private Map<String, Object> createValidationInfo(
      ValidationInfoParameters validationInfoParameters) {
    Map<String, Object> info = new HashMap<>();
    DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    info.put("validationStarted",
        isoDateFormat.format(validationInfoParameters.validationStartedDate));

    Date clientKeysRegenerated = validationInfoParameters.clientKeysRegenerationDate;
    String clientKeysRegeneratedString = null;
    if (clientKeysRegenerated != null) {
      clientKeysRegeneratedString = isoDateFormat.format(clientKeysRegenerated);
    }
    info.put("clientKeysRegenerated", clientKeysRegeneratedString);

    String clientKeysAgeWhenValidationStartedInSeconds = null;
    if (clientKeysRegenerated != null) {
      long clientKeysAgeWhenValidationStartedInMillis =
          validationInfoParameters.validationStartedDate.getTime()
              - clientKeysRegenerated.getTime();
      clientKeysAgeWhenValidationStartedInSeconds = String.valueOf(
          clientKeysAgeWhenValidationStartedInMillis / 1000
      );
    }

    info.put(
        "clientKeysAgeWhenValidationStartedInSeconds",
        clientKeysAgeWhenValidationStartedInSeconds
    );

    info.put("security", validationInfoParameters.httpSecurityDescription.toString());
    info.put("url", validationInfoParameters.url);
    info.put("apiName", validationInfoParameters.apiName);
    info.put("version", validationInfoParameters.version);

    info.put(
        "securityExplanation",
        createSecurityExplanation(validationInfoParameters.httpSecurityDescription)
    );
    return info;
  }

  private Object createSecurityExplanation(HttpSecurityDescription description) {
    String[] explanations = description.getExplanation().split("\n");
    List<Map<String, String>> splitExplanations = new ArrayList<>();
    for (String explanation : explanations) {
      String[] parts = explanation.split(":", 2);
      Map<String, String> mapParts = new HashMap<>();
      mapParts.put("marker", parts[0]);
      mapParts.put("description", parts[1]);
      splitExplanations.add(mapParts);
    }
    return splitExplanations;
  }

  private Map<String, Object> createValidationReportContext(
      List<ValidationStepWithStatus> testResults,
      ValidationInfoParameters validationInfoParameters
  ) {
    Map<String, Object> validationReportContext = new HashMap<>();
    List<Map<String, Object>> testsArray = testResults.stream()
        .map(this::createTestStepDescription)
        .collect(Collectors.toList());

    ValidationStepWithStatus.Status worstStatus =
        testResults.stream().map(ValidationStepWithStatus::getStatus).max(
            ValidationStepWithStatus.Status::compareTo)
            .orElse(ValidationStepWithStatus.Status.SUCCESS);

    validationReportContext.put(
        "info",
        createValidationInfo(validationInfoParameters)
    );
    validationReportContext.put(
        "success",
        worstStatus.equals(ValidationStepWithStatus.Status.SUCCESS)
    );
    validationReportContext.put(
        "status",
        worstStatus.toString()
    );
    validationReportContext.put(
        "tests",
        testsArray
    );

    return validationReportContext;
  }

  private Map<String, Object> createTestStepDescription(
      ValidationStepWithStatus testResult) {
    Map<String, Object> testStepDescription = new HashMap<>();
    testStepDescription.put("name", testResult.getName());
    testStepDescription.put("status", testResult.getStatus().toString());

    String message = testResult.getMessage();
    testStepDescription.put("message", message);

    List<Object> requestSnapshots = formatRequestSnapshotsToMap(testResult);
    testStepDescription.put("requestSnapshots", requestSnapshots);
    List<Object> responseSnapshots = formatResponseSnapshotsToMap(testResult);
    testStepDescription.put("responseSnapshots", responseSnapshots);

    boolean hasMessage = !message.isEmpty() && !message.equals("OK");
    testStepDescription.put("hasMessage", hasMessage);

    boolean hasDetails = !requestSnapshots.isEmpty() || !responseSnapshots.isEmpty();
    testStepDescription.put("hasDetails", hasDetails);

    int height = 1;
    if (hasMessage) {
      height += 1;
    }
    if (hasDetails) {
      height += 1;
    }

    testStepDescription.put("height", height);

    return testStepDescription;
  }


  private List<Object> formatResponseSnapshotsToMap(ValidationStepWithStatus testResult) {
    List<Object> results = new ArrayList<>();
    for (Response snapshot : testResult.getResponseSnapshots()) {
      results.add(formatResponseSnapshotToMap(snapshot));
    }
    return results;
  }

  private Map<String, Object> formatResponseSnapshotToMap(Response response) {
    Map<String, Object> result = new HashMap<>();
    result.put("status", response.getStatus());
    result.put("rawBodyBase64", Base64.encode(response.getBody()));
    BuildParams params = new BuildParams(response.getBody());
    params.setMakingPretty(true);
    BuildResult buildResult = this.docBuilder.build(params);
    if (buildResult.getDocument().isPresent()) {
      Match root = $(buildResult.getDocument().get()).namespaces(KnownNamespace.prefixMap());
      result.put(
          "developerMessage",
          root.xpath("/ewp:error-response/ewp:developer-message").text()
      );
      result.put("prettyXml", buildResult.getPrettyXml().orElse(null));
    } else {
      result.put("developerMessage", null);
      result.put("prettyXml", null);
    }
    result.put("headers", response.getHeaders());
    result.put("processingNoticesHtml", response.getProcessingNoticesHtml());
    return result;
  }

  private List<Object> formatRequestSnapshotsToMap(ValidationStepWithStatus testResult) {
    List<Object> results = new ArrayList<>();
    for (Request snapshot : testResult.getRequestSnapshots()) {
      results.add(formatRequestSnapshotToMap(snapshot));
    }
    return results;
  }

  private Map<String, Object> formatRequestSnapshotToMap(Request request) {
    Map<String, Object> result = new HashMap<>();
    if (request.getBody().isPresent()) {
      byte[] body = request.getBody().get();
      result.put("rawBodyBase64", Base64.encode(body));
      CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
      try {
        CharBuffer decoded = decoder.decode(ByteBuffer.wrap(body));
        result.put("body", decoded.toString());
      } catch (CharacterCodingException e) {
        result.put("body", null);
      }
    } else {
      result.put("rawBodyBase64", null);
      result.put("body", null);
    }
    result.put("url", request.getUrl());
    result.put("method", request.getMethod());
    result.put("headers", request.getHeaders());
    if (request.getClientCertificate().isPresent()) {
      try {
        result.put(
            "clientCertFingerprint",
            DigestUtils.sha256Hex(request.getClientCertificate().get().getEncoded())
        );
      } catch (CertificateEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      result.put("clientCertFingerprint", null);
    }
    result.put("processingNoticesHtml", request.getProcessingNoticesHtml());
    return result;
  }

}
