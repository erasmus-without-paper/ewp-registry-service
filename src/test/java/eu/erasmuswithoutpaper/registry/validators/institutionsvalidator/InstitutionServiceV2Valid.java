package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.types.FlexibleAddress;
import eu.erasmuswithoutpaper.registry.validators.types.HTTPWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.types.InstitutionsResponse;
import eu.erasmuswithoutpaper.registry.validators.types.StringWithOptionalLang;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class InstitutionServiceV2Valid extends AbstractInstitutionService {

  protected static final int max_hei_ids = 2;
  private final EwpHttpSigRequestAuthorizer myAuthorizer;
  private final List<String> coveredHeiIds;
  protected Request currentRequest;
  protected Map<String, InstitutionsResponse.Hei> coveredHeis = new HashMap<>();
  protected List<InstitutionsResponse.Hei> coveredHeisList = new ArrayList<>();

  private void addHei(InstitutionsResponse.Hei data) {
    coveredHeis.put(data.getHeiId(), data);
    coveredHeisList.add(data);
  }

  protected InstitutionsResponse.Hei createFakeHeiData(String heiid) {
    InstitutionsResponse.Hei data = new InstitutionsResponse.Hei();
    data.setHeiId(heiid);

    StringWithOptionalLang stringWithOptionalLang1 = new StringWithOptionalLang();
    stringWithOptionalLang1.setValue("Test1");
    stringWithOptionalLang1.setLang("EN");
    data.getName().add(stringWithOptionalLang1);

    StringWithOptionalLang stringWithOptionalLang2 = new StringWithOptionalLang();
    stringWithOptionalLang2.setValue("Test2");
    data.getName().add(stringWithOptionalLang2);

    StringWithOptionalLang stringWithOptionalLang3 = new StringWithOptionalLang();
    stringWithOptionalLang3.setValue("Test1");
    stringWithOptionalLang3.setLang("EN");
    data.getName().add(stringWithOptionalLang3);

    data.setAbbreviation("TST");
    data.setLogoUrl("https://logo.url");

    FlexibleAddress flexibleAddress = new FlexibleAddress();
    flexibleAddress.setCountry("PL");
    flexibleAddress.setLocality("locality");
    flexibleAddress.setPostalCode("postal code");
    flexibleAddress.getRecipientName().add("name1");
    flexibleAddress.getRecipientName().add("name2");
    flexibleAddress.setRegion("reg1");

    flexibleAddress.setBuildingName("bn1");
    flexibleAddress.setBuildingNumber("42");
    flexibleAddress.getDeliveryPointCode().add("dpc1");
    flexibleAddress.getDeliveryPointCode().add("dpc2");
    flexibleAddress.setFloor("1");
    flexibleAddress.setPostOfficeBox("bo1");
    flexibleAddress.setStreetName("street1");
    flexibleAddress.setUnit("unit");
    data.setMailingAddress(flexibleAddress);

    HTTPWithOptionalLang httpWithOptionalLang1 = new HTTPWithOptionalLang();
    httpWithOptionalLang1.setValue("https://test.1");
    data.getMobilityFactsheetUrl().add(httpWithOptionalLang1);

    data.getOunitId().addAll(getCoveredOUnits());
    data.setRootOunitId(getRootOUnit());

    HTTPWithOptionalLang httpWithOptionalLang2 = new HTTPWithOptionalLang();
    httpWithOptionalLang2.setValue("https://test.1");
    httpWithOptionalLang2.setLang("PL");
    data.getWebsiteUrl().add(httpWithOptionalLang2);
    return data;
  }

  protected String getRootOUnit() {
    return "2";
  }

  protected List<String> getCoveredOUnits() {
    return Arrays.asList("1", "2", "3");
  }

  public List<String> getCoveredHeiIds() {
    return coveredHeiIds;
  }

  public InstitutionServiceV2Valid(String url, RegistryClient registryClient,
    ValidatorKeyStore validatorKeyStore) {
    super(url, registryClient);
    this.myAuthorizer = new EwpHttpSigRequestAuthorizer(this.registryClient);
    coveredHeiIds = validatorKeyStore.getCoveredHeiIDs();

    //Create fake HEIs
    InstitutionsResponse.Hei d1 = createFakeHeiData(coveredHeiIds.get(0));
    addHei(d1);

    InstitutionsResponse.Hei d2 = createFakeHeiData(coveredHeiIds.get(1));
    d2.getMobilityFactsheetUrl().clear();

    HTTPWithOptionalLang httpWithOptionalLang1 = new HTTPWithOptionalLang();
    httpWithOptionalLang1.setValue("https://test.1");
    d2.getMobilityFactsheetUrl().add(httpWithOptionalLang1);

    HTTPWithOptionalLang httpWithOptionalLang2 = new HTTPWithOptionalLang();
    httpWithOptionalLang2.setValue("https://test.1/en");
    httpWithOptionalLang2.setLang("EN");
    d2.getMobilityFactsheetUrl().add(httpWithOptionalLang2);
    addHei(d2);
  }

  @Override
  public Response handleInstitutionsInternetRequest(Request request) {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    try {
      currentRequest = request;
      verifyCertificate();
      checkRequestMethod();
      List<String> heis = extractParams();
      checkHeis(heis);
      List<InstitutionsResponse.Hei> heis_data = processHeis(heis);
      return createInstitutionsResponse(heis_data);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void verifyCertificate() throws ErrorResponseException {
    try {
      this.myAuthorizer.authorize(this.currentRequest);
    } catch (Http4xx e) {
      throw new ErrorResponseException(e.generateEwpErrorResponse());
    }
  }

  protected void checkHeis(List<String> heis) throws ErrorResponseException {
    if (heis.size() > max_hei_ids) {
      throw new ErrorResponseException(
          createErrorResponse(this.currentRequest, 400, "Exceeded max-hei-ids"));
    }
  }

  private List<String> extractParams() throws ErrorResponseException {
    checkParamsEncoding();
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(this.currentRequest);
    if (params.size() == 0) {
      extractParamsNoParams(params);
    }
    if (params.size() == 1 && !params.containsKey("hei_id")) {
      extractParamsNoHeiIds(params);
    }
    if (params.size() > 1) {
      extractParamsMultipleParams(params);
    }
    List<String> ret = params.get("hei_id");
    if (ret != null) {
      return ret;
    }
    return new ArrayList<>();
  }

  protected void checkParamsEncoding() throws ErrorResponseException {
    if (this.currentRequest.getMethod().equals("POST")
        && !this.currentRequest.getHeader("content-type").equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(this.currentRequest, 415, "Unsupported content-type"));
    }
  }

  protected void extractParamsMultipleParams(Map<String, List<String>> params)
      throws ErrorResponseException {
    //Ignore unknown parameters
  }

  protected void extractParamsNoHeiIds(Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Expected \"hei_id\" parameters"));
  }

  protected void extractParamsNoParams(Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "No parameters provided"));
  }

  protected void checkRequestMethod() throws ErrorResponseException {
    if (!(this.currentRequest.getMethod().equals("GET")
        || this.currentRequest.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
          this.createErrorResponse(this.currentRequest, 405, "We expect GETs and POSTs only"));
    }
  }

  protected void processCoveredHei(String hei, List<InstitutionsResponse.Hei> heis)
      throws ErrorResponseException {
    heis.add(coveredHeis.get(hei));
  }

  protected void processNotCoveredHei(String hei, List<InstitutionsResponse.Hei> heis)
      throws ErrorResponseException {
    //Ignore
  }

  protected List<InstitutionsResponse.Hei> processHeis(List<String> heis)
      throws ErrorResponseException {
    List<InstitutionsResponse.Hei> ret = new ArrayList<>();
    for (String hei : heis) {
      if (coveredHeis.containsKey(hei)) {
        processCoveredHei(hei, ret);
      } else {
        processNotCoveredHei(hei, ret);
      }
    }
    return ret;
  }
}
