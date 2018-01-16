package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;

/**
 * Invalid, because it allows anonymous access.
 */
public class ServiceMTTTInvalid1 extends ServiceMTTTValid {

  public ServiceMTTTInvalid1(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    if (request.getClientCertificate().isPresent()) {
      return this.sttt.handleInternetRequest2(request);
    } else if (request.getHeader("Authorization") != null) {
      return this.httt.handleInternetRequest2(request);
    } else {
      return this.createEchoResponse(request, this.retrieveEchoValues(request),
          Lists.newArrayList());
    }
  }
}
