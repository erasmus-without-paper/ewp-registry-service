package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Challenge;

/**
 * This one is a bit invalid, because it does include invalid headers in its WWW-Authenticate
 * header's `headers` key.
 */
public class ServiceHTTTInvalid6 extends ServiceHTTTValid {

  public ServiceHTTTInvalid6(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected Response createHttpSig401Response(Request request) {
    Response response = super.createHttpSig401Response(request);
    Challenge newOne = new Challenge("EWP", Lists.newArrayList("some-header"),
        Lists.newArrayList(Algorithm.RSA_SHA256));
    response.putHeader("WWW-Authenticate", newOne.getHeaderValue());
    return response;
  }
}
