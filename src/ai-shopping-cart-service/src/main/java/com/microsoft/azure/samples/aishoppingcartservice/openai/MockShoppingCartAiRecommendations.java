package com.microsoft.azure.samples.aishoppingcartservice.openai;

import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.azure.samples.aishoppingcartservice.cartitem.CartItem;

public class MockShoppingCartAiRecommendations implements ShoppingCartAiRecommendations {
  public String getAINutritionAnalysis(final List<CartItem> cartItems) {
    final String items = escapeJson(getCartItemsAsString(cartItems));
    return """
        {
          "provider": "azure-openai",
          "model": "mock-azure-chat-deployment",
          "mock": true,
          "nutriscore": "B",
          "explanation": "Mock Azure OpenAI nutrition analysis for %s.",
          "recommendation": "Balance produce with protein and add fiber-rich ingredients where possible."
        }
        """.formatted(items);
  }

  public String getTop3Recipes(final List<CartItem> cartItems) {
    final String ingredients = getCartItemsAsJsonArray(cartItems);
    return """
        {
          "provider": "azure-openai",
          "model": "mock-azure-chat-deployment",
          "mock": true,
          "recipes": [
            {
              "name": "Roasted market bowl",
              "ingredients": [%s],
              "instructions": ["Prepare the ingredients.", "Roast until cooked through and serve warm."],
              "nutriscore": "B"
            },
            {
              "name": "Quick skillet dinner",
              "ingredients": [%s],
              "instructions": ["Prepare the ingredients.", "Cook together in a skillet and season to taste."],
              "nutriscore": "B"
            },
            {
              "name": "Simple soup or salad",
              "ingredients": [%s],
              "instructions": ["Prepare the ingredients.", "Combine as a soup or salad and serve."],
              "nutriscore": "A"
            }
          ]
        }
        """.formatted(ingredients, ingredients, ingredients);
  }

  private String getCartItemsAsString(final List<CartItem> cartItems) {
    return cartItems
        .stream()
        .map(CartItem::getName)
        .map(String::toLowerCase)
        .map(String::trim)
        .collect(Collectors.joining(", "));
  }

  private String getCartItemsAsJsonArray(final List<CartItem> cartItems) {
    return cartItems
        .stream()
        .map(CartItem::getName)
        .map(String::trim)
        .map(this::escapeJson)
        .map(item -> "\"" + item + "\"")
        .collect(Collectors.joining(", "));
  }

  private String escapeJson(final String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
