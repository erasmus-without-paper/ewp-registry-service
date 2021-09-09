package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_institutions.tree.stable_v2.InstitutionsResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.HTTPWithOptionalLang;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.StringWithOptionalLang;
import https.github_com.erasmus_without_paper.ewp_specs_types_address.tree.stable_v1.FlexibleAddress;

public class InstitutionServiceV2Valid extends AbstractInstitutionService {

  protected static final int max_hei_ids = 2;
  private final List<String> coveredHeiIds;
  protected Map<String, InstitutionsResponse.Hei> coveredHeis = new HashMap<>();

  private void addHei(InstitutionsResponse.Hei data) {
    coveredHeis.put(data.getHeiId(), data);
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
      verifyCertificate(request);
      checkRequestMethod(request);
      RequestData requestData = new RequestData(request);
      extractParams(requestData);
      checkHeis(requestData);
      List<InstitutionsResponse.Hei> heis_data = processHeis(requestData);
      return createInstitutionsResponse(heis_data);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected void checkHeis(RequestData requestData) throws ErrorResponseException {
    if (requestData.heiIds.size() > max_hei_ids) {
      errorMaxHeiIdsExceeded(requestData);
    }
  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);
    if (params.size() == 0) {
      handleNoParams(requestData);
    }
    if (params.size() == 1 && !params.containsKey("hei_id")) {
      handleNoHeiIdsParams(requestData);
    }
    if (params.size() > 1) {
      handleMultipleParams(requestData);
    }
    List<String> heiIds = params.get("hei_id");
    if (heiIds == null) {
      heiIds = new ArrayList<>();
    }
    requestData.heiIds = heiIds;
  }

  protected void handleMultipleParams(RequestData params)
      throws ErrorResponseException {
    //Ignore unknown parameters
  }

  protected void handleNoHeiIdsParams(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Expected \"hei_id\" parameters"));
  }

  protected void handleNoParams(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided"));
  }

  protected InstitutionsResponse.Hei processCoveredHei(RequestData requestData, String hei)
      throws ErrorResponseException {
    return coveredHeis.get(hei);
  }

  protected InstitutionsResponse.Hei  processNotCoveredHei(RequestData requestData, String hei)
      throws ErrorResponseException {
    //Ignore
    return null;
  }

  protected List<InstitutionsResponse.Hei> processHeis(RequestData requestData)
      throws ErrorResponseException {
    List<InstitutionsResponse.Hei> result = new ArrayList<>();
    for (String hei : requestData.heiIds) {
      InstitutionsResponse.Hei responseHei;
      if (coveredHeis.containsKey(hei)) {
        responseHei = processCoveredHei(requestData, hei);
      } else {
        responseHei = processNotCoveredHei(requestData, hei);
      }

      if (responseHei != null) {
        result.add(responseHei);
      }
    }
    return result;
  }

  protected void errorMaxHeiIdsExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Exceeded max-hei-ids"));
  }

  static class RequestData {
    Request request;
    List<String> heiIds;

    public RequestData(Request request) {
      this.request = request;
    }
  }
}
