package com.microsoft.azure.samples.aishoppingcartservice.nutrition;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NutritionSeed implements ApplicationRunner {
  private final NutritionFactRepository repository;
  private final boolean enabled;
  public NutritionSeed(NutritionFactRepository repository, @Value("${nutrition.lookup.enabled:false}") boolean enabled) {
    this.repository = repository; this.enabled = enabled;
  }
  @Override @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (!enabled) return;
    try (var reader = new BufferedReader(new InputStreamReader(
        new ClassPathResource("nutrition-facts.csv").getInputStream(), StandardCharsets.UTF_8))) {
      if (!reader.readLine().startsWith("product_id,")) throw new IllegalArgumentException("Invalid fixture header");
      for (String line; (line = reader.readLine()) != null;) {
        if (line.isBlank()) continue;
        NutritionFact fact = NutritionFact.fromFixture(line);
        // Preserve any later edits and record versions already in the database.
        if (!repository.existsById(fact.getProductId())) repository.save(fact);
      }
    }
  }
}
