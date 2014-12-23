package com.appspot.c_three_games.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class GCMBody {

  private String json;

  public static final class Builder {
    List<String> registration_ids;
    String collapse_key;
    Boolean delay_while_idle;
    Map<String, Object> data;
    Long time_to_live;

    public Builder setCollapseKey(String collapseKey) {
      this.collapse_key = collapseKey;
      return this;
    }

    public Builder addRegistrationId(String registrationId) {
      this.registration_ids.add(registrationId);
      return this;
    }

    public Builder addRegistrationIds(List<String> registrationIds) {
      this.registration_ids.addAll(registrationIds);
      return this;
    }

    public Builder setDelayWhileIdle(Boolean delayWhileIdle) {
      this.delay_while_idle = delayWhileIdle;
      return this;
    }

    public Builder addData(String key, Object value) {
      this.data.put(key, value);
      return this;
    }

    public Builder setTimeToLive(Long timeToLive) {
      this.time_to_live = timeToLive;
      return this;
    }

    public GCMBody build() {
      return new GCMBody(this);
    }

    public Builder() {
      registration_ids = new ArrayList<String>();
      data = new HashMap<String, Object>();
    }
  }

  public GCMBody(Builder builder) {
    Gson gson = new Gson();
    json = gson.toJson(builder);
  }

  public String getJson() {
    return json;
  }
}
