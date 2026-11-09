package com.microsoft.azure.samples.aishoppingcartservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.microsoft.azure.samples.aishoppingcartservice.openai.AzureOpenAiShoppingCartAiRecommendations;
import com.microsoft.azure.samples.aishoppingcartservice.openai.MockShoppingCartAiRecommendations;
import com.microsoft.azure.samples.aishoppingcartservice.openai.ShoppingCartAiRecommendations;

@Configuration
public class AiShoppingCartServiceConfiguration {
  @Value("${azure.openai.apikey}")
  private String azureOpenAiApiKey;
  @Value("${azure.openai.endpoint}")
  private String azureOpenAiEndpoint;
  @Value("${azure.openai.deployment.id}")
  private String azureOpenAiModelDeploymentId;
  @Value("${azure.openai.max-completion-tokens:3072}")
  private int maxCompletionTokens;
  @Value("${azure.openai.reasoning-effort:minimal}")
  private String reasoningEffort;
  @Value("${shopping.cart.ai.mock:false}")
  private boolean mockAi;

  @Bean
  public ShoppingCartAiRecommendations shoppingCartRecommendations() {
    if (this.mockAi) {
      return new MockShoppingCartAiRecommendations();
    }
    return new AzureOpenAiShoppingCartAiRecommendations(
        this.azureOpenAiApiKey,
        this.azureOpenAiEndpoint,
        this.azureOpenAiModelDeploymentId,
        this.maxCompletionTokens,
        this.reasoningEffort);
  }
}
