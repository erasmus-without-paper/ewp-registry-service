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
import eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator.CourseReplicationServiceV1Valid;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_courses.tree.stable_v1.CoursesResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.StringWithOptionalLang;

public class CoursesServiceV070Valid extends AbstractCoursesService {
  protected final CourseReplicationServiceV1Valid CourseReplicationServiceV2;
  protected final int maxLosIds = 2;
  protected final int maxLosCodes = 2;
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
    return maxLosCodes;
  }

  protected int getMaxLosIds() {
    return maxLosIds;
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
          DatatypeFactory.newInstance()
              .newXMLGregorianCalendar(2011, 1, 1, 0, 0, 0, 0, getTimeZone());
      i1.setStart(startDate);

      XMLGregorianCalendar endDate =
          DatatypeFactory.newInstance()
              .newXMLGregorianCalendar(2011, 6, 20, 0, 0, 0, 0, getTimeZone());
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
          DatatypeFactory.newInstance()
              .newXMLGregorianCalendar(2010, 1, 1, 0, 0, 0, 0, getTimeZone());
      i2.setStart(startDate);

      XMLGregorianCalendar endDate =
          DatatypeFactory.newInstance()
              .newXMLGregorianCalendar(2010, 6, 20, 0, 0, 0, 0, getTimeZone());
      i2.setEnd(endDate);
    } catch (DatatypeConfigurationException e) {
      // This should not happen.
      assert (false);
    }

    specifies.getLearningOpportunityInstance().add(i2);

    data.setSpecifies(specifies);

    return data;
  }

  protected int getTimeZone() {
    return DatatypeConstants.FIELD_UNDEFINED;
  }

  @Override
  public Response handleCoursesInternetRequest(Request request) {
    try {
      RequestData requestData = new RequestData(request);
      verifyCertificate(requestData.request);
      checkRequestMethod(requestData.request);
      extractParams(requestData);
      checkHei(requestData);
      checkIds(requestData);
      checkCodes(requestData);
      List<CoursesResponse.LearningOpportunitySpecification> data1 = processIds(requestData);
      List<CoursesResponse.LearningOpportunitySpecification> data2 = processCodes(requestData);
      data1.addAll(data2);
      return createCoursesResponse(data1);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
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
      errorNoParams(requestData);
    }
    if (!hasHeiId) {
      errorNoHeiId(requestData);
    }
    if (multipleHeiId) {
      errorMultipleHeiIds(requestData);
    }
    if (hasLosId && hasLosCode) {
      errorIdsAndCodes(requestData);
    }
    if (!hasLosId && !hasLosCode) {
      errorNoIdsNorCodes(requestData);
    }
    if (requestedLoisAfters.size() > 1) {
      errorMultipleLoisAfter(requestData);
    }
    if (requestedLoisBefores.size() > 1) {
      errorMultipleLoisBefore(requestData);
    }
    requestData.loisBefore = null;
    if (hasLoisBefore) {
      requestData.loisBefore = checkDateFormat(requestData, requestedLoisBefores.get(0));
    }

    requestData.loisAfter = null;
    if (hasLoisAfter) {
      requestData.loisAfter = checkDateFormat(requestData, requestedLoisAfters.get(0));
    }

    int expectedParams = 0;
    expectedParams += hasHeiId ? 1 : 0;
    expectedParams += hasLosCode ? 1 : 0;
    expectedParams += hasLosId ? 1 : 0;
    expectedParams += hasLoisAfter ? 1 : 0;
    expectedParams += hasLoisBefore ? 1 : 0;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.heiId == null || requestData.losCodes == null
        || requestData.losIds == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected XMLGregorianCalendar checkDateFormat(RequestData requestData, String date)
      throws ErrorResponseException {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    try {
      formatter.parse(date);
      return DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
    } catch (DateTimeParseException | DatatypeConfigurationException e) {
      return errorDateFormat(requestData);
    }
  }

  private void checkHei(RequestData requestData) throws ErrorResponseException {
    if (!CourseReplicationServiceV2.getCoveredHeiIds().contains(requestData.heiId)) {
      errorUnknownHeiId(requestData);
    } else {
      handleKnownHeiId(requestData);
    }
  }

  private void checkCodes(RequestData requestData) throws ErrorResponseException {
    if (requestData.losCodes.size() > getMaxLosCodes()) {
      errorMaxLosCodesExceeded(requestData);
    }
  }

  private void checkIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.losIds.size() > getMaxLosIds()) {
      errorMaxLosIdsExceeded(requestData);
    }
  }

  private List<CoursesResponse.LearningOpportunitySpecification> processCodes(
      RequestData requestData) {
    return processRequested(requestData, requestData.losCodes, coveredLosCodes);
  }

  private List<CoursesResponse.LearningOpportunitySpecification> processIds(
      RequestData requestData) {
    return processRequested(requestData, requestData.losIds, coveredLosIds);
  }

  private CoursesResponse.LearningOpportunitySpecification filterEndDate(RequestData requestData,
      CoursesResponse.LearningOpportunitySpecification data) {
    List<CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance>
        instances =
        data.getSpecifies().getLearningOpportunityInstance().stream()
            .filter(loi -> isBefore(requestData, loi.getEnd())).collect(
            Collectors.toList());
    return learningOpportunityWithChangedInstances(data, instances);
  }

  private CoursesResponse.LearningOpportunitySpecification filterStartDate(RequestData requestData,
      CoursesResponse.LearningOpportunitySpecification data) {
    List<CoursesResponse.LearningOpportunitySpecification.Specifies.LearningOpportunityInstance>
        instances =
        data.getSpecifies().getLearningOpportunityInstance().stream()
            .filter(loi -> isAfter(requestData, loi.getStart())).collect(
            Collectors.toList());
    return learningOpportunityWithChangedInstances(data, instances);
  }

  private CoursesResponse.LearningOpportunitySpecification learningOpportunityWithChangedInstances(
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
    return end.toGregorianCalendar().compareTo(requestData.loisBefore.toGregorianCalendar()) < 0;
  }

  private boolean isAfter(RequestData requestData, XMLGregorianCalendar end) {
    if (requestData.loisAfter == null) {
      return true;
    }
    return end.toGregorianCalendar().compareTo(requestData.loisAfter.toGregorianCalendar()) > 0;
  }

  protected List<CoursesResponse.LearningOpportunitySpecification> processRequested(
      RequestData requestData, List<String> requested,
      Map<String, CoursesResponse.LearningOpportunitySpecification> covered) {
    List<CoursesResponse.LearningOpportunitySpecification> ret = new ArrayList<>();
    for (String los : requested) {
      CoursesResponse.LearningOpportunitySpecification data = covered.get(los);
      if (data == null) {
        data = handleUnknownLos(requestData);
      } else {
        data = handleKnownLos(requestData, data);
      }
      if (data != null) {
        data = filterEndDate(requestData, data);
        data = filterStartDate(requestData, data);
      }
      if (data != null) {
        ret.add(data);
      }
    }
    return ret;
  }

  protected CoursesResponse.LearningOpportunitySpecification handleKnownLos(RequestData requestData,
      CoursesResponse.LearningOpportunitySpecification data) {
    return data;
  }

  protected CoursesResponse.LearningOpportunitySpecification handleUnknownLos(
      RequestData requestData) {
    return null;
  }

  protected void errorMaxLosCodesExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-los-codes exceeded")
    );
  }

  protected void errorMaxLosIdsExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-los-ids exceeded")
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

  protected void errorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one hei_id provided.")
    );
  }

  protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "los_id xor los_code is required.")
    );
  }

  protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(
            requestData.request, 400, "Only one of los_id and los_code should" +
                " be provided")
    );
  }

  protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No hei_id parameter")
    );
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }

  protected XMLGregorianCalendar errorDateFormat(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid date format.")
    );
  }

  protected void errorMultipleLoisAfter(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple lois-after.")
    );
  }

  protected void errorMultipleLoisBefore(RequestData requestData) throws ErrorResponseException {
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
