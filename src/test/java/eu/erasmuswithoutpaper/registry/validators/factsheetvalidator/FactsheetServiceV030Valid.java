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
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.types.CalendarEntry;
import eu.erasmuswithoutpaper.registry.validators.types.InformationEntry;
import eu.erasmuswithoutpaper.registry.validators.types.FactsheetResponse;
import eu.erasmuswithoutpaper.registry.validators.types.PhoneNumber;
import eu.erasmuswithoutpaper.registry.validators.types.FactsheetResponse.Factsheet.Accessibility;
import eu.erasmuswithoutpaper.registry.validators.types.HTTPWithOptionalLang;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;


public class FactsheetServiceV030Valid extends AbstractFactsheetService {
  protected static final int maxHeiIds = 2;
  private final EwpHttpSigRequestAuthorizer myAuthorizer;
  protected Map<String, FactsheetResponse.Factsheet> coveredHeis = new HashMap<>();

  public FactsheetServiceV030Valid(String url, RegistryClient registryClient,
      ValidatorKeyStore validatorKeyStore) {
    super(url, registryClient);
    this.myAuthorizer = new EwpHttpSigRequestAuthorizer(this.registryClient);
    for (String heiId : validatorKeyStore.getCoveredHeiIDs()) {
      addHei(createFactsheet(heiId));
    }
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
    factsheet.setDecisionWeeksLimit(new BigInteger("2"));
    factsheet.setTorWeeksLimit(new BigInteger("3"));
    Accessibility accessability = new Accessibility();
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

  private void verifyCertificate(Request request) throws ErrorResponseException {
    try {
      this.myAuthorizer.authorize(request);
    } catch (Http4xx e) {
      throw new ErrorResponseException(e.generateEwpErrorResponse());
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

  protected void checkRequestMethod(Request request) throws ErrorResponseException {
    if (!(request.getMethod().equals("GET") || request.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 405, "We expect GETs and POSTs only")
      );
    }
  }

  protected void checkParamsEncoding(Request request) throws ErrorResponseException {
    if (request.getMethod().equals("POST")
        && !request.getHeader("content-type").equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(request, 415, "Unsupported content-type")
      );
    }
  }

}
