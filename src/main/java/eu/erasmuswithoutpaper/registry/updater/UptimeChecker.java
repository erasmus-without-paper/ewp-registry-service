package eu.erasmuswithoutpaper.registry.updater;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import eu.erasmuswithoutpaper.registry.internet.Internet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.joox.Match;
import org.xml.sax.SAXException;

/**
 * This service is responsible for determining the uptime of the application.
 */
@Service
public class UptimeChecker {

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

  @Autowired
  UptimeChecker(Internet internet, @Value("${app.uptimerobot.monitor-key}") String monitorApiKey) {
    this.internet = internet;
    this.monitorApiKey = monitorApiKey;
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
    StringBuilder sb = new StringBuilder();
    sb.append("https://api.uptimerobot.com/getMonitors?apiKey=");
    sb.append(this.monitorApiKey);
    sb.append("&customUptimeRatio=1-7-30-365");
    String url = sb.toString();
    byte[] xml;
    try {
      xml = this.internet.getUrl(url);
    } catch (IOException e) {
      throw new CouldNotRefresh(e);
    }
    Match doc;
    try {
      doc = $(new ByteArrayInputStream(xml));
    } catch (SAXException | IOException e) {
      throw new CouldNotRefresh(e);
    }
    String[] customRatios = doc.find("monitor").attr("customuptimeratio").split("-");
    this.last24HoursUptimeRatio = customRatios[0];
    this.last7DaysUptimeRatio = customRatios[1];
    this.last30DaysUptimeRatio = customRatios[2];
    this.last365DaysUptimeRatio = customRatios[3];
  }
}
