package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it includes stale dates.
 */
public class ServiceMMTTInvalid2 extends ServiceMMTTValid {

  public ServiceMMTTInvalid2(String url, RegistryClient registryClient, KeyPair myKeyPair) {
    super(url, registryClient, myKeyPair);
  }

  @Override
  protected EwpHttpSigResponseSigner getHttpSigSigner() {
    return new EwpHttpSigResponseSigner(this.myKeyPair) {
      @Override
      protected void includeDateHeaders(Response response, boolean date, boolean originalDate) {
        String now = Utils
            .formatHeaderZonedDateTime(Utils.getCurrentZonedDateTime().minusMinutes(10));
          if (date && (response.getHeader("Date") == null)) {
            response.putHeader("Date", now);
          }
          if (originalDate && (response.getHeader("Original-Date") == null)) {
            response.putHeader("Original-Date", now);
          }
        }
      };

  }
}
