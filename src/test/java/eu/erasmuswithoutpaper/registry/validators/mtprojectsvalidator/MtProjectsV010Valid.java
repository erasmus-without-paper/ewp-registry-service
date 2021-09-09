package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_mt_projects.tree.stable_v1.MtProjectsResponse;

public class MtProjectsV010Valid extends AbstractMtProjectsService {

  protected Map<String, List<MtProjectsResponse.Project>> coveredProjects = new HashMap<>();

  private void fillCovered() {
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      return;
    }

    final String TEST_PIC = "999572294";

    MtProjectsResponse.Project project1 = new MtProjectsResponse.Project();
    project1.setStartDate(datatypeFactory.newXMLGregorianCalendarDate(2017, 1, 20, 0));
    project1.setEndDate(datatypeFactory.newXMLGregorianCalendarDate(2019, 8, 2, 0));
    project1.setActionType("action-type-1");
    project1.setAgreementNumber("agreement-number-1");

    MtProjectsResponse.Project project2 = new MtProjectsResponse.Project();
    project2.setStartDate(datatypeFactory.newXMLGregorianCalendarDate(2015, 1, 20, 0));
    project2.setEndDate(datatypeFactory.newXMLGregorianCalendarDate(2016, 8, 2, 0));
    project2.setActionType("action-type-2");
    project2.setAgreementNumber("agreement-number-2");

    MtProjectsResponse.Project project3 = new MtProjectsResponse.Project();
    project3.setStartDate(datatypeFactory.newXMLGregorianCalendarDate(2017, 3, 1, 0));
    project3.setEndDate(datatypeFactory.newXMLGregorianCalendarDate(2021, 2, 10, 0));
    project3.setActionType("action-type-3");
    project3.setAgreementNumber("agreement-number-3");

    coveredProjects.put(TEST_PIC, Arrays.asList(project1, project2));
    coveredProjects.put("other-pic", Arrays.asList(project3));
  }

  public MtProjectsV010Valid(String url, RegistryClient registryClient) {
    super(url, registryClient);
    fillCovered();
  }

  static class RequestData {
    Request request;
    String pic;
    Integer callYear;

    RequestData(Request request) {
      this.request = request;
    }
  }

  @Override
  protected Response handleMtProjectsRequest(
      Request request) throws IOException, ErrorResponseException {
    try {
      RequestData requestData = new RequestData(request);
      verifyCertificate(requestData.request);
      checkRequestMethod(requestData.request);
      extractParams(requestData);
      List<MtProjectsResponse.Project> projects = processPics(requestData);
      return createMtProjectsReponse(projects);
    } catch (ErrorResponseException e) {
      return e.response;
    }

  }

  protected List<MtProjectsResponse.Project> processPics(
      RequestData requestData) throws ErrorResponseException {
    if (!coveredProjects.containsKey(requestData.pic)) {
      errorInvalidPic(requestData);
    }
    List<MtProjectsResponse.Project> projects = coveredProjects
        .getOrDefault(requestData.pic, new ArrayList<>());

    return projects.stream().filter(project -> matchesCallYear(project, requestData.callYear))
        .collect(Collectors.toList());
  }

  protected void errorInvalidPic(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No pic parameter")
    );
  }

  private boolean matchesCallYear(MtProjectsResponse.Project project, Integer callYear) {
    return project.getStartDate().getYear() <= callYear
        && callYear <= project.getEndDate().getYear();
  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params =
        InternetTestHelpers.extractAllParams(requestData.request);

    List<String> pics = params.getOrDefault("pic", new ArrayList<>());
    boolean hasPic = pics.size() > 0;
    boolean multiplePics = pics.size() > 1;

    List<String> callYears = params.getOrDefault("call_year", new ArrayList<>());
    boolean hasCallYear = callYears.size() > 0;
    boolean multipleCallYears = callYears.size() > 1;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!hasPic) {
      errorNoPic(requestData);
    }
    if (!hasCallYear) {
      errorNoCallYear(requestData);
    }
    if (multipleCallYears) {
      errorMultipleCallYears(requestData);
    }
    if (multiplePics) {
      errorMultiplePics(requestData);
    }

    if (hasPic) {
      requestData.pic = pics.get(0);
    }

    if (hasCallYear) {
      requestData.callYear = parseCallYear(requestData, callYears.get(0));
    }

    int expectedParams = 0;
    expectedParams += hasPic ? 1 : 0;
    expectedParams += hasCallYear ? 1 : 0;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.pic == null || requestData.callYear == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected int parseCallYear(RequestData requestData,
      String callYear) throws ErrorResponseException {
    int parsed;
    try {
      parsed = Integer.parseInt(callYear);
    } catch (NumberFormatException e) {
      return errorInvalidCallYearFormat(requestData);
    }

    if (parsed == 0) {
      handleCallYearZero(requestData);
    }

    if (parsed < 0) {
      handleCallYearNegative(requestData);
    }

    return additionalCallYearCheck(requestData, parsed);
  }

  protected int additionalCallYearCheck(RequestData requestData, Integer parsed) throws ErrorResponseException {
    return parsed;
  }

  protected int errorInvalidCallYearFormat(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid call_year format")
    );
  }

  protected void handleCallYearZero(RequestData requestData) throws ErrorResponseException {
    // do nothing
  }

  protected void handleCallYearNegative(RequestData requestData) throws ErrorResponseException {
    // do nothing
  }

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }

  protected void errorNoPic(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No pic parameter")
    );
  }

  protected void errorNoCallYear(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No call_year parameter")
    );
  }

  protected void errorMultipleCallYears(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple call_year parameters")
    );
  }

  protected void errorMultiplePics(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple pic parameters")
    );
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }
}
