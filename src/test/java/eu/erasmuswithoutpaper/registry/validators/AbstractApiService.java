package eu.erasmuswithoutpaper.registry.validators;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.types.ErrorResponse;
import eu.erasmuswithoutpaper.registry.validators.types.MultilineString;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.apache.commons.lang.StringEscapeUtils;

public abstract class AbstractApiService implements FakeInternetService {
  protected final RegistryClient registryClient;
  private EwpHttpSigRequestAuthorizer myAuthorizer;

  protected EwpHttpSigRequestAuthorizer createMyAuthorizer(RegistryClient registryClient) {
    return new EwpHttpSigRequestAuthorizer(registryClient);
  }

  protected EwpHttpSigRequestAuthorizer getMyAuthorizer() {
    return this.myAuthorizer;
  }

  protected String getJaxbContextPackagePath() {
    return "eu.erasmuswithoutpaper.registry.validators.types";
  }

  protected AbstractApiService(RegistryClient registryClient) {
    this.myAuthorizer = createMyAuthorizer(registryClient);
    this.registryClient = registryClient;
  }

  protected String marshallObject(Object object) {
    try {
      JAXBContext jc = JAXBContext.newInstance(getJaxbContextPackagePath());
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      StringWriter sw = new StringWriter();
      marshaller.marshal(object, sw);
      return sw.toString();
    } catch (JAXBException e) {
      e.printStackTrace();
      return null;
    }
  }

  protected Response marshallResponse(int status, Object response) {
    String body = marshallObject(response);
    if (body == null) {
      return null;
    }
    return new Response(status, body.getBytes(StandardCharsets.UTF_8));
  }

  protected Response createErrorResponse(Request request, int status, String developerMessage) {
    ErrorResponse response = new ErrorResponse();
    MultilineString multilineString = new MultilineString();
    multilineString.setValue(StringEscapeUtils.escapeXml(developerMessage));
    response.setDeveloperMessage(multilineString);
    return marshallResponse(status, response);
  }

  /**
   * Helper class for easier delegation of error responses in {@link AbstractApiService}.
   */
  protected static class ErrorResponseException extends Exception {

    public final Response response;

    public ErrorResponseException(Response response) {
      this.response = response;
    }
  }

  protected final ZonedDateTime parseModifiedSince(String modifiedSince) {
    // This pattern is taken from https://www.w3.org/TR/xmlschema11-2/#dateTime
    String dateTimePattern = "-?([1-9][0-9]{3,}|0[0-9]{3})"
        + "-(0[1-9]|1[0-2])"
        + "-(0[1-9]|[12][0-9]|3[01])"
        + "T(([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?|(24:00:00(\\.0+)?))"
        + "(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?";

    if (modifiedSince != null) {
      try {
        Calendar cal = DatatypeConverter.parseDateTime(modifiedSince);
        if (!Pattern.matches(dateTimePattern, modifiedSince)) {
          return null;
        }

        return ZonedDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    return null;
  }

  protected void checkRequestMethod(Request request) throws ErrorResponseException {
    if (!(request.getMethod().equals("GET") || request.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
          createErrorResponse(request, 405, "We expect GETs and POSTs only")
      );
    }
  }

  protected void checkParamsEncoding(Request request) throws ErrorResponseException {
    if (request.getMethod().equals("POST")
        && !request.getHeader("content-type").equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(request, 415, "Unsupported content-type")
      );
    }
  }


  protected EwpClientWithRsaKey verifyCertificate(Request request) throws ErrorResponseException {
    try {
      return getMyAuthorizer().authorize(request);
    } catch (Http4xx e) {
      throw new ErrorResponseException(e.generateEwpErrorResponse());
    }
  }

}
