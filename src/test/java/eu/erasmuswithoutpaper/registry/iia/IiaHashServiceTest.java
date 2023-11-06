package eu.erasmuswithoutpaper.registry.iia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import eu.erasmuswithoutpaper.registry.TestFiles;
import eu.erasmuswithoutpaper.registry.WRTest;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class IiaHashServiceTest extends WRTest {

  @Autowired
  private IiaHashService iiaHashService;

  @Test
  void checkHashResultsForExamples() throws Exception {
    assertThat(getCheckHashResultSingleIiaExample("iia/get-response-example-v6.xml")).isTrue();
    assertThat(getCheckHashResultSingleIiaExample("iia/get-response-example-v7.xml")).isTrue();
    assertThat(getCheckHashResultSingleIiaExample("iia/get-response-wrong-hash-v7.xml")).isFalse();
    assertThat(getCheckHashResultSingleIiaExample("iia/get-response-xsltkit-v6.xml")).isTrue();
    assertThat(getCheckHashResultSingleIiaExample("iia/get-response-xsltkit-v7.xml")).isTrue();
  }

  private boolean getCheckHashResultSingleIiaExample(String exampleFilename) throws Exception {
    List<HashComparisonResult> results =
        iiaHashService.checkHash(new InputSource(TestFiles.getFileAsStream(exampleFilename)));
    assertThat(results).hasSize(1);
    return results.get(0).isCorrect();
  }
}
