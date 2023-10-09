package eu.erasmuswithoutpaper.registry.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite.Parameter;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite.ParameterList;

import org.junit.jupiter.api.Test;

public class AbstractValidationSuiteParameterListTest {

  @Test
  public void bodyIsEscapingNamesAndValues() {
    Parameter p1 = new Parameter("first", "second");
    Parameter p2 = new Parameter("nationalCharactersĄĘ", "get some spaces");
    Parameter p3 = new Parameter("some other", "<#super%{valu|^&some~\\signs}>");
    ParameterList list = new AbstractValidationSuite.ParameterList(Arrays.asList(p1, p2, p3));

    String escapedP1 = "first=second";
    String escapedP2 = "nationalCharacters%C4%84%C4%98=get+some+spaces";
    String escapedP3 = "some+other=%3C%23super%25%7Bvalu%7C%5E%26some%7E%5Csigns%7D%3E";
    String allEscaped = escapedP1 + '&' + escapedP2 + '&' + escapedP3;

    assertThat(list.getPostBody()).isEqualTo(allEscaped);
  }

}
