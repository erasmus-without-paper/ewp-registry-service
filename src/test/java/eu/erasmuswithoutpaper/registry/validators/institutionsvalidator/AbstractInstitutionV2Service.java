package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.types.InstitutionsResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import java.util.List;

public abstract class AbstractInstitutionV2Service extends AbstractInstitutionService {

    /**
     * @param url            The endpoint at which to listen for requests.
     * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
     */
    public AbstractInstitutionV2Service(String url, RegistryClient registryClient) {
        super(url, registryClient);
    }

    protected Response createInstitutionsResponse(
            Request request,
            List<InstitutionsResponse.Hei> heis) {
        return super.createInstitutionsResponse(request, heis);
    }
}
