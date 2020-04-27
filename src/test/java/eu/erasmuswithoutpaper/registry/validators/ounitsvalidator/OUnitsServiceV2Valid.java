package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.institutionsvalidator.InstitutionServiceV2Valid;
import eu.erasmuswithoutpaper.registry.validators.types.FlexibleAddress;
import eu.erasmuswithoutpaper.registry.validators.types.HTTPWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.types.OunitsResponse;
import eu.erasmuswithoutpaper.registry.validators.types.StringWithOptionalLang;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class OUnitsServiceV2Valid extends AbstractOUnitsService {
  protected static final int maxOUnitIds = 2;
  protected static final int maxOUnitCodes = 2;
  protected final InstitutionServiceV2Valid institutionsServiceV2;
  protected Map<String, OunitsResponse.Ounit> coveredOUnitsIds = new HashMap<>();
  protected Map<String, OunitsResponse.Ounit> coveredOUnitsCodes = new HashMap<>();

  public OUnitsServiceV2Valid(String url, RegistryClient registryClient,
      InstitutionServiceV2Valid institutionsService) {
    super(url, registryClient, institutionsService);
    this.institutionsServiceV2 = institutionsService;
    List<String> covered_ounit_ids = Arrays.asList("ounit-1", "ounit-2", "ounit-3");
    List<String> covered_ounit_codes = Arrays.asList("code1", "code2", "code3");

    //Create fake OUnits
    OunitsResponse.Ounit d1 = createFakeOUnitData(
        covered_ounit_ids.get(0),
        covered_ounit_codes.get(0),
        covered_ounit_ids.get(1));
    addHei(d1);

    OunitsResponse.Ounit d2 = createFakeOUnitData(
        covered_ounit_ids.get(1),
        covered_ounit_codes.get(1),
        null);

    d2.getMobilityFactsheetUrl().clear();
    HTTPWithOptionalLang d2_url1 = new HTTPWithOptionalLang();
    d2_url1.setValue("https://test.1");
    HTTPWithOptionalLang d2_url2 = new HTTPWithOptionalLang();
    d2_url2.setValue("https://test.2/en");
    d2_url2.setLang("EN");
    d2.getMobilityFactsheetUrl().add(d2_url1);
    d2.getMobilityFactsheetUrl().add(d2_url2);
    addHei(d2);

    OunitsResponse.Ounit d3 =
        createFakeOUnitData(covered_ounit_ids.get(2), covered_ounit_codes.get(2),
            covered_ounit_ids.get(1));
    d3.getMobilityFactsheetUrl().clear();
    HTTPWithOptionalLang d3_url1 = new HTTPWithOptionalLang();
    d3_url1.setValue("https://test.1");
    HTTPWithOptionalLang d3_url2 = new HTTPWithOptionalLang();
    d3_url2.setValue("https://test.2/pt");
    d3_url2.setLang("PT");
    d3.getMobilityFactsheetUrl().add(d3_url1);
    d3.getMobilityFactsheetUrl().add(d3_url2);
    addHei(d3);
  }

  protected int getMaxOunitCodes() {
    return maxOUnitCodes;
  }

  protected int getMaxOunitIds() {
    return maxOUnitIds;
  }

  private void addHei(OunitsResponse.Ounit data) {
    coveredOUnitsIds.put(data.getOunitId(), data);
    coveredOUnitsCodes.put(data.getOunitCode(), data);
  }

  protected OunitsResponse.Ounit createFakeOUnitData(String ounit_id, String ounit_code,
      String parent_ounit_id) {
    OunitsResponse.Ounit data = new OunitsResponse.Ounit();
    data.setOunitId(ounit_id);
    data.setOunitCode(ounit_code);
    data.setParentOunitId(parent_ounit_id);

    StringWithOptionalLang name1 = new StringWithOptionalLang();
    name1.setValue("Test1");
    name1.setLang("EN");

    StringWithOptionalLang name2 = new StringWithOptionalLang();
    name1.setValue("Test2");

    StringWithOptionalLang name3 = new StringWithOptionalLang();
    name1.setValue("Test4");
    name1.setLang("PL");

    data.getName().add(name1);
    data.getName().add(name2);
    data.getName().add(name3);
    data.setAbbreviation("TST");
    data.setLogoUrl("https://logo.url");

    FlexibleAddress address = new FlexibleAddress();
    address.setBuildingName("bm1");
    address.setBuildingNumber("bnum1");
    address.setFloor("1");
    address.getDeliveryPointCode().add("dpc1");
    address.getDeliveryPointCode().add("dpc2");
    address.setCountry("PL");
    address.setLocality("locality");
    address.setPostalCode("pc");
    address.getRecipientName().add("name1");
    address.getRecipientName().add("name2");
    address.setRegion("reg1");
    data.setMailingAddress(address);

    HTTPWithOptionalLang factsheet_url = new HTTPWithOptionalLang();
    factsheet_url.setValue("https://test.1");
    data.getMobilityFactsheetUrl().add(factsheet_url);

    HTTPWithOptionalLang website_url = new HTTPWithOptionalLang();
    website_url.setValue("https://test.1");
    website_url.setLang("PL");
    data.getWebsiteUrl().add(website_url);

    return data;
  }

  @Override
  public Response handleOUnitsInternetRequest(Request request) {
    try {
      verifyCertificate(request);
      checkRequestMethod(request);
      RequestData requestData = new RequestData(request);
      extractParams(requestData);
      checkHei(requestData);
      checkIds(requestData);
      checkCodes(requestData);
      List<OunitsResponse.Ounit> data1 = processIds(requestData);
      List<OunitsResponse.Ounit> data2 = processCodes(requestData);
      data1.addAll(data2);
      return createOUnitsResponse(data1);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    requestData.heiIds = params.getOrDefault("hei_id", new ArrayList<>());
    boolean hasHeiId = requestData.heiIds.size() > 0;
    boolean multipleHeiId = requestData.heiIds.size() > 1;

    requestData.ounitIds = params.getOrDefault("ounit_id", new ArrayList<>());
    boolean hasOUnitId = !requestData.ounitIds.isEmpty();

    requestData.ounitCodes = params.getOrDefault("ounit_code", new ArrayList<>());
    boolean hasOUnitCode = !requestData.ounitCodes.isEmpty();

    requestData.heiId = hasHeiId ? requestData.heiIds.get(0) : null;

    if (params.size() == 0) {
      handleNoParams(requestData);
    }
    if (!hasHeiId) {
      handleNoHeiId(requestData);
    }
    if (multipleHeiId) {
      handleMultipleHeiIds(requestData);
    }
    if (hasOUnitId && hasOUnitCode) {
      handleIdsAndCodes(requestData);
    }
    if (!hasOUnitId && !hasOUnitCode) {
      handleNoIdsNorCodes(requestData);
    }

    int expectedParams = 0;
    expectedParams += hasHeiId ? 1 : 0;
    expectedParams += hasOUnitCode ? 1 : 0;
    expectedParams += hasOUnitId ? 1 : 0;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.heiId == null || requestData.ounitCodes == null
        || requestData.ounitIds == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  private void checkHei(RequestData requestData) throws ErrorResponseException {
    if (!institutionsServiceV2.getCoveredHeiIds().contains(requestData.heiId)) {
      errorUnknownHeiId(requestData);
    } else {
      handleKnownHeiId(requestData);
    }
  }

  private void checkCodes(RequestData requestData) throws ErrorResponseException {
    if (requestData.ounitCodes.size() > getMaxOunitCodes()) {
      errorMaxOUnitCodesExceeded(requestData);
    }
  }

  private void checkIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.ounitIds.size() > getMaxOunitIds()) {
      errorMaxOUnitIdsExceeded(requestData);
    }
  }

  private List<OunitsResponse.Ounit> processCodes(RequestData requestData) {
    return processRequested(requestData.ounitCodes, coveredOUnitsCodes);
  }

  private List<OunitsResponse.Ounit> processIds(RequestData requestData) {
    return processRequested(requestData.ounitIds, coveredOUnitsIds);
  }

  protected List<OunitsResponse.Ounit> processRequested(
      List<String> requested,
      Map<String, OunitsResponse.Ounit> covered) {
    List<OunitsResponse.Ounit> ret = new ArrayList<>();
    for (String ounit : requested) {
      OunitsResponse.Ounit data = covered.get(ounit);
      if (data == null) {
        data = handleUnknownOUnit();
      } else {
        data = handleKnownOUnit(data);
      }
      if (data != null) {
        ret.add(data);
      }
    }
    return ret;
  }

  protected OunitsResponse.Ounit handleKnownOUnit(OunitsResponse.Ounit data) {
    return data;
  }

  protected OunitsResponse.Ounit handleUnknownOUnit() {
    return null;
  }

  protected void errorMaxOUnitCodesExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-ounit-codes exceeded")
    );
  }

  protected void errorMaxOUnitIdsExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-ounit-ids exceeded")
    );
  }

  protected void handleKnownHeiId(RequestData requestData) {
    //Intentionally left empty
  }

  protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Unknown hei_id")
    );
  }

  protected void handleMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one hei_id provided.")
    );
  }

  protected void handleNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "ounit_id xor ounit_code is required.")
    );
  }

  protected void handleIdsAndCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Only one of ounit_id and ounit_code should" +
            " be provided")
    );
  }

  protected void handleNoHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No hei_id parameter")
    );
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }


  static class RequestData {
    Request request;
    List<String> heiIds;
    List<String> ounitIds;
    List<String> ounitCodes;
    String heiId;

    public RequestData(Request request) {
      this.request = request;
    }
  }
}
