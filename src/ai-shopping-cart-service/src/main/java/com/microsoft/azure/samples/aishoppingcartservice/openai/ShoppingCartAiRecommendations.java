package com.microsoft.azure.samples.aishoppingcartservice.openai;

import java.util.List;

import com.microsoft.azure.samples.aishoppingcartservice.cartitem.CartItem;

public interface ShoppingCartAiRecommendations {
  String getAINutritionAnalysis(List<CartItem> cartItems);

  String getTop3Recipes(List<CartItem> cartItems);
}
