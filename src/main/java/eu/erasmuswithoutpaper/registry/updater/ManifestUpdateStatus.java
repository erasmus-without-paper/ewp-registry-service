package eu.erasmuswithoutpaper.registry.updater;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import eu.erasmuswithoutpaper.registry.common.Severity;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * This entity stores the status of the last manifest update attempt.
 */
@Entity
@Table(name = "REG_MANIFEST_UPDATE_STATUSES")
public class ManifestUpdateStatus {

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
  public ManifestUpdateStatus() {}

  /**
   * Use this constructor when creating new entities. The new entity will have a
   * {@link Severity#UNDETERMINED} status, and an empty list of notices.
   *
   * @param url The URL of the manifest being described.
   */
  public ManifestUpdateStatus(String url) {
    this.url = url;
    this.setLastAccessFlagStatus(Severity.UNDETERMINED);
    this.setLastAccessNotices(Lists.newArrayList());
  }

  /**
   * @return Indicates the time when the {@link RegistryUpdater} has last tried to update the
   *         manifest. Can be {@link Optional#empty()} if {@link RegistryUpdater} hasn't tried yet.
   */
  public Optional<Date> getLastAccessAttempt() {
    return Optional.ofNullable(this.lastAccessAttempt);
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
    JsonParser parser = new JsonParser();
    JsonArray arr = parser.parse(this.lastAccessNoticesJson).getAsJsonArray();
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
    this.lastAccessAttempt = lastAccessAttempt;
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
