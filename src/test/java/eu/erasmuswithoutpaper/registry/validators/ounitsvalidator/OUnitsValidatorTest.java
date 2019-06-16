package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.institutionsvalidator.InstitutionServiceV2Valid;
import eu.erasmuswithoutpaper.registry.validators.types.OunitsResponse;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class OUnitsValidatorTest extends AbstractApiTest {
  private static final String institutionsUrlHTTT =
      "https://university.example.com/institutions/HTTT/";
  private static final String ounitsUrlHTTT = "https://university.example.com/ounits/HTTT/";
  @Autowired
  private OUnitsValidator validator;

  @Override
  protected String getManifestFilename() {
    return "ounitsvalidator/manifest.xml";
  }

  InstitutionServiceV2Valid GetInstitutions() {
    return new InstitutionServiceV2Valid(institutionsUrlHTTT, client, validatorKeyStore) {
      @Override
      protected List<String> GetCoveredOUnits() {
        return Arrays.asList("ounit-1", "ounit-2", "ounit-3");
      }

      @Override
      protected String GetRootOUnit() {
        return "ounit-3";
      }
    };
  }

  @Test
  public void testAgainstOUnitsValid() {
    serviceTest(new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
                },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsValidOutput.txt"
    );
  }

  /**
   * Doesn't validate length of ounit-id list.
   */
  @Test
  public void testAgainstOUnitsInvalid1() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorMaxOUnitIdsExceeded() throws ErrorResponseException {
            //Do nothing
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput1.txt"
    );
  }

  /**
   * Doesn't validate length of ounit-code list.
   */
  @Test
  public void testAgainstOUnitsInvalid2() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorMaxOUnitCodesExceeded() throws ErrorResponseException {
            //Do nothing
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput2.txt"
    );
  }

  /**
   * Return 200 when no parameters are provided.
   */
  @Test
  public void testAgainstOUnitsInvalid3() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorNoParams()
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput3.txt"
    );
  }

  /**
   * Return 400 when unknown parameters are passed.
   */
  @Test
  public void testAgainstOUnitsInvalid4() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void HandleUnexpectedParams() throws ErrorResponseException {
            throw new ErrorResponseException(
                createErrorResponse(this.currentRequest, 400, "Unknown parameter")
            );
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput4.txt"
    );
  }

  /**
   * Returns 200 when hei-id is not passed.
   */
  @Test
  public void testAgainstOUnitsInvalid5() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorNoHeiId() throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput5.txt"
    );
  }

  /**
   * Returns 200 when ounit-code and ounit-id is not passed.
   */
  @Test
  public void testAgainstOUnitsInvalid6() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorNoIdsNorCodes() throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput6.txt"
    );
  }

  /**
   * Ignores additional hei-ids.
   */
  @Test
  public void testAgainstOUnitsInvalid7() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorMultipleHeiIds() throws ErrorResponseException {
            //Ignore
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput7.txt"
    );
  }

  /**
   * Handles both ids and codes.
   */
  @Test
  public void testAgainstOUnitsInvalid8() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorIdsAndCodes() throws ErrorResponseException {
            //Ignore
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput8.txt"
    );
  }

  /**
   * When ids and codes are passed, ignored codes.
   */
  @Test
  public void testAgainstOUnitsInvalid9() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorIdsAndCodes() throws ErrorResponseException {
            this.requestedOUnitCodes.clear();
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput9.txt"
    );
  }

  /**
   * When ids and codes are passed, ignored ids.
   */
  @Test
  public void testAgainstOUnitsInvalid10() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorIdsAndCodes() throws ErrorResponseException {
            this.requestedOUnitIds.clear();
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput10.txt"
    );
  }

  /**
   * When hei-id is not provided first covered hei is used.
   */
  @Test
  public void testAgainstOUnitsInvalid11() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorNoHeiId() throws ErrorResponseException {
            this.requestedHeiId = this.institutionsServiceV2.GetCoveredHeiIds().get(0);
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput11.txt"
    );
  }

  /**
   * When ounit-id is not provided first covered is used.
   */
  @Test
  public void testAgainstOUnitsInvalid12() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorNoIdsNorCodes() throws ErrorResponseException {
            String id = this.coveredOUnitsIds.values().iterator().next().getOunitId();
            this.requestedOUnitIds = Arrays.asList(id);
            this.requestedOUnitCodes = new ArrayList<>();
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput12.txt"
    );
  }

  /**
   * When passed unknown hei-id returns 200 and empty response.
   */
  @Test
  public void testAgainstOUnitsInvalid13() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorUnknownHeiId() throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput13.txt"
    );
  }

  /**
   * When passed unknown hei-id uses one of covered hei-ids.
   */
  @Test
  public void testAgainstOUnitsInvalid14() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void ErrorUnknownHeiId() throws ErrorResponseException {
            this.requestedHeiId = this.institutionsServiceV2.GetCoveredHeiIds().get(0);
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput14.txt"
    );
  }

  /**
   * Returns wrong ounit-ids.
   */
  @Test
  public void testAgainstOUnitsInvalid15() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected OunitsResponse.Ounit HandleKnownOUnit(OunitsResponse.Ounit data) {
            data.setOunitId("invalid-id");
            return data;
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput15.txt"
    );
  }

  /**
   * Returns some data for unknown ounit-ids.
   */
  @Test
  public void testAgainstOUnitsInvalid16() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected OunitsResponse.Ounit HandleUnknownOUnit() {
            return this.coveredOUnitsIds.values().iterator().next();
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput16.txt"
    );
  }

  /**
   * Too large max-ounit-ids in manifest.
   */
  @Test
  public void testAgainstOUnitsInvalid17() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected int getMaxOunitIds() {
            return super.getMaxOunitIds() - 1;
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput17.txt"
    );
  }

  /**
   * Too large max-ounit-codes in manifest.
   */
  @Test
  public void testAgainstOUnitsInvalid18() {
    serviceTest(
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected int getMaxOunitCodes() {
            return super.getMaxOunitCodes() - 1;
          }
        },
        ounitsUrlHTTT, "ounitsvalidator/OUnitsInvalidOutput18.txt"
    );
  }

  @Override
  protected ApiValidator GetValidator() {
    return validator;
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(2, 0, 0);
  }
}

