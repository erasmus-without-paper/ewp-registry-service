package eu.erasmuswithoutpaper.registry.validators;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.types.ErrorResponse;
import eu.erasmuswithoutpaper.registry.validators.types.MultilineString;

import org.apache.commons.lang.StringEscapeUtils;

public abstract class AbstractApiService implements FakeInternetService {
  protected String marshallObject(Object object) {
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

}
