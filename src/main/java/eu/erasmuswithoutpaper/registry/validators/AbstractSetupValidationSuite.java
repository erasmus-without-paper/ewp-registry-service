package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.HttpSecuritySettings;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.base.Charsets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

public abstract class AbstractSetupValidationSuite<S extends SuiteState>
    extends AbstractValidationSuite<S> {
  protected AbstractSetupValidationSuite(
      ApiValidator<S> validator,
      EwpDocBuilder docBuilder,
      Internet internet,
      RegistryClient regClient,
      ManifestRepository repo, S state) {
    super(validator, docBuilder, internet, regClient, repo, state);
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
    String url = "https://api.github.com/repos/erasmus-without-paper/ewp-specs-api-";
    url += getApiName();
    url += "/tags";

    List<SemanticVersion> result = new ArrayList<>();
    try {
      byte[] data = this.internet.getUrl(url);
      JSONArray jsonArray = new JSONArray(new String(data, Charsets.UTF_8));
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonObject = jsonArray.getJSONObject(i);
        SemanticVersion version = new SemanticVersion(jsonObject.getString("name"));
        result.add(version);
      }
      return result;
    } catch (IOException e) {
      getLogger().warn("Cannot fetch github tags from url " + url);
    } catch (JSONException e) {
      getLogger().warn("GitHub api returned invalid JSON from url " + url);
    } catch (SemanticVersion.InvalidVersionString e) {
      getLogger().warn("GitHub tags response contained invalid name field.");
    }
    return new ArrayList<>();
  }

  protected void runApiSpecificTests(HttpSecurityDescription security) {
    //intentionally left empty
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
              "API version " + AbstractSetupValidationSuite.this.currentState.version.toString()
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
        if (new Date().getTime() - AbstractSetupValidationSuite.this.parentValidator
            .getCredentialsGenerationDate().getTime() < 10 * 60 * 1000) {
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
        if (!AbstractSetupValidationSuite.this.currentState.url.startsWith("https://")) {
          throw new Failure("It needs to be HTTPS.", Status.FAILURE, null);
        }
        try {
          new URL(AbstractSetupValidationSuite.this.currentState.url);
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
            if (supportsGetRequests) {
              this.currentState.combinations.add(
                  new Combination("GET", AbstractSetupValidationSuite.this.currentState.url,
                      AbstractSetupValidationSuite.this.currentState.matchedApiEntry, cliauth,
                      srvauth,
                      reqencr,
                      resencr
                  ));
            }
            this.currentState.combinations.add(
                new Combination("POST", AbstractSetupValidationSuite.this.currentState.url,
                    AbstractSetupValidationSuite.this.currentState.matchedApiEntry, cliauth,
                    srvauth,
                    reqencr,
                    resencr
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
        int matchedApiEntries = 0;

        ApiSearchConditions conds = new ApiSearchConditions();
        conds.setApiClassRequired(
            AbstractSetupValidationSuite.this.getApiNamespace(),
            AbstractSetupValidationSuite.this.getApiName(),
            AbstractSetupValidationSuite.this.currentState.version.toString()
        );

        String expectedVersionStr =
            AbstractSetupValidationSuite.this.currentState.version.toString();
        String expectedUrl = AbstractSetupValidationSuite.this.currentState.url;
        Collection<Element> entries = AbstractSetupValidationSuite.this.regClient.findApis(conds);
        for (Element entry : entries) {
          String version = entry.getAttribute("version");
          if ($(entry).find("url").text().equals(expectedUrl)
              && version.equals(expectedVersionStr)) {
            AbstractSetupValidationSuite.this.currentState.matchedApiEntry = entry;
            matchedApiEntries++;
          }
        }

        if (matchedApiEntries == 0) {
          throw new Failure("Could not find this URL and version in the Registry Catalogue. "
              + "Make sure that it is properly registered "
              + "(as declared in API's `manifest-entry.xsd` file): "
              + AbstractSetupValidationSuite.this.currentState.url, Status.FAILURE, null);
        }

        if (matchedApiEntries > 1) {
          throw new Failure("Multiple (" + Integer.toString(matchedApiEntries)
              + ") API entries found for this URL and version.", Status.FAILURE, null);
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

  protected List<CombEntry> getClientAuthenticationMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (!sec.supportsCliAuthNone()) {
      notices.add("You may consider allowing this API to by accessed by anonymous clients.");
    } else {
      ret.add(CombEntry.CLIAUTH_NONE);
    }

    if (sec.supportsCliAuthTlsCert()) {
      if (sec.supportsCliAuthTlsCertSelfSigned()) {
        ret.add(CombEntry.CLIAUTH_TLSCERT_SELFSIGNED);
      } else {
        notices.add("Not implemented."); //TODO
      }
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

  @Override
  protected void validateCombinationPost(Combination combination)
      throws SuiteBroken {
  }

  @Override
  protected void validateCombinationGet(Combination combination)
      throws SuiteBroken {
  }

}
