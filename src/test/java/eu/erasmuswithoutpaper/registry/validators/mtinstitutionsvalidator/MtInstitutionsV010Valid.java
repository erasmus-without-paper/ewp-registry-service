package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import static javax.xml.datatype.DatatypeConstants.GREATER;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_mt_institutions.tree.stable_v1.MtInstitutionsResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.StringWithOptionalLang;

public class MtInstitutionsV010Valid extends AbstractMtInstitutionsService {
  protected static final int maxIds = 2;

  protected List<MtInstitutionsResponse.Hei> coveredPics = new ArrayList<>();

  private void fillCovered() {
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      return;
    }

    final String TEST_PIC = "999572294";
    final String TEST_ERASMUS = "PL WARSZAW01";

    MtInstitutionsResponse.Hei hei1 = new MtInstitutionsResponse.Hei();
    hei1.setPic(TEST_PIC);
    hei1.setErasmus(TEST_ERASMUS);

    MtInstitutionsResponse.Hei.ErasmusCharter hei1Charter =
        new MtInstitutionsResponse.Hei.ErasmusCharter();

    hei1Charter.setEndDate(datatypeFactory.newXMLGregorianCalendarDate(2019, 7, 1, 0));
    hei1Charter.setStartDate(datatypeFactory.newXMLGregorianCalendarDate(2017, 1, 1, 0));
    hei1Charter.setValue("hei1-erasmus-charter-1");
    hei1.setErasmusCharter(hei1Charter);

    StringWithOptionalLang hei1Name = new StringWithOptionalLang();
    hei1Name.setValue("hei1");
    hei1.getName().add(hei1Name);
    coveredPics.add(hei1);


    MtInstitutionsResponse.Hei hei2 = new MtInstitutionsResponse.Hei();
    hei2.setPic(TEST_PIC);
    hei2.setErasmus(TEST_ERASMUS);

    MtInstitutionsResponse.Hei.ErasmusCharter hei2Charter =
        new MtInstitutionsResponse.Hei.ErasmusCharter();

    hei2Charter.setEndDate(datatypeFactory.newXMLGregorianCalendarDate(2016, 7, 1, 0));
    hei2Charter.setStartDate(datatypeFactory.newXMLGregorianCalendarDate(2014, 1, 1, 0));
    hei2Charter.setValue("hei2-erasmus-charter-1");
    hei2.setErasmusCharter(hei2Charter);

    StringWithOptionalLang hei2Name = new StringWithOptionalLang();
    hei2Name.setValue("hei2");
    hei2.getName().add(hei2Name);
    coveredPics.add(hei2);


    MtInstitutionsResponse.Hei hei3 = new MtInstitutionsResponse.Hei();
    hei3.setPic("other-pic");
    hei3.setErasmus("other-erasmus");

    MtInstitutionsResponse.Hei.ErasmusCharter hei3Charter =
        new MtInstitutionsResponse.Hei.ErasmusCharter();

    hei3Charter.setEndDate(datatypeFactory.newXMLGregorianCalendarDate(2016, 7, 1, 0));
    hei3Charter.setStartDate(datatypeFactory.newXMLGregorianCalendarDate(2014, 1, 1, 0));
    hei3Charter.setValue("hei3-erasmus-charter-1");
    hei3.setErasmusCharter(hei3Charter);

    StringWithOptionalLang hei3Name = new StringWithOptionalLang();
    hei3Name.setValue("hei3");
    hei3.getName().add(hei3Name);
    coveredPics.add(hei3);
  }

  public MtInstitutionsV010Valid(String url, RegistryClient registryClient) {
    super(url, registryClient);
    fillCovered();
  }

  protected int getMaxIds() {
    return maxIds;
  }

  static class RequestData {
    Request request;
    List<String> pic;
    XMLGregorianCalendar echeAtDate;

    RequestData(Request request) {
      this.request = request;
    }
  }

  @Override
  protected Response handleMtInstitutionsRequest(
      Request request) throws IOException, ErrorResponseException {
    try {
      RequestData requestData = new RequestData(request);
      verifyCertificate(requestData.request);
      checkRequestMethod(requestData.request);
      extractParams(requestData);
      checkPics(requestData);
      List<MtInstitutionsResponse.Hei> heis = processPics(requestData);
      return createMtInstitutionsReponse(heis);
    } catch (ErrorResponseException e) {
      return e.response;
    }

  }

  protected List<MtInstitutionsResponse.Hei> processPics(
      RequestData requestData) throws ErrorResponseException {
    List<MtInstitutionsResponse.Hei> result = new ArrayList<>();
    for (String pic : requestData.pic) {
      result.addAll(
          this.coveredPics.stream().filter(
              hei -> hei.getPic().equals(pic)
                  && dateMatches(hei.getErasmusCharter(), requestData.echeAtDate)
          ).collect(Collectors.toList())
      );
    }
    return result;
  }

  private boolean dateMatches(MtInstitutionsResponse.Hei.ErasmusCharter erasmusCharter,
      XMLGregorianCalendar echeAtDate) {
    return erasmusCharter.getStartDate().compare(echeAtDate) != GREATER
        && echeAtDate.compare(erasmusCharter.getEndDate()) != GREATER;

  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params =
        InternetTestHelpers.extractAllParams(requestData.request);

    requestData.pic = params.getOrDefault("pic", new ArrayList<>());
    boolean hasPics = requestData.pic.size() > 0;

    List<String> echeAtDates = params.getOrDefault("eche_at_date", new ArrayList<>());
    boolean hasEcheAtDate = echeAtDates.size() > 0;
    boolean multipleEcheAtDate = echeAtDates.size() > 1;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!hasPics) {
      errorNoPic(requestData);
    }
    if (!hasEcheAtDate) {
      errorNoEcheAtDate(requestData);
    }
    if (multipleEcheAtDate) {
      errorMultipleEcheAtDates(requestData);
    }

    if (hasEcheAtDate) {
      requestData.echeAtDate = checkDateFormat(requestData, echeAtDates.get(0));
    }

    int expectedParams = 0;
    expectedParams += hasPics ? 1 : 0;
    expectedParams += hasEcheAtDate ? 1 : 0;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.pic == null || requestData.echeAtDate == null) {
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

  protected XMLGregorianCalendar errorDateFormat(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid date format.")
    );
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

  protected void errorNoEcheAtDate(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No eche_at_date parameter")
    );
  }

  protected void errorMultipleEcheAtDates(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple eche_at_date parameters")
    );
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }


  private void checkPics(RequestData requestData) throws ErrorResponseException {
    if (requestData.pic.size() > getMaxIds()) {
      errorMaxPicsExceeded(requestData);
    }
  }

  protected void errorMaxPicsExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "<max-ids> exceeded")
    );
  }
}
