package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.types.ErrorResponse;
import eu.erasmuswithoutpaper.registry.validators.types.InstitutionsResponse;
import eu.erasmuswithoutpaper.registry.validators.types.InstitutionsResponse.Hei;
import eu.erasmuswithoutpaper.registry.validators.types.MultilineString;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.apache.commons.lang.StringEscapeUtils;

public abstract class AbstractInstitutionService implements FakeInternetService {
  protected final String myEndpoint;
  protected final RegistryClient registryClient;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractInstitutionService(String url, RegistryClient registryClient) {
    this.myEndpoint = url;
    this.registryClient = registryClient;
  }

  public String getEndpoint() {
    return myEndpoint;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      return this.handleInternetRequest2(request);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private String marshallObject(Object object) {
    try {
      JAXBContext jc = JAXBContext.newInstance("eu.erasmuswithoutpaper.registry.validators.types");
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

  private Response marshallResponse(int status, Object response) {
    String body = marshallObject(response);
    if (body == null) {
      return null;
    }
    return new Response(status, body.getBytes(StandardCharsets.UTF_8));
  }

  protected Response createInstitutionsResponse(Request request, List<Hei> heis) {
    InstitutionsResponse response = new InstitutionsResponse();
    response.getHei().addAll(heis);
    return marshallResponse(200, response);
  }

  protected Response createErrorResponse(Request request, int status, String developerMessage) {
    ErrorResponse response = new ErrorResponse();
    MultilineString multilineString = new MultilineString();
    multilineString.setValue(StringEscapeUtils.escapeXml(developerMessage));
    response.setDeveloperMessage(multilineString);
    return marshallResponse(status, response);
  }

  /**
   * @param request
   *     The request for which a response is to be generated
   * @return EWPEither <code>null</code> or {@link Response} object. If this service doesn't cover
   *     this particular request (for example the request is for a different domain), then
   *     <code>null</code> should be returned.
   * @throws IOException
   * @throws ErrorResponseException
   *     This can be thrown instead of returning the error response (a shortcut).
   */
  protected abstract Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException;


  /**
   * Helper class for easier delegation of error responses in {@link AbstractInstitutionService}.
   */
  protected static class ErrorResponseException extends Exception {

    protected final Response response;

    public ErrorResponseException(Response response) {
      this.response = response;
    }
  }
}
