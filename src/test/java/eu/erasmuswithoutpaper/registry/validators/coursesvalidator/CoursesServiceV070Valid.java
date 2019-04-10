package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator.CourseReplicationServiceV1Valid;
import eu.erasmuswithoutpaper.registry.validators.types.CoursesResponse;
import eu.erasmuswithoutpaper.registry.validators.types.StringWithOptionalLang;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class CoursesServiceV070Valid extends AbstractCoursesService {
  protected final CourseReplicationServiceV1Valid CourseReplicationServiceV2;
  protected final int max_los_ids = 2;
  protected final int max_los_codes = 2;
  private final EwpHttpSigRequestAuthorizer myAuthorizer;
  protected Map<String, CoursesResponse.LearningOpportunitySpecification> coveredLosIds =
      new HashMap<>();
  protected Map<String, CoursesResponse.LearningOpportunitySpecification> coveredLosCodes =
      new HashMap<>();
  protected List<CoursesResponse.LearningOpportunitySpecification> coveredLossList =
      new ArrayList<>();

  public CoursesServiceV070Valid(String url, RegistryClient registryClient,
      CourseReplicationServiceV1Valid courseReplicationService) {
    super(url, registryClient, courseReplicationService);
    this.CourseReplicationServiceV2 = courseReplicationService;
    this.myAuthorizer = new EwpHttpSigRequestAuthorizer(this.registryClient);

    List<String> covered_los_ids = CourseReplicationServiceV2.getCoveredLosIds();

    for (String losId : covered_los_ids) {
      CoursesResponse.LearningOpportunitySpecification data = createFakeLosData(
          losId,
          losId + "_code"
      );
      addLos(data);
    }
  }

  protected int getMaxLosCodes() {
    return max_los_codes;
  }

  protected int getMaxLosIds() {
    return max_los_ids;
  }

  private void addLos(CoursesResponse.LearningOpportunitySpecification data) {
    coveredLosIds.put(data.getLosId(), data);
    coveredLosCodes.put(data.getLosCode(), data);
    coveredLossList.add(data);
  }

  protected void setLosCode(CoursesResponse.LearningOpportunitySpecification spec,
      String los_code) {
    spec.setLosCode(los_code);
  }

  protected CoursesResponse.LearningOpportunitySpecification createFakeLosData(String los_id,
      String los_code) {

    CoursesResponse.LearningOpportunitySpecification data =
        new CoursesResponse.LearningOpportunitySpecification();
    data.setLosId(los_id);
    setLosCode(data, los_code);
    StringWithOptionalLang title = new StringWithOptionalLang();
    title.setLang("en");
    title.setValue("test");
    data.getTitle().add(title);
    data.setOunitId("test-ounit-id");

    CoursesResponse.LearningOpportunitySpecification.Specifies specifies =
        new CoursesResponse.LearningOpportunitySpecification.Specifies();

    CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance i1 =
        new CoursesResponse.LearningOpportunitySpecification.Specifies
            .LearningOpportunityInstance();
    i1.setLoiId("CRI/111212/" + los_id);

    try {
      XMLGregorianCalendar startDate =
          DatatypeFactory.newInstance().newXMLGregorianCalendar(2011, 1, 1, 0, 0, 0, 0, 0);
      startDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
      i1.setStart(startDate);

      XMLGregorianCalendar endDate =
          DatatypeFactory.newInstance().newXMLGregorianCalendar(2011, 6, 20, 0, 0, 0, 0, 0);
      endDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
      i1.setEnd(endDate);
    } catch (DatatypeConfigurationException e) {
      // This should not happen.
      assert (false);
    }

    specifies.getLearningOpportunityInstance().add(i1);

    CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance i2 =
        new CoursesResponse.LearningOpportunitySpecification.Specifies
            .LearningOpportunityInstance();
    i2.setLoiId("CRI/111211/" + los_id);

    try {
      XMLGregorianCalendar startDate =
          DatatypeFactory.newInstance().newXMLGregorianCalendar(2010, 1, 1, 0, 0, 0, 0, 0);
      startDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
      i2.setStart(startDate);

      XMLGregorianCalendar endDate =
          DatatypeFactory.newInstance().newXMLGregorianCalendar(2010, 6, 20, 0, 0, 0, 0, 0);
      endDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
      i2.setEnd(endDate);
    } catch (DatatypeConfigurationException e) {
      // This should not happen.
      assert (false);
    }

    specifies.getLearningOpportunityInstance().add(i2);

    data.setSpecifies(specifies);

    return data;
  }

  @Override
  public Response handleCoursesInternetRequest(Request request) {
    try {
      RequestData requestData = new RequestData(request);
      VerifyCertificate(requestData);
      CheckRequestMethod(requestData);
      ExtractParams(requestData);
      CheckHei(requestData);
      CheckIds(requestData);
      CheckCodes(requestData);
      List<CoursesResponse.LearningOpportunitySpecification> data1 = ProcessIds(requestData);
      List<CoursesResponse.LearningOpportunitySpecification> data2 = ProcessCodes(requestData);
      data1.addAll(data2);
      return createCoursesResponse(data1);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void VerifyCertificate(RequestData requestData) throws ErrorResponseException {
    try {
      this.myAuthorizer.authorize(requestData.request);
    } catch (Http4xx e) {
      throw new ErrorResponseException(
          e.generateEwpErrorResponse()
      );
    }
  }

  private void ExtractParams(RequestData requestData) throws ErrorResponseException {
    CheckParamsEncoding(requestData);
    Map<String, List<String>> params =
        InternetTestHelpers.extractAllParams(requestData.request);

    List<String> heiIds = params.getOrDefault("hei_id", new ArrayList<>());
    boolean hasHeiId = heiIds.size() > 0;
    boolean multipleHeiId = heiIds.size() > 1;

    requestData.losIds = params.getOrDefault("los_id", new ArrayList<>());
    boolean hasLosId = !requestData.losIds.isEmpty();

    requestData.losCodes = params.getOrDefault("los_code", new ArrayList<>());
    boolean hasLosCode = !requestData.losCodes.isEmpty();

    requestData.heiId = hasHeiId ? heiIds.get(0) : null;

    List<String> requestedLoisAfters = params.getOrDefault("lois_after", new ArrayList<>());
    boolean hasLoisAfter = !requestedLoisAfters.isEmpty();
    List<String> requestedLoisBefores = params.getOrDefault("lois_before", new ArrayList<>());
    boolean hasLoisBefore = !requestedLoisBefores.isEmpty();

    if (params.size() == 0) {
      ErrorNoParams(requestData);
    }
    if (!hasHeiId) {
      ErrorNoHeiId(requestData);
    }
    if (multipleHeiId) {
      ErrorMultipleHeiIds(requestData);
    }
    if (hasLosId && hasLosCode) {
      ErrorIdsAndCodes(requestData);
    }
    if (!hasLosId && !hasLosCode) {
      ErrorNoIdsNorCodes(requestData);
    }
    if (requestedLoisAfters.size() > 1) {
      ErrorMultipleLoisAfter(requestData);
    }
    if (requestedLoisBefores.size() > 1) {
      ErrorMultipleLoisBefore(requestData);
    }
    requestData.loisBefore = null;
    if (hasLoisBefore) {
      requestData.loisBefore = CheckDateFormat(requestData, requestedLoisBefores.get(0));
    }

    requestData.loisAfter = null;
    if (hasLoisAfter) {
      requestData.loisAfter = CheckDateFormat(requestData, requestedLoisAfters.get(0));
    }

    int expectedParams = 0;
    expectedParams += hasHeiId ? 1 : 0;
    expectedParams += hasLosCode ? 1 : 0;
    expectedParams += hasLosId ? 1 : 0;
    expectedParams += hasLoisAfter ? 1 : 0;
    expectedParams += hasLoisBefore ? 1 : 0;
    if (params.size() > expectedParams) {
      HandleUnexpectedParams(requestData);
    }

    if (requestData.heiId == null || requestData.losCodes == null
        || requestData.losIds == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected XMLGregorianCalendar CheckDateFormat(RequestData requestData, String date)
      throws ErrorResponseException {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    try {
      formatter.parse(date);
      return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
    } catch (DateTimeParseException | DatatypeConfigurationException e) {
      return ErrorDateFormat(requestData);
    }
  }

  private void CheckHei(RequestData requestData) throws ErrorResponseException {
    if (!CourseReplicationServiceV2.GetCoveredHeiIds().contains(requestData.heiId)) {
      ErrorUnknownHeiId(requestData);
    } else {
      HandleKnownHeiId(requestData);
    }
  }

  private void CheckCodes(RequestData requestData) throws ErrorResponseException {
    if (requestData.losCodes.size() > getMaxLosCodes()) {
      ErrorMaxLosCodesExceeded(requestData);
    }
  }

  private void CheckIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.losIds.size() > getMaxLosIds()) {
      ErrorMaxLosIdsExceeded(requestData);
    }
  }

  private List<CoursesResponse.LearningOpportunitySpecification> ProcessCodes(
      RequestData requestData) {
    return ProcessRequested(requestData, requestData.losCodes, coveredLosCodes);
  }

  private List<CoursesResponse.LearningOpportunitySpecification> ProcessIds(
      RequestData requestData) {
    return ProcessRequested(requestData, requestData.losIds, coveredLosIds);
  }

  private CoursesResponse.LearningOpportunitySpecification FilterEndDate(RequestData requestData,
      CoursesResponse.LearningOpportunitySpecification data) {
    List<CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance>
        instances =
        data.getSpecifies().getLearningOpportunityInstance().stream()
            .filter(loi -> isBefore(requestData, loi.getEnd())).collect(
            Collectors.toList());
    return LearningOpportunityWithChangedInstances(data, instances);
  }

  private CoursesResponse.LearningOpportunitySpecification FilterStartDate(RequestData requestData,
      CoursesResponse.LearningOpportunitySpecification data) {
    List<CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance>
        instances =
        data.getSpecifies().getLearningOpportunityInstance().stream()
            .filter(loi -> isAfter(requestData, loi.getStart())).collect(
            Collectors.toList());
    return LearningOpportunityWithChangedInstances(data, instances);
  }

  private CoursesResponse.LearningOpportunitySpecification LearningOpportunityWithChangedInstances(
      CoursesResponse.LearningOpportunitySpecification data,
      List<CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance>
          instances) {
    CoursesResponse.LearningOpportunitySpecification result =
        new CoursesResponse.LearningOpportunitySpecification();

    result.setOunitId(data.getOunitId());
    result.setLosId(data.getLosId());
    result.setLosCode(data.getLosCode());
    result.getTitle().addAll(data.getTitle());
    result.setContains(data.getContains());
    result.setEqfLevelProvided(data.getEqfLevelProvided());
    result.setIscedCode(data.getIscedCode());
    result.setSubjectArea(data.getSubjectArea());
    result.setType(data.getType());

    CoursesResponse.LearningOpportunitySpecification.Specifies specifies =
        new CoursesResponse.LearningOpportunitySpecification.Specifies();
    specifies.getLearningOpportunityInstance().addAll(instances);
    result.setSpecifies(specifies);

    return result;
  }

  private boolean isBefore(RequestData requestData, XMLGregorianCalendar end) {
    if (requestData.loisBefore == null) {
      return true;
    }
    return end.compare(requestData.loisBefore) < 0;
  }

  private boolean isAfter(RequestData requestData, XMLGregorianCalendar end) {
    if (requestData.loisAfter == null) {
      return true;
    }
    return end.compare(requestData.loisAfter) > 0;
  }

  private List<CoursesResponse.LearningOpportunitySpecification> ProcessRequested(
      RequestData requestData, List<String> requested,
      Map<String, CoursesResponse.LearningOpportunitySpecification> covered) {
    List<CoursesResponse.LearningOpportunitySpecification> ret = new ArrayList<>();
    for (String los : requested) {
      CoursesResponse.LearningOpportunitySpecification data = covered.get(los);
      if (data == null) {
        data = HandleUnknownLos(requestData);
      } else {
        data = HandleKnownLos(requestData, data);
      }
      if (data != null) {
        data = FilterEndDate(requestData, data);
        data = FilterStartDate(requestData, data);
      }
      if (data != null) {
        ret.add(data);
      }
    }
    return ret;
  }

  protected CoursesResponse.LearningOpportunitySpecification HandleKnownLos(RequestData requestData,
      CoursesResponse.LearningOpportunitySpecification data) {
    return data;
  }

  protected CoursesResponse.LearningOpportunitySpecification HandleUnknownLos(
      RequestData requestData) {
    return null;
  }

  protected void CheckRequestMethod(RequestData requestData) throws ErrorResponseException {
    if (!(requestData.request.getMethod().equals("GET") || requestData.request
        .getMethod()
        .equals("POST"))) {
      throw new ErrorResponseException(
          createErrorResponse(requestData.request, 405, "We expect GETs and POSTs only")
      );
    }
  }

  protected void CheckParamsEncoding(RequestData requestData) throws ErrorResponseException {
    if (requestData.request.getMethod().equals("POST")
        && !requestData.request.getHeader("content-type")
        .equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(requestData.request, 415, "Unsupported content-type")
      );
    }
  }

  protected void ErrorMaxLosCodesExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-los-codes exceeded")
    );
  }

  protected void ErrorMaxLosIdsExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-los-ids exceeded")
    );
  }

  protected void HandleKnownHeiId(RequestData requestData) {
    //Intentionally left empty
  }

  protected void ErrorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Unknown hei_id")
    );
  }

  protected void ErrorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one hei_id provided.")
    );
  }

  protected void ErrorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "los_id xor los_code is required.")
    );
  }

  protected void ErrorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(
            requestData.request, 400, "Only one of los_id and los_code should" +
                " be provided")
    );
  }

  protected void ErrorNoHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No hei_id parameter")
    );
  }

  protected void HandleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void ErrorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }

  protected XMLGregorianCalendar ErrorDateFormat(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid date format.")
    );
  }

  protected void ErrorMultipleLoisAfter(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple lois-after.")
    );
  }

  protected void ErrorMultipleLoisBefore(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple lois-before.")
    );
  }

  static class RequestData {
    Request request;
    String heiId;
    List<String> losIds;
    List<String> losCodes;
    XMLGregorianCalendar loisBefore;
    XMLGregorianCalendar loisAfter;

    RequestData(Request request) {
      this.request = request;
    }
  }
}
