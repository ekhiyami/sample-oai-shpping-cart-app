package com.microsoft.azure.samples.aishoppingcartservice.openai;

public final class UserMessageConstants {
  public static final String STRICT_JSON_AI_NUTRITION_ANALYSIS_POSTFIX = "Return only a JSON with the following format: { nutriscore: \"\", explanation: \"\", recommendation: \"\"}. Do not add any other text.";

  public static final String STRICT_JSON_RECIPES_POSTFIX = "Even if the basket is too small, propose 3 recipes. Return only a JSON with the following format: { recipes: [ { name: \"\", ingredients: [], instructions: [], nutriscore: \"\", }]}. Do not add any other text.";

  private UserMessageConstants() {
  }
}
