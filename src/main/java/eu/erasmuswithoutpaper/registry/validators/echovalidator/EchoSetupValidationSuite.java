package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

public class EchoSetupValidationSuite
    extends AbstractSetupValidationSuite<EchoSuiteState> {

  protected EchoSetupValidationSuite(
      ApiValidator<EchoSuiteState> echoValidator,
      EchoSuiteState state,
      ValidationSuiteConfig config,
      int version) {
    super(echoValidator, state, config, false, version);
  }

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new EchoValidatedApiInfo(version);
  }

  public static List<ValidationParameter> getParameters() {
    return new ArrayList<>();
  }

  @Override
  protected List<CombEntry> getResponseEncryptionMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsResEncrTls()) {
      ret.add(CombEntry.RESENCR_TLS);
    }
    if (sec.supportsResEncrEwp()) {
      ret.add(CombEntry.RESENCR_EWP);
    }
    if (ret.size() == 0) {
      errors.add("Your Echo API does not support ANY of the response encryption "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  @Override
  protected List<CombEntry> getRequestEncryptionMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsReqEncrTls()) {
      ret.add(CombEntry.REQENCR_TLS);
    }
    if (sec.supportsReqEncrEwp()) {
      ret.add(CombEntry.REQENCR_EWP);
    }
    if (ret.size() == 0) {
      errors.add("Your Echo API does not support ANY of the request encryption "
          + "methods recognized by the Validator.");
    }

    return ret;
  }

  @Override
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
      errors.add("Your Echo API does not support ANY of the server authentication "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  @Override
  protected List<CombEntry> getClientAuthenticationMethods(HttpSecuritySettings sec,
      List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsCliAuthNone()) {
      warnings.add("Anonymous Client Authentication SHOULD NOT be enabled for Echo API.");
    }
    // Even though, we will still run some tests on it.
    ret.add(CombEntry.CLIAUTH_NONE);
    if (sec.supportsCliAuthHttpSig()) {
      ret.add(CombEntry.CLIAUTH_HTTPSIG);
    } else {
      warnings.add("It is RECOMMENDED for all EWP server endpoints to support HTTP "
          + "Signature Client Authentication. Your endpoint doesn't.");
    }
    if (ret.size() <= 1) {
      errors.add("Your Echo API does not support ANY of the client authentication "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

}
