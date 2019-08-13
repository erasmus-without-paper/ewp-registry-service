package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.institutionsvalidator.InstitutionServiceV2Valid;
import eu.erasmuswithoutpaper.registry.validators.types.FlexibleAddress;
import eu.erasmuswithoutpaper.registry.validators.types.HTTPWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.types.OunitsResponse;
import eu.erasmuswithoutpaper.registry.validators.types.StringWithOptionalLang;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OUnitsServiceV2Valid extends AbstractOUnitsService {
  protected final InstitutionServiceV2Valid institutionsServiceV2;
  protected Request currentRequest;
  protected String requestedHeiId;
  protected List<String> requestedOUnitIds;
  protected List<String> requestedOUnitCodes;
  protected List<String> requestedHeiIds;

  protected final int max_ounit_ids = 2;
  protected final int max_ounit_codes = 2;

  protected int getMaxOunitCodes() {
    return max_ounit_codes;
  }

  protected int getMaxOunitIds() {
    return max_ounit_ids;
  }

  private final EwpHttpSigRequestAuthorizer myAuthorizer;
  protected Map<String, OunitsResponse.Ounit> coveredOUnitsIds = new HashMap<>();
  protected Map<String, OunitsResponse.Ounit> coveredOUnitsCodes = new HashMap<>();
  protected List<OunitsResponse.Ounit> coveredOUnitsList = new ArrayList<>();

  private void addHei(OunitsResponse.Ounit data) {
    coveredOUnitsIds.put(data.getOunitId(), data);
    coveredOUnitsCodes.put(data.getOunitCode(), data);
    coveredOUnitsList.add(data);
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

  public OUnitsServiceV2Valid(String url, RegistryClient registryClient,
      InstitutionServiceV2Valid institutionsService) {
    super(url, registryClient, institutionsService);
    this.institutionsServiceV2 = institutionsService;
    this.myAuthorizer = new EwpHttpSigRequestAuthorizer(this.registryClient);
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

    OunitsResponse.Ounit d3 = createFakeOUnitData(covered_ounit_ids.get(2), covered_ounit_codes.get(2),
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

  @Override
  public Response handleOUnitsInternetRequest(Request request) {
    try {
      currentRequest = request;
      VerifyCertificate();
      CheckRequestMethod();
      ExtractParams();
      CheckHei();
      CheckIds();
      CheckCodes();
      List<OunitsResponse.Ounit> data1 = ProcessIds();
      List<OunitsResponse.Ounit> data2 = ProcessCodes();
      data1.addAll(data2);
      return createOUnitsResponse(data1);
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

  private void ExtractParams() throws ErrorResponseException {
    CheckParamsEncoding();
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(this.currentRequest);

    this.requestedHeiIds = params.getOrDefault("hei_id", new ArrayList<>());
    boolean hasHeiId = this.requestedHeiIds.size() > 0;
    boolean multipleHeiId = this.requestedHeiIds.size() > 1;

    this.requestedOUnitIds = params.getOrDefault("ounit_id", new ArrayList<>());
    boolean hasOUnitId = !this.requestedOUnitIds.isEmpty();

    this.requestedOUnitCodes = params.getOrDefault("ounit_code", new ArrayList<>());
    boolean hasOUnitCode = !this.requestedOUnitCodes.isEmpty();

    this.requestedHeiId = hasHeiId ? this.requestedHeiIds.get(0) : null;

    if (params.size() == 0) {
      ErrorNoParams();
    }
    if (!hasHeiId) {
      ErrorNoHeiId();
    }
    if (multipleHeiId) {
      ErrorMultipleHeiIds();
    }
    if (hasOUnitId && hasOUnitCode) {
      ErrorIdsAndCodes();
    }
    if (!hasOUnitId && !hasOUnitCode) {
      ErrorNoIdsNorCodes();
    }

    int expectedParams = 0;
    expectedParams += hasHeiId ? 1 : 0;
    expectedParams += hasOUnitCode ? 1 : 0;
    expectedParams += hasOUnitId ? 1 : 0;
    if (params.size() > expectedParams) {
      HandleUnexpectedParams();
    }

    if (this.requestedHeiId == null || this.requestedOUnitCodes == null || this.requestedOUnitIds == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  private void CheckHei() throws ErrorResponseException {
    if (!institutionsServiceV2.GetCoveredHeiIds().contains(this.requestedHeiId)) {
      ErrorUnknownHeiId();
    } else {
      HandleKnownHeiId();
    }
  }

  private void CheckCodes() throws ErrorResponseException {
    if (this.requestedOUnitCodes.size() > getMaxOunitCodes()) {
      ErrorMaxOUnitCodesExceeded();
    }
  }

  private void CheckIds() throws ErrorResponseException {
    if (this.requestedOUnitIds.size() > getMaxOunitIds()) {
      ErrorMaxOUnitIdsExceeded();
    }
  }

  private List<OunitsResponse.Ounit> ProcessCodes() {
    return ProcessRequested(this.requestedOUnitCodes, coveredOUnitsCodes);
  }

  private List<OunitsResponse.Ounit> ProcessIds() {
    return ProcessRequested(this.requestedOUnitIds, coveredOUnitsIds);
  }

  protected List<OunitsResponse.Ounit> ProcessRequested(
      List<String> requested,
      Map<String, OunitsResponse.Ounit> covered) {
    List<OunitsResponse.Ounit> ret = new ArrayList<>();
    for (String ounit : requested) {
      OunitsResponse.Ounit data = covered.get(ounit);
      if (data == null) {
        data = HandleUnknownOUnit();
      } else {
        data = HandleKnownOUnit(data);
      }
      if (data != null) {
        ret.add(data);
      }
    }
    return ret;
  }

  protected OunitsResponse.Ounit HandleKnownOUnit(OunitsResponse.Ounit data) {
    return data;
  }

  protected OunitsResponse.Ounit HandleUnknownOUnit() {
    return null;
  }

  protected void ErrorMaxOUnitCodesExceeded() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "max-ounit-codes exceeded")
    );
  }

  protected void ErrorMaxOUnitIdsExceeded() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "max-ounit-ids exceeded")
    );
  }

  protected void HandleKnownHeiId() {
    //Intentionally left empty
  }

  protected void ErrorUnknownHeiId() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Unknown hei_id")
    );
  }

  protected void ErrorMultipleHeiIds() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "More that one hei_id provided.")
    );
  }

  protected void ErrorNoIdsNorCodes() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "ounit_id xor ounit_code is required.")
    );
  }

  protected void ErrorIdsAndCodes() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Only one of ounit_id and ounit_code should" +
            " be provided")
    );
  }

  protected void ErrorNoHeiId() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "No hei_id parameter")
    );
  }

  protected void HandleUnexpectedParams() throws ErrorResponseException {
    //Ignore
  }


  protected void ErrorNoParams()
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

  protected void CheckParamsEncoding() throws ErrorResponseException {
    if (this.currentRequest.getMethod().equals("POST")
        && !this.currentRequest.getHeader("content-type").equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(this.currentRequest, 415, "Unsupported content-type")
      );
    }
  }

}
