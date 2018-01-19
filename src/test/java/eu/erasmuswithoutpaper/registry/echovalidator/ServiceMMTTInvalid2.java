package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
      protected ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(10);
      }
    };
  }
}
