package com.microsoft.azure.samples.aishoppingcartservice.nutrition;

import com.microsoft.azure.samples.aishoppingcartservice.cartitem.CartItem;
import com.microsoft.azure.samples.aishoppingcartservice.cartitem.CartItemRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NutritionService {
  private final NutritionFactRepository facts;
  private final CartItemRepository cart;
  public NutritionService(NutritionFactRepository facts, CartItemRepository cart) { this.facts = facts; this.cart = cart; }

  @Transactional(readOnly=true)
  public Snapshot snapshot() {
    List<NutritionFact> records = facts.findAll().stream().filter(NutritionFact::isApproved)
        .sorted(Comparator.comparing(NutritionFact::getProductId)).toList();
    Map<String,NutritionFact> byId = new HashMap<>();
    records.forEach(r -> byId.put(r.getProductId(), r));
    List<Line> lines = new ArrayList<>();
    Map<String,BigDecimal> sums = new LinkedHashMap<>();
    boolean complete = true;
    for (CartItem item : cart.findAll()) {
      // Old app versions can edit the cart during rollback without knowing the new fields.
      // Bind a portion to the name/count that the owner explicitly confirmed.
      boolean mappingCurrent = Objects.equals(item.getName(),item.getPortionName()) &&
          Objects.equals(item.getQuantity(),item.getPortionQuantity());
      NutritionFact fact = mappingCurrent ? byId.get(item.getProductId()) : null;
      BigDecimal grams = mappingCurrent ? item.getTotalGrams() : null;
      boolean weighed = grams != null && grams.signum() > 0 && grams.compareTo(new BigDecimal("100000")) <= 0;
      Map<String,BigDecimal> totals = null;
      String message = fact == null ? "Choose the exact demo product." : !weighed ? "Enter the total weight for this cart line." : "Calculated from the selected product record.";
      if (fact == null || !weighed) complete = false;
      else {
        totals = new LinkedHashMap<>();
        for (var nutrient : fact.nutrients().entrySet()) {
          BigDecimal value = nutrient.getValue().multiply(grams).divide(fact.getBasisGrams(),MathContext.DECIMAL128);
          totals.put(nutrient.getKey(),value.setScale(2,RoundingMode.HALF_UP));
          sums.merge(nutrient.getKey(),value,BigDecimal::add);
        }
      }
      lines.add(new Line(item.getId(),item.getName(),item.getQuantity(),fact == null ? null : item.getProductId(),grams,
          fact == null ? null : fact.view().version(),message,totals));
    }
    Map<String,BigDecimal> aggregate = null;
    if (complete && !lines.isEmpty()) {
      aggregate = new LinkedHashMap<>();
      for (var value : sums.entrySet()) aggregate.put(value.getKey(),value.getValue().setScale(2,RoundingMode.HALF_UP));
    }
    return new Snapshot(records.stream().map(NutritionFact::view).toList(), List.copyOf(lines), aggregate);
  }

  @Transactional
  public void setPortion(long itemId, String productId, BigDecimal grams) {
    CartItem item = cart.findById(itemId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (productId == null || productId.isBlank()) {
      if (grams != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      item.setProductId(null); item.setTotalGrams(null);
      item.setPortionName(null); item.setPortionQuantity(null);
    } else {
      NutritionFact fact = facts.findById(productId).filter(NutritionFact::isApproved)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
      if (grams != null && (grams.signum() <= 0 || grams.compareTo(new BigDecimal("100000")) > 0 || grams.scale() > 3))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      item.setProductId(fact.getProductId()); item.setTotalGrams(grams);
      item.setPortionName(item.getName()); item.setPortionQuantity(item.getQuantity());
    }
    cart.save(item);
  }
  public record Line(long itemId, String name, int quantity, String productId, BigDecimal totalGrams,
                     String recordVersion, String message, Map<String,BigDecimal> totals) {}
  public record Snapshot(List<NutritionFact.FactView> records, List<Line> lines, Map<String,BigDecimal> cartTotals) {}
}
