package eu.erasmuswithoutpaper.registry.updater;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.common.Severity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.ocpsoft.prettytime.PrettyTime;

/**
 * This entity stores the status of the last manifest update attempt.
 */
@Entity
@Table(name = "REG_MANIFEST_UPDATE_STATUSES")
@ConditionalOnWebApplication
public class ManifestUpdateStatus {

  /**
   * Format a date for humans.
   *
   * @param date the date to be formatted.
   * @return A regular {@link Date#toString()} with a suffix appended (e.g. "(3 days ago)").
   */
  private static String formatTime(Date date) {
    if (date == null) {
      return "(never)";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(date);
    sb.append(" (");
    sb.append(new PrettyTime().format(date));
    sb.append(')');
    return sb.toString();
  }

  @Id
  private String url;

  private Date lastAccessAttempt;

  @Column(name = "last_access_flag_state")
  private int lastAccessFlagStatus;

  @Column(name = "last_access_notices_json")
  private String lastAccessNoticesJson;

  /**
   * Needed for Hibernate. Don't use explicitly.
   */
  public ManifestUpdateStatus() {
  }

  /**
   * Use this constructor when creating new entities. The new entity will have a
   * {@link Severity#UNDETERMINED} status, and an empty list of notices.
   *
   * @param url The URL of the manifest being described.
   */
  public ManifestUpdateStatus(String url) {
    this.url = url;
    this.setLastAccessFlagStatus(Severity.UNDETERMINED);
    this.setLastAccessNotices(new ArrayList<>());
  }

  /**
   * @return Indicates the time when the {@link RegistryUpdater} has last tried to update the
   *         manifest. Can be {@link Optional#empty()} if {@link RegistryUpdater} hasn't tried yet.
   */
  public Optional<Date> getLastAccessAttempt() {
    return Optional.ofNullable(this.lastAccessAttempt);
  }

  /**
   * @return Same as {@link #getLastAccessAttempt()}, but formatted for humans.
   */
  public String getLastAccessAttemptFormatted() {
    return formatTime(this.lastAccessAttempt);
  }

  /**
   * The overall status of the last manifest update attempt. This can be
   * {@link Severity#UNDETERMINED} if the the {@link RegistryUpdater} has not yet tried to update
   * the manifest!
   *
   * @return One of the {@link Severity} constants.
   */
  public Severity getLastAccessFlagStatus() {
    return Severity.fromIntegerValue(this.lastAccessFlagStatus);
  }

  /**
   * @return A list of {@link UpdateNotice}s which have been reported when {@link RegistryUpdater}
   *         has tried to import the manifest last time.
   */
  public List<UpdateNotice> getLastAccessNotices() {
    Gson gson = new Gson();
    JsonArray arr = JsonParser.parseString(this.lastAccessNoticesJson).getAsJsonArray();
    List<UpdateNotice> result = new ArrayList<>(arr.size());
    for (JsonElement elem : arr) {
      result.add(gson.fromJson(elem, UpdateNotice.class));
    }
    return result;
  }

  /**
   * @return The URL of the manifest who's update status is being described by this entity.
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * @param lastAccessAttempt see {@link #getLastAccessAttempt()}.
   */
  public void setLastAccessAttempt(Date lastAccessAttempt) {
    this.lastAccessAttempt = new Date(lastAccessAttempt.getTime());
  }

  /**
   * @param severity see {@link #getLastAccessFlagStatus()}.
   */
  public void setLastAccessFlagStatus(Severity severity) {
    this.lastAccessFlagStatus = severity.getIntegerValue();
  }

  /**
   * @param notices see {@link #getLastAccessNotices()}.
   */
  public void setLastAccessNotices(List<UpdateNotice> notices) {
    Gson gson = new Gson();
    this.lastAccessNoticesJson = gson.toJson(notices);
  }

  @Override
  public String toString() {
    return "ManifestUpdateStatus[url=" + this.getUrl() + ", severity="
        + this.getLastAccessFlagStatus().toString() + "]";
  }
}
