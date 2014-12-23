package com.appspot.c_three_games.domain;

import java.util.ArrayList;
import java.util.Map;

public class GCMResponse {
  // response
  public Long multicast_id;
  public int success;
  public int failure;
  public int canonical_ids;
  public ArrayList<Map<String, Object>> results;

  public GCMResponse() {
    results = new ArrayList<Map<String, Object>>();
  }
}
