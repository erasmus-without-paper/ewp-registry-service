package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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

import org.w3c.dom.Element;

public abstract class AbstractSetupValidationSuite<S extends SuiteState>
    extends AbstractValidationSuite<S> {
  protected AbstractSetupValidationSuite(
      ApiValidator<S> validator,
      EwpDocBuilder docBuilder,
      Internet internet, String urlStr,
      RegistryClient regClient,
      ManifestRepository repo, S state) {
    super(validator, docBuilder, internet, urlStr, regClient, repo, state);
  }

  @Override
  protected void runTests(HttpSecurityDescription security)
      throws SuiteBroken {
    runCredentialsDateTest();
    runHttpsSchemeTest();
    runApiVersionTest();
    validateSecurityMethods();
    runApiSpecificTests(security);
  }

  protected void runApiSpecificTests(HttpSecurityDescription security) {
    //intentionally left empty
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
        if (!AbstractSetupValidationSuite.this.urlToBeValidated.startsWith("https://")) {
          throw new Failure("It needs to be HTTPS.", Status.FAILURE, null);
        }
        try {
          new URL(AbstractSetupValidationSuite.this.urlToBeValidated);
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
                  new Combination("GET", AbstractSetupValidationSuite.this.urlToBeValidated,
                      AbstractSetupValidationSuite.this.currentState.matchedApiEntry, cliauth,
                      srvauth,
                      reqencr,
                      resencr
                  ));
            }
            this.currentState.combinations.add(
                new Combination("POST", AbstractSetupValidationSuite.this.urlToBeValidated,
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

  protected void runApiVersionTest() throws SuiteBroken {
    this.addAndRun(true, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Verifying if the URL is properly registered.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        //TODO: sprawdzić czy dostarczony url jest zarejestrowany w katalogu oraz
        //ze wersja do ktorej przygotowane sa testy jest kompatybilna z ta z katalogu
        //w ogole tu trzeba wziac pod uwage dostarczona wersje, a nie kazda z katalogu
        //do zastanowienia sie

        int matchedApiEntries = 0;

        ApiSearchConditions conds = new ApiSearchConditions();
        conds.setApiClassRequired(
            AbstractSetupValidationSuite.this.getApiNamespace(),
            AbstractSetupValidationSuite.this.getApiName(),
            AbstractSetupValidationSuite.this.getApiVersion()
        );
        Collection<Element> entries = AbstractSetupValidationSuite.this.regClient.findApis(conds);
        for (Element entry : entries) {
          if ($(entry).find("url").text()
              .equals(AbstractSetupValidationSuite.this.urlToBeValidated)) {
            AbstractSetupValidationSuite.this.currentState.matchedApiEntry = entry;
            matchedApiEntries++;
          }
        }

        if (matchedApiEntries == 0) {
          throw new Failure("Could not find this URL in the Registry Catalogue. "
              + "Make sure that it is properly registered "
              + "(as declared in API's `manifest-entry.xsd` file): "
              + AbstractSetupValidationSuite.this.urlToBeValidated, Status.FAILURE, null);
        }

        if (matchedApiEntries > 1) {
          throw new Failure("Multiple (" + Integer.toString(matchedApiEntries)
              + ") API entries found for this URL. "
              + "Results of the remaining tests might be non-determinictic.", Status.WARNING, null);
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
