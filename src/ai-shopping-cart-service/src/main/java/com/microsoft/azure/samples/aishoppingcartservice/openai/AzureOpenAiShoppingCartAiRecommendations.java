package com.microsoft.azure.samples.aishoppingcartservice.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.samples.aishoppingcartservice.AiActivityTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.samples.aishoppingcartservice.cartitem.CartItem;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Calls Azure OpenAI's current v1 Chat Completions endpoint. */
public class AzureOpenAiShoppingCartAiRecommendations implements ShoppingCartAiRecommendations {
  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15)).build();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String apiKey;
  private final URI endpoint;
  private final String deployment;
  private final int maxCompletionTokens;
  private final String reasoningEffort;

  public AzureOpenAiShoppingCartAiRecommendations(
      String apiKey, String endpoint, String deployment,
      int maxCompletionTokens, String reasoningEffort) {
    if (apiKey.isBlank() || endpoint.isBlank() || deployment.isBlank()) {
      throw new IllegalArgumentException("Azure OpenAI endpoint, key and deployment are required");
    }
    this.apiKey = apiKey;
    this.endpoint = URI.create(endpoint.replaceAll("/+$", "") + "/openai/v1/chat/completions");
    this.deployment = deployment;
    this.maxCompletionTokens = maxCompletionTokens;
    this.reasoningEffort = reasoningEffort;
  }

  public String getAINutritionAnalysis(List<CartItem> cartItems) {
    return getChatCompletion("Nutrition analysis", SystemMessageConstants.AI_NUTRITION_ANALYSIS, cartItems);
  }

  public String getTop3Recipes(List<CartItem> cartItems) {
    return getChatCompletion("Recipes", SystemMessageConstants.RECIPES, cartItems);
  }

  private String getChatCompletion(String feature, String instructions, List<CartItem> cartItems) {
    String basket = cartItems.stream().map(CartItem::getName)
        .map(String::trim).map(String::toLowerCase).collect(Collectors.joining(", "));
    Map<String, Object> body = Map.of(
        "model", deployment,
        "messages", List.of(
            Map.of("role", "system", "content", instructions),
            Map.of("role", "user", "content", "The basket is: " + basket)),
        "max_completion_tokens", maxCompletionTokens,
        "reasoning_effort", reasoningEffort,
        "response_format", Map.of("type", "json_object"));
    try {
      HttpRequest request = HttpRequest.newBuilder(endpoint)
          .timeout(Duration.ofSeconds(120))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
      try (var activity = AiActivityTracker.begin(feature)) {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            "Azure OpenAI returned HTTP " + response.statusCode());
      }
      JsonNode result = mapper.readTree(response.body());
      activity.response(result);
      JsonNode choice = result.path("choices").path(0);
      if (!"stop".equals(choice.path("finish_reason").asText())) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            "Azure OpenAI did not return a complete answer");
      }
      String content = choice.path("message").path("content").asText("");
      JsonNode answer = mapper.readTree(content);
      if (answer == null || !answer.isObject()) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            "Azure OpenAI returned an invalid JSON answer");
      }
      activity.success();
      return mapper.writeValueAsString(answer);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI request interrupted");
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service response unavailable");
    }
  }
}
