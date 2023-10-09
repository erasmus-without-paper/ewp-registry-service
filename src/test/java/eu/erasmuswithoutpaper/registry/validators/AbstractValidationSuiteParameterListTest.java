package eu.erasmuswithoutpaper.registry.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite.Parameter;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite.ParameterList;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractValidationSuiteParameterListTest {

  private ParameterList testList;
  private String escapedTestList;

  @BeforeEach
  private void beforeAll() {
    Parameter p1 = new Parameter("first", "second");
    Parameter p2 = new Parameter("nationalCharactersĄĘ", "get some spaces");
    Parameter p3 = new Parameter("some other", "<#super%{valu|^&some~\\signs}>");
    Parameter p4 = new Parameter("Beijing", "北京");
    testList = new ParameterList(Arrays.asList(p1, p2, p3, p4));

    String escapedP1 = "first=second";
    String escapedP2 = "nationalCharacters%C4%84%C4%98=get+some+spaces";
    String escapedP3 = "some+other=%3C%23super%25%7Bvalu%7C%5E%26some%7E%5Csigns%7D%3E";
    String escapedP4 = "Beijing=%E5%8C%97%E4%BA%AC";
    escapedTestList = Strings.join(Arrays.asList(escapedP1, escapedP2, escapedP3, escapedP4), '&');
  }

  @Test
  public void bodyIsEscapingNamesAndValues() {
    assertThat(testList.getPostBody()).isEqualTo(escapedTestList);
  }

  @Test
  public void urlIsEscapingNamesAndValuesInParams() {
    String url = testList.getGetUrl("example.com");
    assertThat(url).isEqualTo("example.com" + '?' + escapedTestList);
  }

  @Test
  public void urlMustBeValid() {
    ParameterList emptyList = new AbstractValidationSuite.ParameterList(Collections.emptyList());
    Assertions.assertThrows(RuntimeException.class, () -> {
      emptyList.getGetUrl(null);
    });
    Assertions.assertThrows(RuntimeException.class, () -> {
      emptyList.getGetUrl("^http://example.com");
    });
    Assertions.assertThrows(RuntimeException.class, () -> {
      emptyList.getGetUrl("http://example. com");
    });
  }

}
