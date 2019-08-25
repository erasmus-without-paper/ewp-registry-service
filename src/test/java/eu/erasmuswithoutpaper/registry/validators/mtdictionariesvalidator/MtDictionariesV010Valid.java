package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.types.MtDictionariesResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class MtDictionariesV010Valid extends AbstractMtDictionariesService {
  private final EwpHttpSigRequestAuthorizer myAuthorizer;

  protected static final class DictionaryAndYear {
    public final String dictionary;
    public final int year;

    public DictionaryAndYear(String dictionary, int year) {
      this.dictionary = dictionary;
      this.year = year;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DictionaryAndYear that = (DictionaryAndYear) o;
      return year == that.year &&
          Objects.equals(dictionary, that.dictionary);
    }

    @Override
    public int hashCode() {
      return Objects.hash(dictionary, year);
    }
  }

  protected Map<DictionaryAndYear, List<MtDictionariesResponse.Term>> coveredTerms =
      new HashMap<>();

  protected Set<String> coveredDictionaries = new HashSet<>();

  private MtDictionariesResponse.Term createTerm(String code, String description) {
    MtDictionariesResponse.Term term = new MtDictionariesResponse.Term();
    term.setCode(code);
    term.setDescription(description);
    return term;
  }

  private void fillCovered() {
    coveredTerms.put(new DictionaryAndYear("key_actions", 2019), Arrays.asList(
        createTerm("A", "Key Action 1"),
        createTerm("B", "Key Action 2"),
        createTerm("C", "Key Action 3"),
        createTerm("D", "Key Action 4"),
        createTerm("E", "Key Action 5")
    ));

    coveredTerms.put(new DictionaryAndYear("key_actions", 2018), Arrays.asList(
        createTerm("A", "Key Action 5"),
        createTerm("B", "Key Action 4"),
        createTerm("C", "Key Action 3"),
        createTerm("D", "Key Action 2"),
        createTerm("E", "Key Action 1")
    ));

    coveredTerms.put(new DictionaryAndYear("countries", 2019), Arrays.asList(
        createTerm("PL", "Poland"),
        createTerm("DE", "Germany"),
        createTerm("FR", "France"),
        createTerm("GB", "Great Britain"),
        createTerm("IR", "Ireland")
    ));

    coveredTerms.put(new DictionaryAndYear("countries", 2020), Arrays.asList(
        createTerm("PL", "Poland"),
        createTerm("DE", "Germany"),
        createTerm("FR", "France"),
        createTerm("IR", "Ireland")
    ));

    coveredDictionaries = coveredTerms.keySet().stream()
        .map(dictionaryAndYear -> dictionaryAndYear.dictionary)
        .collect(Collectors.toSet());
  }

  public MtDictionariesV010Valid(String url, RegistryClient registryClient) {
    super(url, registryClient);
    this.myAuthorizer = new EwpHttpSigRequestAuthorizer(this.registryClient);
    fillCovered();
  }

  static class RequestData {
    Request request;
    String dictionary;
    Integer callYear;

    RequestData(Request request) {
      this.request = request;
    }
  }

  @Override
  protected Response handleMtDictionariesRequest(
      Request request) throws IOException, ErrorResponseException {
    try {
      RequestData requestData = new RequestData(request);
      verifyCertificate(requestData);
      checkRequestMethod(requestData);
      extractParams(requestData);
      List<MtDictionariesResponse.Term> terms = processDictionaries(requestData);
      return createMtDictionariesReponse(terms);
    } catch (ErrorResponseException e) {
      return e.response;
    }

  }

  protected List<MtDictionariesResponse.Term> processDictionaries(
      RequestData requestData) throws ErrorResponseException {

    if (!coveredDictionaries.contains(requestData.dictionary)) {
      errorInvalidDictionary(requestData);
    }

    DictionaryAndYear dictionaryAndYear = new DictionaryAndYear(
        requestData.dictionary, requestData.callYear);

    return coveredTerms.getOrDefault(dictionaryAndYear, new ArrayList<>());

  }

  protected void errorInvalidDictionary(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No dictionary parameter")
    );
  }

  private void verifyCertificate(RequestData requestData) throws ErrorResponseException {
    try {
      this.myAuthorizer.authorize(requestData.request);
    } catch (Http4xx e) {
      throw new ErrorResponseException(
          e.generateEwpErrorResponse()
      );
    }
  }

  protected void checkRequestMethod(RequestData requestData) throws ErrorResponseException {
    if (!(requestData.request.getMethod().equals("GET")
        || requestData.request.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
          createErrorResponse(requestData.request, 405, "We expect GETs and POSTs only")
      );
    }
  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData);
    Map<String, List<String>> params =
        InternetTestHelpers.extractAllParams(requestData.request);

    List<String> dictionaries = params.getOrDefault("dictionary", new ArrayList<>());
    boolean hasDictionary = dictionaries.size() > 0;
    boolean multipleDictionaries = dictionaries.size() > 1;

    List<String> callYears = params.getOrDefault("call_year", new ArrayList<>());
    boolean hasCallYear = callYears.size() > 0;
    boolean multipleCallYears = callYears.size() > 1;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!hasDictionary) {
      errorNoDictionary(requestData);
    }
    if (!hasCallYear) {
      errorNoCallYear(requestData);
    }
    if (multipleCallYears) {
      errorMultipleCallYears(requestData);
    }
    if (multipleDictionaries) {
      errorMultipleDictionaries(requestData);
    }

    if (hasDictionary) {
      requestData.dictionary = dictionaries.get(0);
    }

    if (hasCallYear) {
      requestData.callYear = parseCallYear(requestData, callYears.get(0));
    }

    int expectedParams = 0;
    expectedParams += hasDictionary ? 1 : 0;
    expectedParams += hasCallYear ? 1 : 0;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.dictionary == null || requestData.callYear == null) {
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

  protected int additionalCallYearCheck(RequestData requestData,
      Integer parsed) throws ErrorResponseException {
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

  protected void handleCallYearNegative(
      RequestData requestData) throws ErrorResponseException {
    // do nothing
  }

  protected void checkParamsEncoding(RequestData requestData) throws ErrorResponseException {
    if (requestData.request.getMethod().equals("POST")
        && !requestData.request.getHeader("content-type")
        .equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(requestData.request, 415, "Unsupported content-type")
      );
    }
  }

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }

  protected void errorNoDictionary(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No dictionary parameter")
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

  protected void errorMultipleDictionaries(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple dictionary parameters")
    );
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }
}
