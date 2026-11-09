package com.microsoft.azure.samples.aishoppingcartservice.nutrition;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class NutritionFact {
  @Id private String productId;
  private String productName;
  private String preparation;
  private BigDecimal basisGrams;
  private BigDecimal servingGrams;
  private BigDecimal energyKcal;
  private BigDecimal proteinG;
  private BigDecimal totalFatG;
  private BigDecimal saturatedFatG;
  private BigDecimal carbohydrateG;
  private BigDecimal sugarG;
  private BigDecimal fiberG;
  private BigDecimal sodiumMg;
  private String recordVersion;
  private String updatedDate;
  private String status;
  private String dataset;
  protected NutritionFact() {}

  static NutritionFact fromFixture(String line) {
    // This packaged synthetic fixture has a fixed, validated format and no quoted commas.
    String[] v = line.split(",", -1);
    if (v.length != 17) throw new IllegalArgumentException("Invalid nutrition fixture");
    NutritionFact f = new NutritionFact();
    f.productId = v[0]; f.productName = v[1]; f.preparation = v[2];
    f.basisGrams = new BigDecimal(v[3]); f.servingGrams = new BigDecimal(v[4]);
    f.energyKcal = new BigDecimal(v[5]); f.proteinG = new BigDecimal(v[6]);
    f.totalFatG = new BigDecimal(v[7]); f.saturatedFatG = new BigDecimal(v[8]);
    f.carbohydrateG = new BigDecimal(v[9]); f.sugarG = new BigDecimal(v[10]);
    f.fiberG = new BigDecimal(v[11]); f.sodiumMg = new BigDecimal(v[12]);
    f.recordVersion = v[13]; f.updatedDate = v[14]; f.status = v[15]; f.dataset = v[16];
    if (f.basisGrams.signum() <= 0 || !f.isApproved()) throw new IllegalArgumentException("Invalid nutrition basis/status");
    return f;
  }
  public boolean isApproved() { return "approved-for-demo".equals(status) && "synthetic-demo".equals(dataset); }
  public String getProductId() { return productId; }
  public BigDecimal getBasisGrams() { return basisGrams; }
  public Map<String, BigDecimal> nutrients() {
    Map<String, BigDecimal> values = new LinkedHashMap<>();
    values.put("energy_kcal", energyKcal); values.put("protein_g", proteinG);
    values.put("total_fat_g", totalFatG); values.put("saturated_fat_g", saturatedFatG);
    values.put("carbohydrate_g", carbohydrateG); values.put("sugar_g", sugarG);
    values.put("fiber_g", fiberG); values.put("sodium_mg", sodiumMg);
    return values;
  }
  public FactView view() {
    return new FactView(productId, productName, preparation, basisGrams, servingGrams, nutrients(), recordVersion, updatedDate);
  }
  public record FactView(String productId, String productName, String preparation, BigDecimal basisGrams,
                         BigDecimal servingGrams, Map<String, BigDecimal> nutrients, String version, String updatedDate) {}
}
