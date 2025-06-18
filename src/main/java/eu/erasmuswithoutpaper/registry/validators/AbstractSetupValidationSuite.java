package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.HttpSecuritySettings;
import eu.erasmuswithoutpaper.registry.validators.githubtags.GitHubTagsGetter;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.w3c.dom.Element;


public abstract class AbstractSetupValidationSuite<S extends SuiteState>
    extends AbstractValidationSuite<S> {
  private final GitHubTagsGetter gitHubTagsGetter;
  private final boolean getRequestsForbidden;

  protected AbstractSetupValidationSuite(
      ApiValidator<S> validator,
      S state,
      ValidationSuiteConfig config,
      boolean getRequestsForbidden) {
    super(validator, state, config);
    this.gitHubTagsGetter = config.gitHubTagsGetter;
    this.getRequestsForbidden = getRequestsForbidden;
  }

  protected static HttpSecurityDescription getDescriptionFromSecuritySettings(
      HttpSecuritySettings securitySettings) {
    CombEntry cliauth = CombEntry.CLIAUTH_NONE;
    if (securitySettings.supportsCliAuthHttpSig()) {
      cliauth = CombEntry.CLIAUTH_HTTPSIG;
    } else if (securitySettings.supportsCliAuthNone()) {
      cliauth = CombEntry.CLIAUTH_NONE;
    }

    CombEntry srvauth = CombEntry.SRVAUTH_TLSCERT;
    if (securitySettings.supportsSrvAuthTlsCert()) {
      srvauth = CombEntry.SRVAUTH_TLSCERT;
    } else if (securitySettings.supportsSrvAuthHttpSig()) {
      srvauth = CombEntry.SRVAUTH_HTTPSIG;
    }

    CombEntry reqencr = CombEntry.REQENCR_TLS;
    if (securitySettings.supportsReqEncrTls()) {
      reqencr = CombEntry.REQENCR_TLS;
    } else if (securitySettings.supportsReqEncrEwp()) {
      reqencr = CombEntry.REQENCR_EWP;
    }

    CombEntry resencr = CombEntry.RESENCR_TLS;
    if (securitySettings.supportsResEncrTls()) {
      resencr = CombEntry.RESENCR_TLS;
    } else if (securitySettings.supportsResEncrEwp()) {
      resencr = CombEntry.RESENCR_EWP;
    }

    return new HttpSecurityDescription(cliauth, srvauth, reqencr, resencr);
  }

  @Override
  protected void runTests(HttpSecurityDescription security)
      throws SuiteBroken {
    runCredentialsDateTest();
    runHttpsSchemeTest();
    runCheckUrlAndVersionTest();
    validateSecurityMethods();
    checkApiVersion();
    runApiSpecificTests(security);
  }

  private List<SemanticVersion> getGitHubTags() {
    return this.gitHubTagsGetter.getTags(
        getApiInfo().getGitHubRepositoryName(),
        this.internet,
        getLogger());
  }

  protected void runApiSpecificTests(HttpSecurityDescription security) throws SuiteBroken {
    //intentionally left empty
  }

  protected List<String> getCoveredHeiIds(String url) throws SuiteBroken {
    List<String> heis = new ArrayList<>();
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Check if this host covers any HEI.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        List<String> coveredHeiIds = fetchHeiIdsCoveredByApiByUrl(url);
        if (coveredHeiIds.isEmpty()) {
          throw new InlineValidationStep.Failure(
              "Manifest file doesn't contain any <hei-id> field covered by this URL. We cannot "
                  + "preform tests.",
              ValidationStepWithStatus.Status.NOTICE, null
          );
        }
        heis.addAll(coveredHeiIds);
        return Optional.empty();
      }
    });

    return heis;
  }


  private void checkApiVersion() throws SuiteBroken {
    final List<SemanticVersion> tags = getGitHubTags();
    if (tags.isEmpty()) {
      return;
    }

    final SemanticVersion expectedVersion = this.currentState.version;
    final boolean isCorrect = tags.contains(expectedVersion);
    final Optional<SemanticVersion> newVersion = tags.stream().filter(
        version -> version.compareTo(expectedVersion) > 0 && !version.isReleaseCandidate()
    ).max(Comparator.naturalOrder());

    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Verifying API version.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        if (!isCorrect) {
          throw new Failure(
              "API version " + currentState.version.toString()
                  + " is not valid. It's not listed as a tag in GitHub.",
              Status.FAILURE, null
          );
        }
        if (newVersion.isPresent()) {
          throw new Failure(
              "There is a new version of this API available (" + newVersion.get().toString()
                  + "). Consider upgrading your "
                  + "implementation.",
              Status.NOTICE, null
          );
        }
        return Optional.empty();
      }
    });
  }

  protected void runCredentialsDateTest() throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Check if our client credentials have been served long enough.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Date credentialsGenerationDate = validatorKeyStore.getCredentialsGenerationDate();
        if (credentialsGenerationDate == null) {
          return Optional.empty();
        }
        if (new Date().getTime() - credentialsGenerationDate.getTime() < 10 * 60 * 1000) {
          throw new Failure(
              "Our client credentials are quite fresh. This means that many APIs will "
                  + "(correctly) return error responses in places where we expect HTTP 200. "
                  + "This notice will disappear once our credentials are 10 minutes old.",
              Status.NOTICE,
              null
          );
        }
        return Optional.empty();
      }
    });
  }

  protected void runHttpsSchemeTest() throws SuiteBroken {
    this.addAndRun(true, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Verifying the format of the URL. Expecting a valid HTTPS-scheme URL.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        if (!currentState.url.startsWith("https://")) {
          throw new Failure("It needs to be HTTPS.", Status.FAILURE, null);
        }
        try {
          new URL(currentState.url);
        } catch (MalformedURLException e) {
          throw new Failure("Exception while parsing URL format: " + e, Status.FAILURE, null);
        }
        return Optional.empty();
      }
    });

  }

  protected void validateSecurityMethods() throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Querying for supported security methods. Validating http-security integrity.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {

        List<String> notices = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Parse http-security element. Record all warnings.

        HttpSecuritySettings sec = getSecuritySettings();
        notices.addAll(sec.getNotices());

        // Generate all possible combinations of validatable security methods.
        populateCombinationsToValidate(
            getClientAuthenticationMethods(sec, notices, warnings, errors),
            getServerAuthenticationMethods(sec, notices, warnings, errors),
            getRequestEncryptionMethods(sec, notices, warnings, errors),
            getResponseEncryptionMethods(sec, notices, warnings, errors)
        );

        // Determine the status. If not success, then raise a proper exception.
        checkAuthErrors(errors, warnings, notices);

        return Optional.empty();
      }
    });
  }

  private void populateCombinationsToValidate(List<CombEntry> cliAuthMethodsToValidate,
      List<CombEntry> srvAuthMethodsToValidate, List<CombEntry> reqEncrMethodsToValidate,
      List<CombEntry> resEncrMethodsToValidate) {
    for (CombEntry cliauth : cliAuthMethodsToValidate) {
      for (CombEntry srvauth : srvAuthMethodsToValidate) {
        for (CombEntry reqencr : reqEncrMethodsToValidate) {
          for (CombEntry resencr : resEncrMethodsToValidate) {
            boolean supportsGetRequests = true;
            if (reqencr.equals(CombEntry.REQENCR_EWP)) {
              supportsGetRequests = false;
            }
            if (supportsGetRequests && !getRequestsForbidden) {
              this.currentState.combinations.add(
                  new Combination("GET", currentState.url,
                      getMatchedApiEntry(), cliauth, srvauth, reqencr, resencr
                  ));
            }
            this.currentState.combinations.add(
                new Combination("POST", currentState.url,
                    getMatchedApiEntry(), cliauth, srvauth, reqencr, resencr
                ));
          }
        }
      }
    }
  }

  protected void runCheckUrlAndVersionTest() throws SuiteBroken {
    this.addAndRun(true, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Verifying if the URL is properly registered.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        ApiSearchConditions apiSearchConditions = new ApiSearchConditions();
        apiSearchConditions.setApiClassRequired(
            getApiInfo().getApiNamespace(),
            getApiInfo().getApiName(),
            currentState.version.toString()
        );
        currentState.apiSearchConditions = apiSearchConditions;

        try {
          getMatchedApiEntry();
        } catch (InvalidNumberOfApiEntries exception) {
          int matchedApiEntries = exception.getMatchedApiEntries();
          if (matchedApiEntries == 0) {
            throw new Failure("Could not find this URL and version in the Registry Catalogue. "
                    + "Make sure that it is properly registered "
                    + "(as declared in API's `manifest-entry.xsd` file): "
                    + getUrlElementName() + " " + currentState.url + ", v"
                    + currentState.version.toString(),
                    Status.FAILURE, null);
          }

          if (matchedApiEntries > 1) {
            throw new Failure("Multiple (" + Integer.toString(matchedApiEntries)
                    + ") API entries found for this URL and version.", Status.FAILURE, null);
          }
        }

        return Optional.empty();
      }
    });
  }

  protected List<CombEntry> getResponseEncryptionMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsResEncrTls()) {
      ret.add(CombEntry.RESENCR_TLS);
    }
    if (sec.supportsResEncrEwp()) {
      ret.add(CombEntry.RESENCR_EWP);
      warnings.add("It is RECOMMENDED to support only TLS response encryption");
    }
    if (ret.size() == 0) {
      errors.add("Your API does not support ANY of the response encryption "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  protected List<CombEntry> getRequestEncryptionMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsReqEncrTls()) {
      ret.add(CombEntry.REQENCR_TLS);
    }
    if (sec.supportsReqEncrEwp()) {
      ret.add(CombEntry.REQENCR_EWP);
      warnings.add("It is RECOMMENDED to support only TLS request encryption");
    }
    if (ret.size() == 0) {
      errors.add("Your API does not support ANY of the request encryption "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  protected List<CombEntry> getServerAuthenticationMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsSrvAuthTlsCert()) {
      ret.add(CombEntry.SRVAUTH_TLSCERT);
    }
    if (sec.supportsSrvAuthHttpSig()) {
      ret.add(CombEntry.SRVAUTH_HTTPSIG);
      if (!sec.supportsSrvAuthTlsCert()) {
        warnings.add("Server which support HTTP Signature Server Authentication "
            + "SHOULD also support TLS Server Certificate Authentication");
      }
    } else {
      notices.add("It is RECOMMENDED for all servers to support "
          + "HTTP Signature Server Authentication.");
    }
    if (ret.size() == 0) {
      errors.add("Your API does not support ANY of the server authentication "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return true;
  }

  protected List<CombEntry> getClientAuthenticationMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (!sec.supportsCliAuthNone() && shouldAnonymousClientBeAllowedToAccessThisApi()) {
      notices.add("You may consider allowing this API to be accessed by anonymous clients.");
    } else {
      ret.add(CombEntry.CLIAUTH_NONE);
    }

    if (sec.supportsCliAuthHttpSig()) {
      ret.add(CombEntry.CLIAUTH_HTTPSIG);
    } else {
      warnings.add("It is RECOMMENDED for all EWP server endpoints to support HTTP "
          + "Signature Client Authentication. Your endpoint doesn't.");
    }

    if (ret.size() < 1) {
      errors.add("Your API does not support ANY of the client authentication "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  protected HttpSecurityDescription getSecurityDescriptionFromApiEntry(Element apiEntry,
      HttpSecurityDescription preferredSecurityDesc) {
    Element httpSecurityElem = $(apiEntry).xpath("*[local-name()='http-security']").get(0);
    HttpSecuritySettings securitySettings = new HttpSecuritySettings(httpSecurityElem);

    // Try to use the method from preferredSecurityDesc,
    // but if it is not compatible select one of available methods.
    HttpSecurityDescription securityDescription = preferredSecurityDesc;
    if (preferredSecurityDesc == null || !preferredSecurityDesc.isCompatible(securitySettings)) {
      securityDescription = getDescriptionFromSecuritySettings(securitySettings);
    }
    return securityDescription;
  }

  @Override
  protected void validateCombinationPost(Combination combination)
      throws SuiteBroken {
  }

  @Override
  protected void validateCombinationGet(Combination combination)
      throws SuiteBroken {
  }

  @Override
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
  }


  protected String getApiUrlForHei(String heiId,
      String api, ApiEndpoint endpoint, String testName, String error) throws SuiteBroken {
    return getApiUrlsForHeis(Arrays.asList(heiId), api, endpoint, testName, error).get(0).url;
  }

  protected List<HeiIdAndUrl> getApiUrlsForHeis(List<String> heiIds,
      String api, ApiEndpoint endpoint, String testName, String error) throws SuiteBroken {
    List<HeiIdAndUrl> heiIdAndUrls = new ArrayList<>();

    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return testName;
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        for (String hei : heiIds) {
          List<String> urls =
              selectApiUrlForHeiFromCatalogue(api, endpoint, getApiInfo().getVersion(), hei);
          if (urls != null && !urls.isEmpty()) {
            heiIdAndUrls.add(new HeiIdAndUrl(hei, urls.get(0), endpoint));
          }
        }

        if (heiIdAndUrls.isEmpty()) {
          throw new Failure(error, Status.NOTICE, null);
        }

        return Optional.empty();
      }
    });

    return heiIdAndUrls;
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected String getSelectedHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }

  protected Request makeApiRequestWithPreferredSecurity(
      InlineValidationStep step, String url, ApiEndpoint endpoint,
      HttpSecurityDescription preferredSecurityDescription,
      Parameters parameters) {
    Element apiEntry = getApiEntryFromUrlFormCatalogue(url, endpoint);
    if (apiEntry == null) {
      return null;
    }

    return createRequestWithParameters(
        step,
        new Combination("GET", url, apiEntry,
            getSecurityDescriptionFromApiEntry(apiEntry, preferredSecurityDescription)
        ),
        parameters
    );
  }

  protected Request makeApiRequestWithPreferredSecurity(
      InlineValidationStep step, HeiIdAndUrl heiIdAndUrl,
      HttpSecurityDescription preferredSecurityDescription) {
    return makeApiRequestWithPreferredSecurity(
        step, heiIdAndUrl.url, heiIdAndUrl.endpoint, preferredSecurityDescription,
        new ParameterList(new Parameter(heiIdAndUrl.heiIdParameterName, heiIdAndUrl.heiId))
    );
  }

  /**
   * Given list of pairs (heiId, apiUrl) calls each of apiUrls with it's corresponding
   * heiId as a 'hei_id' parameter. 'selector' is used to extract values from the response.
   * When the function finds first (heiId, apiUrl) pair for which 'selector' select at least one
   * element, then (heiId, first selected element as a string) pair is returned.
   *
   * @param heiIdAndUrls
   *     List of (heiId, apiUrl) pairs.
   * @param securityDescription
   *     This security method will be used in calls to url from apiUrls if it is possible.
   *     If queried endpoint doesn't support this security method different one will be used.
   * @param selector
   *     xpath selector that filters responses from apiUrls
   * @param stepName
   *     Name of generated validation step.
   * @param error
   *     Error to report if not a single (heiId, string) pair can be found.
   * @return (heiId, first selected element as a string) pair.
   * @throws SuiteBroken
   *     If one of requests made results in fatal error, e.g. when schema won't match.
   */
  protected HeiIdAndString findResponseWithString(
      List<HeiIdAndUrl> heiIdAndUrls,
      HttpSecurityDescription securityDescription,
      String selector,
      String stepName,
      String error
  )
      throws SuiteBroken {
    HeiIdAndString heiIdAndString = new HeiIdAndString();
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return stepName;
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        for (HeiIdAndUrl heiIdAndUrl : heiIdAndUrls) {
          if (heiIdAndUrl.url == null || heiIdAndUrl.heiId == null) {
            continue;
          }

          try {
            Response response = makeRequest(this,
                makeApiRequestWithPreferredSecurity(this, heiIdAndUrl, securityDescription)
            );
            expect200(response);
            List<String> selectedStrings = selectFromDocument(
                makeXmlFromBytes(response.getBody()), selector
            );

            if (!selectedStrings.isEmpty()) {
              heiIdAndString.string = selectedStrings.get(0);
              heiIdAndString.heiId = heiIdAndUrl.heiId;
              return Optional.empty();
            }
          } catch (Failure e) {
            if (e.isFatal()) {
              throw e;
            }
            // else ignore
          }
        }

        throw new Failure(error, Status.NOTICE, null);
      }
    });

    return heiIdAndString;
  }

  protected interface ParameterSupplier {
    String get() throws SuiteBroken;
  }

  protected String getParameterValue(String parameter,
      ParameterSupplier defaultValueSupplier) throws SuiteBroken {
    if (this.currentState.parameters.contains(parameter)) {
      return this.currentState.parameters.get(parameter);
    } else {
      return defaultValueSupplier.get();
    }
  }

  protected String getParameterValue(String parameter) throws SuiteBroken {
    return getParameterValue(parameter, () -> null);
  }

  protected boolean isParameterProvided(String parameter) {
    return this.currentState.parameters.contains(parameter);
  }


  protected static class HeiIdAndUrl {
    public String heiIdParameterName;
    public String heiId;
    public String url;
    public ApiEndpoint endpoint;

    public HeiIdAndUrl(String heiId, String url, ApiEndpoint endpoint) {
      this.heiIdParameterName = "hei_id";
      this.heiId = heiId;
      this.url = url;
      this.endpoint = endpoint;
    }

    public HeiIdAndUrl(String heiIdParameterName, String heiId, String url, ApiEndpoint endpoint) {
      this.heiIdParameterName = heiIdParameterName;
      this.heiId = heiId;
      this.url = url;
      this.endpoint = endpoint;
    }
  }


  public static class HeiIdAndString {
    public String heiId;
    public String string;
  }
}
