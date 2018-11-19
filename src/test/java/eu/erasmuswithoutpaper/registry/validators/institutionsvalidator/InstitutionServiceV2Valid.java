package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.EWPCountryCode;
import eu.erasmuswithoutpaper.registry.validators.EWPFlexibleAddress;
import eu.erasmuswithoutpaper.registry.validators.EWPHTTPWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.EWPStringWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.EWPUrlHTTP;
import eu.erasmuswithoutpaper.registry.validators.EWPUrlHTTPS;
import eu.erasmuswithoutpaper.registry.validators.EWPEither;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.XMLSchemaRef;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import java.io.IOException;
import java.util.*;

public class InstitutionServiceV2Valid extends AbstractInstitutionV2Service {

  protected Request currentRequest;

  protected static class ErrorResponseException extends Exception {
    public Response response;
    public ErrorResponseException(Response _response) {
      response = _response;
    }
  }

  protected final int max_hei_ids = 2;
  private final EwpHttpSigRequestAuthorizer myAuthorizer;
  private final List<String> coveredHeiIds;
  protected Map<String, HEIData> coveredHeis = new HashMap<>();
  protected List<HEIData> coveredHeisList = new ArrayList<>();

  private void addHei(HEIData data) {
    coveredHeis.put(data.hei_id, data);
    coveredHeisList.add(data);
  }

  protected HEIData createFakeHeiData(String heiid) {
    HEIData data = new HEIData();
    data.hei_id = heiid;
    data.name = new EWPStringWithOptionalLang("Test1", "EN");
    data.name_ = Arrays.asList(
      new EWPStringWithOptionalLang("Test2"),
      new EWPStringWithOptionalLang("Test3", "PL")
    );
    data.abbreviation = "TST";
    data.contact = null;
    data.logo_url = new EWPUrlHTTPS("https://logo.url");
    data.mailing_address = new XMLSchemaRef<>(new EWPFlexibleAddress(), "a");

    EWPFlexibleAddress.AdvancedAddress adr = new EWPFlexibleAddress.AdvancedAddress();
    adr.buildingName = "bn1";
    adr.buildingNumber = "bnum2";
    adr.deliveryPointCode = Arrays.asList("dpc1", "dpc2");
    adr.floor = "1";
    adr.postBoxOffice = "bo1";
    adr.streetName = "street1";
    adr.unit = "unit";
    data.mailing_address.elem.address = EWPEither.fromRight(adr);
    data.mailing_address.elem.country = new EWPCountryCode("PL");
    data.mailing_address.elem.locality = "locality";
    data.mailing_address.elem.postalCode = "postal code";
    data.mailing_address.elem.recipientName = Arrays.asList("name1", "name2");
    data.mailing_address.elem.region = "reg1";
    data.mobility_factsheet_url = Arrays.asList(
      new EWPHTTPWithOptionalLang(new EWPUrlHTTP("https://test.1"))
    );
    data.ounit_id = GetCoveredOUnits();
    data.root_ounit_id = GetRootOUnit();
    data.website_url = new EWPHTTPWithOptionalLang(new EWPUrlHTTP("https://www.test.pl"), "PL");
    return data;
  }

  protected String GetRootOUnit() {
    return "2";
  }

  protected List<String> GetCoveredOUnits() {
    return Arrays.asList("1", "2", "3");
  }

  public List<String> GetCoveredHeiIds() {
    return coveredHeiIds;
  }

  public InstitutionServiceV2Valid(String url, RegistryClient registryClient,
    ValidatorKeyStore validatorKeyStore) {
    super(url, registryClient);
    this.myAuthorizer = new EwpHttpSigRequestAuthorizer(this.registryClient);
    coveredHeiIds = validatorKeyStore.getCoveredHeiIDs();

    //Create fake HEIs
    HEIData d1 = createFakeHeiData(coveredHeiIds.get(0));
    addHei(d1);

    HEIData d2 = createFakeHeiData(coveredHeiIds.get(1));
    d2.mobility_factsheet_url = Arrays.asList(
      new EWPHTTPWithOptionalLang(new EWPUrlHTTP("https://test.1")),
      new EWPHTTPWithOptionalLang(new EWPUrlHTTP("https://test.2/en"), "EN")
    );
    addHei(d2);
  }

  @Override
  public Response handleInternetRequest2(Request request) throws IOException {

    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    try {
      currentRequest = request;
      VerifyCertificate();
      CheckRequestMethod();
      List<String> heis = ExtractParams();
      CheckHeis(heis);
      List<HEIData> heis_data = ProcessHeis(heis);
      return createInstitutionsResponse(this.currentRequest, heis_data);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void VerifyCertificate() throws ErrorResponseException {
    try {
      this.myAuthorizer.authorize(this.currentRequest);
    } catch (Http4xx e) {
      throw new ErrorResponseException(
        e.generateEwpErrorResponse()
      );
    }
  }

  protected void CheckHeis(List<String> heis) throws ErrorResponseException {
    if (heis.size() > max_hei_ids) {
      throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Exceeded max-hei-ids")
      );
    }
  }

  private List<String> ExtractParams() throws ErrorResponseException {
    CheckParamsEncoding();
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(this.currentRequest);
    if (params.size() == 0) {
      ExtractParamsNoParams(params);
    }
    if (params.size() == 1 && !params.containsKey("hei-id")) {
      ExtractParamsNoHeiIds(params);
    }
    if (params.size() > 1) {
      ExtractParamsMultipleParams(params);
    }
    List<String> ret = params.get("hei-id");
    if (ret != null) {
      return ret;
    }
    return new ArrayList<>();
  }

  protected void CheckParamsEncoding() throws ErrorResponseException {
    if (this.currentRequest.getMethod().equals("POST")
        && !this.currentRequest.getHeader("content-type").equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 415, "Unsupported content-type")
      );
    }
  }

  protected void ExtractParamsMultipleParams(Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
      createErrorResponse(this.currentRequest, 400, "Expected only \"hei-id\" parameters")
    );
  }

  protected void ExtractParamsNoHeiIds(Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
      createErrorResponse(this.currentRequest, 400, "Expected \"hei-id\" parameters")
    );
  }

  protected void ExtractParamsNoParams(Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
      createErrorResponse(this.currentRequest, 400, "No parameters provided")
    );
  }

  protected void CheckRequestMethod() throws ErrorResponseException {
    if (!(this.currentRequest.getMethod().equals("GET") || this.currentRequest.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
        this.createErrorResponse(this.currentRequest, 405, "We expect GETs and POSTs only")
      );
    }
  }

  protected void ProcessCoveredHei(String hei, List<HEIData> heis)
      throws ErrorResponseException {
    heis.add(coveredHeis.get(hei));
  }

  protected void ProcessNotCoveredHei(String hei, List<HEIData> heis)
      throws ErrorResponseException {
    //Ignore
  }

  protected List<HEIData> ProcessHeis(List<String> heis)
      throws ErrorResponseException {
    List<HEIData> ret = new ArrayList<>();
    for (String hei : heis) {
      if (coveredHeis.containsKey(hei)) {
        ProcessCoveredHei(hei, ret);
      }
      else {
        ProcessNotCoveredHei(hei, ret);
      }
    }
    return ret;
  }
}
