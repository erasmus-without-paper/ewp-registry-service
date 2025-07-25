package eu.erasmuswithoutpaper.registry.validators;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.ErrorResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.MultilineString;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.w3c.dom.Element;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public abstract class AbstractApiService implements FakeInternetService {
  protected final RegistryClient registryClient;
  private EwpHttpSigRequestAuthorizer myAuthorizer;

  protected static <T> List<T> filter(List<T> data, Predicate<T> predicate) {
    return data.stream().filter(predicate).collect(Collectors.toList());
  }

  protected EwpHttpSigRequestAuthorizer createMyAuthorizer(RegistryClient registryClient) {
    return new EwpHttpSigRequestAuthorizer(registryClient);
  }

  protected EwpHttpSigRequestAuthorizer getMyAuthorizer() {
    return this.myAuthorizer;
  }

  protected AbstractApiService(RegistryClient registryClient) {
    this.myAuthorizer = createMyAuthorizer(registryClient);
    this.registryClient = registryClient;
  }

  protected String marshallObject(Object object) {
    try {
      JAXBContext jc = JAXBContext.newInstance(object.getClass().getPackage().getName());
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

  @SuppressWarnings("unchecked")
  protected <T> T unmarshallObject(byte[] data, Class<?> aClass) throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance(aClass.getPackage().getName());
    Unmarshaller unmarshaller = jc.createUnmarshaller();
    Element xml = AbstractValidationSuite.makeXmlFromBytes(data, true);
    return (T) unmarshaller.unmarshal(xml);
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
    multilineString.setValue(Utils.escapeXml(developerMessage));
    response.setDeveloperMessage(multilineString);
    return marshallResponse(status, response);
  }

  protected XMLGregorianCalendar xmlDate(int year, int month) {
    return xmlDate(year, month, 1);
  }

  protected XMLGregorianCalendar xmlDate(int year, int month, int day) {
    DatatypeFactory datatypeFactory;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      return null;
    }

    return datatypeFactory.newXMLGregorianCalendarDate(year, month, day, 0);
  }

  protected boolean isCalledPermittedToSeeReceivingHeiIdsData(EwpClientWithRsaKey client, String receivingHeiId) {
    return isHeiIdCoveredByClient(client.getRsaPublicKey(), receivingHeiId);
  }

  protected void errorUpdateCallerNotPermitted(Request request)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(request, 400, "Not permitted."));
  }

  protected void errorUnknownOmobilityIdUpdated(Request request)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(request, 400, "OMobility id not found"));
  }

  protected void errorChangesProposalIdDoNotMatch(Request request)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(request, 409, "Latest proposal id do not match."));
  }

  protected void errorNoChangesProposalId(Request request) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(request, 400, "Missing latest proposal id."));
  }

  protected boolean isHeiIdCoveredByClient(RSAPublicKey rsaPublicKey, String receivingHeiId) {
    return this.registryClient.getHeisCoveredByClientKey(rsaPublicKey).contains(receivingHeiId);
  }

  /**
   * Helper class for easier delegation of error responses in {@link AbstractApiService}.
   */
  protected static class ErrorResponseException extends Exception {
    private static final long serialVersionUID = -8024944558951936409L;

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
    checkRequestMethod(request, Arrays.asList("GET", "POST"));
  }

  protected void checkRequestMethod(Request request, List<String> allowedMethods) throws ErrorResponseException {
    if (!allowedMethods.contains(request.getMethod())) {
      throw new ErrorResponseException(
          createErrorResponse(request, 405, "Method not allowed.")
      );
    }
  }

  protected void checkParamsEncoding(Request request) throws ErrorResponseException {
    checkParamsEncoding(request, "application/x-www-form-urlencoded");
  }

  protected void checkParamsEncoding(Request request,
      String expectedContentType) throws ErrorResponseException {
    if (request.getMethod().equals("POST")
        && !request.getHeader("content-type").equals(expectedContentType)) {
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

  protected boolean checkReceivingAcademicYearId(String receivingAcademicYear) {
    String receivingAcademicYearPattern = "[0-9]{4}/[0-9]{4}";
    return Pattern.matches(receivingAcademicYearPattern, receivingAcademicYear);
  }

}
