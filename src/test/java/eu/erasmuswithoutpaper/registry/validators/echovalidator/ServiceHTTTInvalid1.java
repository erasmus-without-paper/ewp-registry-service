package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Algorithm;

/**
 * This one accepts RSA-SHA512 algorithm, instead of the require RSA-SHA256.
 */
public class ServiceHTTTInvalid1 extends ServiceHTTTValid {

  public ServiceHTTTInvalid1(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected List<Algorithm> getSupportedHttpsigAlgorithms() {
        return Collections.singletonList(Algorithm.RSA_SHA512);
      }
    };
  }

}
