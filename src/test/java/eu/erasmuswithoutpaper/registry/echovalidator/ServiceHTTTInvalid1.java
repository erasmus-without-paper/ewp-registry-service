package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Algorithm;
import org.assertj.core.util.Lists;

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
        return Lists.newArrayList(Algorithm.RSA_SHA512);
      }
    };
  }

}
