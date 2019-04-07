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
import eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator.CourseReplicationServiceV2Valid;
import eu.erasmuswithoutpaper.registry.validators.types.CoursesResponse;
import eu.erasmuswithoutpaper.registry.validators.types.StringWithOptionalLang;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class CoursesServiceV070Valid extends AbstractCoursesService {
  protected final CourseReplicationServiceV2Valid CourseReplicationServiceV2;
  private final EwpHttpSigRequestAuthorizer myAuthorizer;

  protected final int max_los_ids = 2;
  protected final int max_los_codes = 2;
  protected Map<String, CoursesResponse.LearningOpportunitySpecification> coveredLosIds =
      new HashMap<>();
  protected Map<String, CoursesResponse.LearningOpportunitySpecification> coveredLosCodes =
      new HashMap<>();
  protected List<CoursesResponse.LearningOpportunitySpecification> coveredLossList =
      new ArrayList<>();

  protected Request currentRequest;
  protected String requestedHeiId;
  protected List<String> requestedLosIds;
  protected List<String> requestedLosCodes;
  protected List<String> requestedHeiIds;
  private XMLGregorianCalendar requestedLoisBefore;
  private XMLGregorianCalendar requestedLoisAfter;

  public CoursesServiceV070Valid(String url, RegistryClient registryClient,
      CourseReplicationServiceV2Valid courseReplicationService) {
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
      currentRequest = request;
      VerifyCertificate();
      CheckRequestMethod();
      ExtractParams();
      CheckHei();
      CheckIds();
      CheckCodes();
      List<CoursesResponse.LearningOpportunitySpecification> data1 = ProcessIds();
      List<CoursesResponse.LearningOpportunitySpecification> data2 = ProcessCodes();
      data1.addAll(data2);
      return createCoursesResponse(data1);
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

    this.requestedLosIds = params.getOrDefault("los_id", new ArrayList<>());
    boolean hasLosId = !this.requestedLosIds.isEmpty();

    this.requestedLosCodes = params.getOrDefault("los_code", new ArrayList<>());
    boolean hasLosCode = !this.requestedLosCodes.isEmpty();

    this.requestedHeiId = hasHeiId ? this.requestedHeiIds.get(0) : null;

    List<String> requestedLoisAfters = params.getOrDefault("lois_after", new ArrayList<>());
    boolean hasLoisAfter = !requestedLoisAfters.isEmpty();
    List<String> requestedLoisBefores = params.getOrDefault("lois_before", new ArrayList<>());
    boolean hasLoisBefore = !requestedLoisBefores.isEmpty();

    if (params.size() == 0) {
      ErrorNoParams();
    }
    if (!hasHeiId) {
      ErrorNoHeiId();
    }
    if (multipleHeiId) {
      ErrorMultipleHeiIds();
    }
    if (hasLosId && hasLosCode) {
      ErrorIdsAndCodes();
    }
    if (!hasLosId && !hasLosCode) {
      ErrorNoIdsNorCodes();
    }
    if (requestedLoisAfters.size() > 1) {
      ErrorMultipleLoisAfter();
    }
    if (requestedLoisBefores.size() > 1) {
      ErrorMultipleLoisBefore();
    }
    this.requestedLoisBefore = null;
    if (hasLoisBefore) {
      this.requestedLoisBefore = CheckDateFormat(requestedLoisBefores.get(0));
    }

    this.requestedLoisAfter = null;
    if (hasLoisAfter) {
      this.requestedLoisAfter = CheckDateFormat(requestedLoisAfters.get(0));
    }

    int expectedParams = 0;
    expectedParams += hasHeiId ? 1 : 0;
    expectedParams += hasLosCode ? 1 : 0;
    expectedParams += hasLosId ? 1 : 0;
    expectedParams += hasLoisAfter ? 1 : 0;
    expectedParams += hasLoisBefore ? 1 : 0;
    if (params.size() > expectedParams) {
      HandleUnexpectedParams();
    }

    if (this.requestedHeiId == null || this.requestedLosCodes == null
        || this.requestedLosIds == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected XMLGregorianCalendar CheckDateFormat(String date) throws ErrorResponseException {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    try {
      formatter.parse(date);
      return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
    } catch (DateTimeParseException | DatatypeConfigurationException e) {
      return ErrorDateFormat();
    }
  }

  private void CheckHei() throws ErrorResponseException {
    if (!CourseReplicationServiceV2.GetCoveredHeiIds().contains(this.requestedHeiId)) {
      ErrorUnknownHeiId();
    } else {
      HandleKnownHeiId();
    }
  }

  private void CheckCodes() throws ErrorResponseException {
    if (this.requestedLosCodes.size() > getMaxLosCodes()) {
      ErrorMaxLosCodesExceeded();
    }
  }

  private void CheckIds() throws ErrorResponseException {
    if (this.requestedLosIds.size() > getMaxLosIds()) {
      ErrorMaxLosIdsExceeded();
    }
  }

  private List<CoursesResponse.LearningOpportunitySpecification> ProcessCodes() {
    return ProcessRequested(this.requestedLosCodes, coveredLosCodes);
  }

  private List<CoursesResponse.LearningOpportunitySpecification> ProcessIds() {
    return ProcessRequested(this.requestedLosIds, coveredLosIds);
  }

  private CoursesResponse.LearningOpportunitySpecification FilterEndDate(
      CoursesResponse.LearningOpportunitySpecification data) {
    List<CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance>
        instances =
        data.getSpecifies().getLearningOpportunityInstance().stream()
            .filter(loi -> isBefore(loi.getEnd())).collect(
            Collectors.toList());
    return LearningOpportunityWithChangedInstances(data, instances);
  }

  private CoursesResponse.LearningOpportunitySpecification FilterStartDate(
      CoursesResponse.LearningOpportunitySpecification data) {
    List<CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance>
        instances =
        data.getSpecifies().getLearningOpportunityInstance().stream()
            .filter(loi -> isAfter(loi.getStart())).collect(
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

  private boolean isBefore(XMLGregorianCalendar end) {
    if (this.requestedLoisBefore == null) {
      return true;
    }
    return end.compare(this.requestedLoisBefore) < 0;
  }

  private boolean isAfter(XMLGregorianCalendar end) {
    if (this.requestedLoisAfter == null) {
      return true;
    }
    return end.compare(this.requestedLoisAfter) > 0;
  }

  private List<CoursesResponse.LearningOpportunitySpecification> ProcessRequested(
      List<String> requested,
      Map<String, CoursesResponse.LearningOpportunitySpecification> covered) {
    List<CoursesResponse.LearningOpportunitySpecification> ret = new ArrayList<>();
    for (String los : requested) {
      CoursesResponse.LearningOpportunitySpecification data = covered.get(los);
      if (data == null) {
        data = HandleUnknownLos();
      } else {
        data = HandleKnownLos(data);
      }
      if (data != null) {
        data = FilterEndDate(data);
        data = FilterStartDate(data);
      }
      if (data != null) {
        ret.add(data);
      }
    }
    return ret;
  }

  protected CoursesResponse.LearningOpportunitySpecification HandleKnownLos(
      CoursesResponse.LearningOpportunitySpecification data) {
    return data;
  }

  protected CoursesResponse.LearningOpportunitySpecification HandleUnknownLos() {
    return null;
  }

  protected void CheckRequestMethod() throws ErrorResponseException {
    if (!(this.currentRequest.getMethod().equals("GET") || this.currentRequest.getMethod()
        .equals("POST"))) {
      throw new ErrorResponseException(
          this.createErrorResponse(this.currentRequest, 405, "We expect GETs and POSTs only")
      );
    }
  }

  protected void CheckParamsEncoding() throws ErrorResponseException {
    if (this.currentRequest.getMethod().equals("POST")
        && !this.currentRequest.getHeader("content-type")
        .equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(this.currentRequest, 415, "Unsupported content-type")
      );
    }
  }


  protected void ErrorMaxLosCodesExceeded() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "max-los-codes exceeded")
    );
  }

  protected void ErrorMaxLosIdsExceeded() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "max-los-ids exceeded")
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
        createErrorResponse(this.currentRequest, 400, "los_id xor los_code is required.")
    );
  }

  protected void ErrorIdsAndCodes() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Only one of los_id and los_code should" +
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

  protected void ErrorNoParams() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "No parameters provided")
    );
  }

  protected XMLGregorianCalendar ErrorDateFormat() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Invalid date format.")
    );
  }

  protected void ErrorMultipleLoisAfter() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Multiple lois-after.")
    );
  }

  protected void ErrorMultipleLoisBefore() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Multiple lois-before.")
    );
  }
}
