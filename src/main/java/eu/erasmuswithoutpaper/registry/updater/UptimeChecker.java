package eu.erasmuswithoutpaper.registry.updater;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * This service is responsible for determining the uptime of the application.
 */
@Service
@ConditionalOnWebApplication
public class UptimeChecker {
  private static final Logger logger = LoggerFactory.getLogger(UptimeChecker.class);

  /**
   * Thrown by {@link UptimeChecker#refresh()} on refresh errors.
   */
  @SuppressWarnings("serial")
  public static class CouldNotRefresh extends RuntimeException {
    CouldNotRefresh(Exception ex) {
      super(ex);
    }
  }

  private static String formatRatio(String value) {
    if (value == null) {
      return "n/a";
    }
    NumberFormat format = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));
    return format.format(Double.parseDouble(value)) + "%";
  }

  private final Internet internet;
  private final String monitorApiKey;

  private String last24HoursUptimeRatio = null;
  private String last7DaysUptimeRatio = null;
  private String last30DaysUptimeRatio = null;
  private String last365DaysUptimeRatio = null;

  private final int maxRetries;
  private final int retryTimeoutSeconds;

  @Autowired
  UptimeChecker(Internet internet,
      @Value("${app.uptimerobot.monitor-key}") String monitorApiKey,
      @Value("${app.uptimerobot.max-retries}") int maxRetries,
      @Value("${app.uptimerobot.retry-timeout-seconds}") int retryTimeoutSeconds) {
    this.internet = internet;
    this.monitorApiKey = monitorApiKey;
    this.maxRetries = maxRetries;
    this.retryTimeoutSeconds = retryTimeoutSeconds;
  }

  /**
   * @return Uptime-ratio-string for the last 24 hours.
   */
  public String getLast24HoursUptimeRatio() {
    return formatRatio(this.last24HoursUptimeRatio);
  }

  /**
   * @return Uptime-ratio-string for the last 30 days.
   */
  public String getLast30DaysUptimeRatio() {
    return formatRatio(this.last30DaysUptimeRatio);
  }

  /**
   * @return Uptime-ratio-string for the last 365 days.
   */
  public String getLast365DaysUptimeRatio() {
    return formatRatio(this.last365DaysUptimeRatio);
  }

  /**
   * @return Uptime-ratio-string for the last 7 days.
   */
  public String getLast7DaysUptimeRatio() {
    return formatRatio(this.last7DaysUptimeRatio);
  }

  /**
   * Contact the offsite service and fetch the uptime ratios.
   */
  public void refresh() {
    if (this.monitorApiKey.length() == 0) {
      return;
    }
    String params =
        String.format("api_key=%s&format=xml&custom_uptime_ratios=1-7-30-365", this.monitorApiKey);
    byte[] xml = loadPageWithRetries(params);
    Match doc;
    try {
      doc = $(new ByteArrayInputStream(xml));
    } catch (SAXException | IOException e) {
      throw new CouldNotRefresh(e);
    }
    String[] customRatios = doc.find("monitor").attr("custom_uptime_ratio").split("-");
    this.last24HoursUptimeRatio = customRatios[0];
    this.last7DaysUptimeRatio = customRatios[1];
    this.last30DaysUptimeRatio = customRatios[2];
    this.last365DaysUptimeRatio = customRatios[3];
  }

  private byte[] loadPageWithRetries(String body) throws CouldNotRefresh {
    int retry = 0;
    while (true) {
      try {
        Request request = new Request("POST", "https://api.uptimerobot.com/v2/getMonitors");
        request.setBody(body.getBytes(StandardCharsets.UTF_8));
        Response response = this.internet.makeRequest(request);
        return response.getBody();
      } catch (IOException e) {
        logger.error("An error has occurred when fetching uptimerobot.com API.", e);
        if (retry >= this.maxRetries) {
          throw new CouldNotRefresh(e);
        }

        retry++;
        try {
          TimeUnit.SECONDS.sleep(this.retryTimeoutSeconds);
        } catch (InterruptedException ex) {
          //ignore
        }
      }
    }
  }
}
