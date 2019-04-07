package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.types.InstitutionsResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import org.apache.xerces.impl.dv.util.Base64;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class InstitutionValidatorTest extends AbstractApiTest {
  private static String institutionsUrlHTTT;
  @Autowired
  private InstitutionsValidator validator;

  @BeforeClass
  public static void setUpClass() {
    selfManifestUrl = "https://registry.example.com/manifest.xml";
    apiManifestUrl = "https://university.example.com/manifest.xml";
    institutionsUrlHTTT = "https://university.example.com/institutions/HTTT/";
    needsReinit = true;
  }

  @Before
  public void setUp() {
    if (needsReinit) {
      /*
       * Minimal setup for the services to guarantee that repo contains a valid catalogue,
       * consistent with the certificates returned by the validator.
       */
      this.sourceProvider.clearSources();
      this.repo.deleteAll();
      this.internet.clearAll();

      String myManifest = this.selfManifestProvider.getManifest();
      this.internet.putURL(selfManifestUrl, myManifest);
      this.sourceProvider.addSource(ManifestSource.newTrustedSource(selfManifestUrl));

      String apiManifest = this.getFileAsString("institutionsvalidator/manifest.xml");
      myKeyPair = this.validator.generateKeyPair();
      apiManifest = apiManifest.replace(
          "SERVER-KEY-PLACEHOLDER",
          Base64.encode(myKeyPair.getPublic().getEncoded())
      );
      this.internet.putURL(apiManifestUrl, apiManifest);
      this.sourceProvider
          .addSource(ManifestSource.newRegularSource(apiManifestUrl, Lists.newArrayList()));

      this.registryUpdater.reloadAllManifestSources();
      needsReinit = false;
    }
  }

  private void serviceTest(FakeInternetService service, String url, String filename) {
    try {
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(url, new SemanticVersion(2, 0, 0), null))
          .isEqualTo(this.getFileAsString(filename));
      this.internet.removeFakeInternetService(service);
    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstInstitutionsValid() {
    String url = institutionsUrlHTTT;
    serviceTest(new InstitutionServiceV2Valid(url, this.client, validatorKeyStore),
        url, "institutionsvalidator/InstitutionsValidOutput.txt"
    );
  }

  /**
   * Doesn't validate length of hei-id list.
   */
  @Test
  public void testAgainstInstitutionsInvalid1() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void CheckHeis(List<String> heis) throws ErrorResponseException {
            //Do nothing.
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput1.txt"
    );
  }

  /**
   * Counts unique hei-id parameters.
   */
  @Test
  public void testAgainstInstitutionsInvalid2() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void CheckHeis(List<String> heis) throws ErrorResponseException {
            ArrayList<String> unique_heis = new ArrayList<>(new HashSet<>(heis));
            super.CheckHeis(unique_heis);
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput2.txt"
    );
  }

  /**
   * Reports wrong max-hei-id in its manifest.
   */
  @Test
  public void testAgainstInstitutionsInvalid3() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void CheckHeis(List<String> heis) throws ErrorResponseException {
            if (heis.size() > max_hei_ids - 1) {
              throw new ErrorResponseException(
                  createErrorResponse(this.currentRequest, 400, "Exceeded max-hei-ids parameter")
              );
            }
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput3.txt"
    );
  }

  /**
   * Returns 400 if passed parameters other than "hei_id"
   */
  @Test
  public void testAgainstInstitutionsInvalid4() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void ExtractParamsMultipleParams(Map<String, List<String>> params)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createErrorResponse(this.currentRequest, 400, "Expected only hei_id parameters")
            );
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput4.txt"
    );
  }

  /**
   * Returns 200 OK empty response when no parameters are provided.
   */
  @Test
  public void testAgainstInstitutionsInvalid5() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void CheckRequestMethod() throws ErrorResponseException {
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput5.txt"
    );
  }

  /**
   * Returns 200 OK empty response when no parameters are provided.
   */
  @Test
  public void testAgainstInstitutionsInvalid6() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void ExtractParamsNoParams(Map<String, List<String>> params)
              throws ErrorResponseException {
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput6.txt"
    );
  }

  /**
   * Returns 200 OK empty response when there are some parameters, but they aren't hei-id.
   */
  @Test
  public void testAgainstInstitutionsInvalid7() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void ExtractParamsNoHeiIds(Map<String, List<String>> params)
              throws ErrorResponseException {
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput7.txt"
    );
  }

  /**
   * Returns some data even for HEIs it shouldn't know.
   */
  @Test
  public void testAgainstInstitutionsInvalid8() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void ProcessNotCoveredHei(String hei, List<InstitutionsResponse.Hei> heis)
              throws ErrorResponseException {
            heis.add(createFakeHeiData(hei));
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput8.txt"
    );
  }

  /**
   * Returns 400 when passed unknown HEI ID.
   */
  @Test
  public void testAgainstInstitutionsInvalid9() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected void ProcessNotCoveredHei(String hei, List<InstitutionsResponse.Hei> heis)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                this.createErrorResponse(this.currentRequest, 400, "Unknown HEI ID encountered")
            );
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput9.txt"
    );
  }

  /**
   * root-ounit-id is not in ounit-id list
   */
  @Test
  public void testAgainstInstitutionsInvalid10() {
    String url = institutionsUrlHTTT;
    serviceTest(
        new InstitutionServiceV2Valid(url, this.client, validatorKeyStore) {
          @Override
          protected List<String> GetCoveredOUnits() {
            return Arrays.asList("1", "2", "3");
          }

          @Override
          protected String GetRootOUnit() {
            return "4";
          }
        },
        url, "institutionsvalidator/InstitutionsInvalidOutput10.txt"
    );
  }

  @Override
  protected ApiValidator<InstitutionsSuiteState> GetValidator() {
    return validator;
  }
}

