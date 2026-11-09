package com.microsoft.azure.samples.aishoppingcartservice;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiActivityController {
  private final String configuredModel;
  private final boolean mockRecommendations;

  public AiActivityController(@Value("${ai.activity.model}") String configuredModel,
                              @Value("${shopping.cart.ai.mock:false}") boolean mockRecommendations) {
    this.configuredModel = configuredModel;
    this.mockRecommendations = mockRecommendations;
  }

  @GetMapping("/api/ai-activity")
  public ResponseEntity<Map<String, Object>> activity(HttpSession session) {
    Map<String, Object> data = AiActivityTracker.snapshot(session);
    String model = configuredModel;
    Object latest = data.get("lastRequest");
    if (latest instanceof Map<?, ?> call && call.get("model") instanceof String actual && !actual.isBlank()) {
      model = actual;
    }
    String displayModel = model.contains("gpt-5-mini") ? "GPT-5 mini" : model;
    data.put("provider", "azure");
    data.put("mockRecommendations", mockRecommendations);
    data.put("providerName", mockRecommendations ? "local recommendation mocks" : "Azure OpenAI");
    data.put("model", model);
    data.put("modelName", displayModel);
    data.put("message", mockRecommendations
        ? "Recipe and nutrition recommendations use fixed local data. Grounded chat, if enabled separately, uses Azure OpenAI."
        : "Your nutrition analysis, recipe and grounded-chat generation requests are processed by Azure OpenAI.");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(data);
  }
}
