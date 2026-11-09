package com.microsoft.azure.samples.aishoppingcartservice.nutrition;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NutritionFactRepository extends JpaRepository<NutritionFact, String> {}
