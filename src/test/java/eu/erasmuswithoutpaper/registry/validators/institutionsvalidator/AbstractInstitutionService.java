package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.EWPContact;
import eu.erasmuswithoutpaper.registry.validators.EWPFlexibleAddress;
import eu.erasmuswithoutpaper.registry.validators.EWPHTTPWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.EWPStringWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.EWPUrlHTTPS;
import eu.erasmuswithoutpaper.registry.validators.XMLBuilder;
import eu.erasmuswithoutpaper.registry.validators.XMLSchemaRef;
import eu.erasmuswithoutpaper.registry.validators.XMLSerializable;
import eu.erasmuswithoutpaper.registry.validators.XMLSerializableUtils;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.apache.commons.lang.StringEscapeUtils;

import javax.validation.constraints.NotNull;

abstract public class AbstractInstitutionService implements FakeInternetService {

  public class HEIData implements XMLSerializable {
    public @NotNull String hei_id;
    public List<String> other_id;
    public @NotNull EWPStringWithOptionalLang name;
    public List<EWPStringWithOptionalLang> name_;
    public String abbreviation;
    public XMLSchemaRef<EWPFlexibleAddress> street_address;
    public XMLSchemaRef<EWPFlexibleAddress> mailing_address;
    public EWPHTTPWithOptionalLang website_url;
    public EWPUrlHTTPS logo_url;
    public List<EWPHTTPWithOptionalLang> mobility_factsheet_url;
    public List<XMLSchemaRef<EWPContact>> contact;
    public String root_ounit_id;
    public List<String> ounit_id;

    @Override
    public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
      XMLSerializableUtils.OpenTag(_name, _ns, _tags, _builder);
      XMLSerializableUtils.XMLSerializeString("hei-id", _ns, hei_id, _builder);
      XMLSerializableUtils.XMLSerializeListString("other-id", _ns, other_id, _builder);
      XMLSerializableUtils.XMLSerializeSerializable("name", _ns, name, _builder);
      XMLSerializableUtils.XMLSerializeList("name", _ns, name_, _builder);
      XMLSerializableUtils.XMLSerializeString("abbreviation", _ns, abbreviation, _builder);
      XMLSerializableUtils.XMLSerializeSerializable("street-address", "a", street_address, _builder);
      XMLSerializableUtils.XMLSerializeSerializable("mailing-address", "a", mailing_address, _builder);
      XMLSerializableUtils.XMLSerializeSerializable("website-url", _ns, website_url, _builder);
      XMLSerializableUtils.XMLSerializeSerializable("logo-url", _ns, logo_url, _builder);
      XMLSerializableUtils.XMLSerializeList("mobility-factsheet-url", _ns, mobility_factsheet_url, _builder);
      XMLSerializableUtils.XMLSerializeList("contact", _ns, contact, _builder);
      XMLSerializableUtils.XMLSerializeString("root-ounit-id", _ns, root_ounit_id, _builder);
      XMLSerializableUtils.XMLSerializeListString("ounit-id", _ns, ounit_id, _builder);
      XMLSerializableUtils.CloseTag(_name, _ns, _builder);
    }
  }

  /**
   * Helper class for easier delegation of error responses in {@link AbstractInstitutionService}.
   */
  protected static class ErrorResponseException extends Exception {

    protected final Response response;

    public ErrorResponseException(Response response) {
      this.response = response;
    }
  }

  public String getEndpoint() {
    return myEndpoint;
  }

  protected final String myEndpoint;
  protected final RegistryClient registryClient;

  /**
   * @param url The endpoint at which to listen for requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractInstitutionService(String url, RegistryClient registryClient) {
    this.myEndpoint = url;
    this.registryClient = registryClient;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      return this.handleInternetRequest2(request);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createInstitutionsResponse(
      Request _request,
      String _xmlns,
      List<HEIData> _heis) {
    XMLBuilder builder = new XMLBuilder("institutions-response");
    builder.addNamespace(_xmlns, "");
    builder.addNamespace(
        "https://github.com/erasmus-without-paper/ewp-specs-types-address/tree/stable-v1",
        "a"
    );
    builder.addNamespace(
        "https://github.com/erasmus-without-paper/ewp-specs-types-contact/tree/stable-v1",
        "c"
    );
    builder.AddSchemaLocation(
        "https://github.com/erasmus-without-paper/ewp-specs-api-institutions/tree/stable-v2",
        "https://raw.githubusercontent.com/erasmus-without-paper/ewp-specs-api-institutions/stable-v2/response.xsd"
    );
    XMLSerializableUtils.XMLSerializeList("hei", null, _heis, builder);
    return new Response(200, builder.toString().getBytes(StandardCharsets.UTF_8));
  }

  protected Response createErrorResponse(Request request, int status, String developerMessage) {
    StringBuilder sb = new StringBuilder();
    String NS = KnownNamespace.COMMON_TYPES_V1.getNamespaceUri();
    sb.append("<error-response xmlns='").append(NS).append("'>");
    sb.append("<developer-message>");
    sb.append(StringEscapeUtils.escapeXml(developerMessage));
    sb.append("</developer-message>");
    sb.append("</error-response>");
    return new Response(status, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * @param request The request for which a response is to be generated
   * @return EWPEither <code>null</code> or {@link Response} object. If this service doesn't cover this
   *     particular request (for example the request is for a different domain), then
   *     <code>null</code> should be returned.
   * @throws IOException
   * @throws ErrorResponseException This can be thrown instead of returning the error response (a
   *     shortcut).
   */
  abstract protected Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException;
}
