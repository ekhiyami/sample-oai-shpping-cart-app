package com.microsoft.azure.samples.aishoppingcartservice;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Stores request metadata only, scoped to the browser's server session. */
public final class AiActivityTracker {
  private static final String ATTRIBUTE = AiActivityTracker.class.getName();

  private static State state(HttpSession session) {
    synchronized (session) {
      State state = (State) session.getAttribute(ATTRIBUTE);
      if (state == null) {
        state = new State();
        session.setAttribute(ATTRIBUTE, state);
      }
      return state;
    }
  }

  public static Call begin(String feature) {
    State state = null;
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      state = state(attributes.getRequest().getSession());
    }
    Call call = new Call(state, feature);
    if (state != null) {
      synchronized (state) {
        call.id = ++state.requests;
        state.latest = call.snapshot();
      }
    }
    return call;
  }

  public static Map<String, Object> snapshot(HttpSession session) {
    State state = state(session);
    synchronized (state) {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("requests", state.requests);
      result.put("lastRequest", state.latest == null ? null : new LinkedHashMap<>(state.latest));
      return result;
    }
  }

  private static final class State implements Serializable {
    private long requests;
    private Map<String, Object> latest;
  }

  public static final class Call implements AutoCloseable {
    private final State state;
    private final String feature;
    private final long started = System.nanoTime();
    private long id;
    private String status = "RUNNING";
    private String model;
    private Long inputTokens;
    private Long outputTokens;
    private Long durationMs;

    private Call(State state, String feature) {
      this.state = state;
      this.feature = feature;
    }

    public void response(JsonNode response) {
      if (response.path("model").isTextual()) model = response.path("model").asText();
      inputTokens = tokens(response.path("usage").path("prompt_tokens"));
      outputTokens = tokens(response.path("usage").path("completion_tokens"));
    }

    private Long tokens(JsonNode value) {
      return value.isIntegralNumber() && value.canConvertToLong() && value.asLong() >= 0
          ? value.asLong() : null;
    }

    public void success() {
      status = "SUCCESS";
    }

    private Map<String, Object> snapshot() {
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("id", id);
      value.put("feature", feature);
      value.put("status", status);
      value.put("durationMs", durationMs);
      value.put("model", model);
      value.put("inputTokens", inputTokens);
      value.put("outputTokens", outputTokens);
      return value;
    }

    @Override public void close() {
      if ("RUNNING".equals(status)) status = "FAILED";
      durationMs = Math.max(0, (System.nanoTime() - started) / 1_000_000);
      if (state != null) {
        synchronized (state) {
          // An older concurrent request must not replace the most recently started request.
          if (state.requests == id) state.latest = snapshot();
        }
      }
    }
  }
}
