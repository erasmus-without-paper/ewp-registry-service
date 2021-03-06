package eu.erasmuswithoutpaper.registry.consoleapplication;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.validators.HtmlValidationReportFormatter;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportUtils {
  private static final Logger logger = LoggerFactory.getLogger(ReportUtils.class);

  /**
   * Generates short text summary of run tests.
   * @param steps
   *      Descriptions of run tests.
   * @return
   *      String with summary of tests passed in parameter.
   */
  public static String generateReportSummary(List<ValidationStepWithStatus> steps) {
    List<String> results = new ArrayList<>();
    ValidationStepWithStatus.Status worstStatus =
        steps.stream().map(ValidationStepWithStatus::getStatus).max(
            ValidationStepWithStatus.Status::compareTo)
            .orElse(ValidationStepWithStatus.Status.SUCCESS);
    results.add(String.format("RESULT: %s", worstStatus));
    results.add(String.format("Run %s tests", steps.size()));
    for (ValidationStepWithStatus.Status status : ValidationStepWithStatus.Status.values()) {
      long numberOfStepsWithStatus = steps.stream()
          .filter(step -> step.getStatus().equals(status))
          .count();
      if (numberOfStepsWithStatus > 0) {
        results.add(String.format("%s: %d/%d", status, numberOfStepsWithStatus, steps.size()));
      }
    }

    return String.join("\n", results);
  }

  /**
   * Generates full HTML report from run tests.
   * @param steps
   *      Descriptions of run tests.
   * @param validationInfoParameters
   *      Contains some information about performed tests.
   * @param registryDomain
   *      Domain of the Registry.
   * @param docBuilder
   *      Used to parse and format XML in responses.
   * @return
   *      HTML as a String that contains the report.
   */
  public static String generateHtmlReport(List<ValidationStepWithStatus> steps,
      HtmlValidationReportFormatter.ValidationInfoParameters validationInfoParameters,
      EwpDocBuilder docBuilder,String registryDomain) {
    HtmlValidationReportFormatter htmlValidationReportFormatter =
        new HtmlValidationReportFormatter(docBuilder);

    Map<String, Object> pebbleContext = htmlValidationReportFormatter.getPebbleContext(
        steps,
        validationInfoParameters
    );

    String registryUrl = "https://" + registryDomain;
    pebbleContext.put("baseUrl", registryUrl);
    pebbleContext.put("isUsingDevDesign", true);

    ClasspathLoader classpathLoader = new ClasspathLoader();
    classpathLoader.setPrefix("templates");
    classpathLoader.setSuffix(".pebble");
    PebbleEngine engine = new PebbleEngine.Builder().loader(classpathLoader).build();
    PebbleTemplate compiledTemplate = null;
    try {
      compiledTemplate = engine.getTemplate("validationResult");
    } catch (PebbleException e) {
      logger.error("Problem with generating full report as HTML.", e);
      return "An error occurred";
    }

    Writer writer = new StringWriter();
    try {
      compiledTemplate.evaluate(writer, pebbleContext);
    } catch (PebbleException | IOException e) {
      logger.error("Problem with generating full report as HTML.", e);
      return "An error occurred";
    }

    return writer.toString();
  }
}
