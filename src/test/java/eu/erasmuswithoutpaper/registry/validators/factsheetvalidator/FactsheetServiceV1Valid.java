package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_factsheet.tree.stable_v1.CalendarEntry;
import https.github_com.erasmus_without_paper.ewp_specs_api_factsheet.tree.stable_v1.FactsheetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_factsheet.tree.stable_v1.InformationEntry;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.HTTPWithOptionalLang;
import https.github_com.erasmus_without_paper.ewp_specs_types_phonenumber.tree.stable_v1.PhoneNumber;


public class FactsheetServiceV1Valid extends AbstractFactsheetService {
  protected static final int maxHeiIds = 2;
  protected Map<String, FactsheetResponse.Factsheet> coveredHeis = new HashMap<>();

  public FactsheetServiceV1Valid(String url, RegistryClient registryClient, String heiId) {
    super(url, registryClient);
    addHei(createFactsheet(heiId));
  }

  private void addHei(FactsheetResponse.Factsheet data) {
    coveredHeis.put(data.getHeiId(), data);
  }

  protected XMLGregorianCalendar getTermDate(String month, String day){
    try {
      return DatatypeFactory.newInstance()
          .newXMLGregorianCalendar("2014-" + month + "-" + day);
    } catch (DatatypeConfigurationException e) {
      return null;
    }
  }

  protected FactsheetResponse.Factsheet createFactsheet(String heiId) {
    FactsheetResponse.Factsheet factsheet = new FactsheetResponse.Factsheet();
    factsheet.setHeiId(heiId);
    FactsheetResponse.Factsheet.Calendar calendar = new FactsheetResponse.Factsheet.Calendar();
    CalendarEntry calendarEntry = new CalendarEntry();
    calendarEntry.setAutumnTerm(getTermDate("10", "04"));
    calendarEntry.setSpringTerm(getTermDate("04", "10"));
    calendar.setStudentApplications(calendarEntry);
    calendar.setStudentNominations(calendarEntry);
    factsheet.setCalendar(calendar);
    InformationEntry informationEntry = new InformationEntry();
    informationEntry.setEmail("test-email@example.invalid");
    PhoneNumber phoneNumber = new PhoneNumber();
    phoneNumber.setE164("+123456789");
    informationEntry.setPhoneNumber(phoneNumber);
    HTTPWithOptionalLang url = new HTTPWithOptionalLang();
    url.setValue("http://test.url.com");
    url.setLang("en");
    informationEntry.getWebsiteUrl().add(url);
    factsheet.setApplicationInfo(informationEntry);
    factsheet.setDecisionWeeksLimit(new BigInteger("12"));
    factsheet.setTorWeeksLimit(new BigInteger("123"));
    FactsheetResponse.Factsheet.Accessibility accessability = new FactsheetResponse.Factsheet.Accessibility();
    accessability.setName("test");
    accessability.setType("service");
    accessability.setInformation(informationEntry);
    factsheet.getAccessibility().add(accessability);
    factsheet.setHousingInfo(informationEntry);
    factsheet.setVisaInfo(informationEntry);
    factsheet.setInsuranceInfo(informationEntry);
    return factsheet;
  }

  @Override
  public Response handleFactsheetInternetRequest(Request request) {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    try {
      verifyCertificate(request);
      checkRequestMethod(request);
      RequestData requestData = new RequestData(request);
      extractParams(requestData);
      checkHeis(requestData);
      List<FactsheetResponse.Factsheet> heisData = processHeis(requestData);
      return createFactsheetResponse(heisData);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected void checkHeis(RequestData requestData) throws ErrorResponseException {
    if (requestData.heis.size() > maxHeiIds) {
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
      handleNoHeiIdParam(requestData);
    }
    if (params.size() > 1) {
      handleMultipleParams(requestData);
    }
    List<String> heiIds = params.get("hei_id");
    if (heiIds != null) {
      requestData.heis = heiIds;
    } else {
      requestData.heis = new ArrayList<>();
    }
  }

  protected void handleMultipleParams(RequestData requestData)
      throws ErrorResponseException {
    //Ignore unknown parameters
  }

  protected void handleNoHeiIdParam(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Expected \"hei_id\" parameters"));
  }

  protected void handleNoParams(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided"));
  }

  protected FactsheetResponse.Factsheet processCoveredHei(RequestData requestData, String hei)
      throws ErrorResponseException {
    return coveredHeis.get(hei);
  }

  protected FactsheetResponse.Factsheet processNotCoveredHei(RequestData requestData, String hei)
      throws ErrorResponseException {
    //Ignore
    return null;
  }

  protected List<FactsheetResponse.Factsheet> processHeis(RequestData requestData)
      throws ErrorResponseException {
    List<FactsheetResponse.Factsheet> result = new ArrayList<>();
    for (String hei : requestData.heis) {
      FactsheetResponse.Factsheet factsheetForHei;
      if (coveredHeis.containsKey(hei)) {
        factsheetForHei = processCoveredHei(requestData, hei);
      } else {
        factsheetForHei = processNotCoveredHei(requestData, hei);
      }

      if (factsheetForHei != null) {
        result.add(factsheetForHei);
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
    List<String> heis;

    RequestData(Request request) {
      this.request = request;
    }
  }
}
