package com.microsoft.azure.samples.aishoppingcartservice.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.math.BigDecimal;
import com.microsoft.azure.samples.aishoppingcartservice.nutrition.NutritionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Azure Easy Auth protects /api/*; new chat writes also require a session CSRF token. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
  private static final String STATE = ChatController.class.getName();
  private final boolean enabled;
  private final String ownerId;
  private final String origin;
  private final boolean ragEnabled;
  private final RagSearchService retrieval;
  private final boolean nutritionEnabled;
  private final NutritionService nutrition;
  private final boolean aiEnabled;
  private final GroundedChatService grounded;

  public ChatController(@Value("${chat.enabled:false}") boolean enabled,
                        @Value("${chat.owner-id:}") String ownerId,
                        @Value("${chat.origin:}") String origin,
                        @Value("${rag.enabled:false}") boolean ragEnabled,
                        RagSearchService retrieval,
                        @Value("${nutrition.lookup.enabled:false}") boolean nutritionEnabled,
                        NutritionService nutrition,
                        @Value("${chat.ai-enabled:false}") boolean aiEnabled,
                        GroundedChatService grounded) {
    this.enabled = enabled; this.ownerId = ownerId; this.origin = origin;
    this.ragEnabled = ragEnabled; this.retrieval = retrieval;
    this.nutritionEnabled = nutritionEnabled; this.nutrition = nutrition;
    this.aiEnabled = aiEnabled; this.grounded = grounded;
  }

  private void owner(HttpServletRequest request) {
    if (!ownerId.isBlank() && !ownerId.equals(request.getHeader("X-MS-CLIENT-PRINCIPAL-ID")))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  private State state(HttpSession session) {
    synchronized (session) {
      State state = (State) session.getAttribute(STATE);
      if (state == null) { state = new State(); session.setAttribute(STATE, state); }
      return state;
    }
  }

  private State access(HttpServletRequest request, boolean write) {
    owner(request);
    if (!enabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    State state = state(request.getSession());
    if (write) {
      String token = request.getHeader("X-CSRF-TOKEN");
      String requestOrigin = request.getHeader("Origin");
      if (token == null || !MessageDigest.isEqual(state.csrf.getBytes(StandardCharsets.UTF_8),
          token.getBytes(StandardCharsets.UTF_8)) ||
          (!origin.isBlank() && requestOrigin != null && !origin.equals(requestOrigin)))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return state;
  }

  @GetMapping("/config")
  public Map<String, Object> config(HttpServletRequest request, HttpServletResponse response) {
    owner(request);
    response.setHeader("Cache-Control", "no-store");
    if (!enabled) return Map.of("enabled", false);
    return Map.of("enabled", true, "mode", aiEnabled ? (ragEnabled && nutritionEnabled ? "grounded" : "unavailable") : "preview", "retrievalEnabled", ragEnabled,
        "nutritionEnabled", nutritionEnabled, "csrfToken", state(request.getSession()).csrf);
  }

  @GetMapping
  public Map<String, Object> history(HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    State state = access(request, false);
    synchronized (state) {
      Map<String,Object> result = new LinkedHashMap<>();
      result.put("messages",List.copyOf(state.messages)); result.put("lastTrace",state.lastTrace);
      return result;
    }
  }

  @PostMapping
  public Object chat(@RequestBody ChatRequest body, HttpServletRequest request,
                                   HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    State state = access(request, true);
    if (body.message() == null || body.message().isBlank() || body.message().length() > 2000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a message of 1–2,000 characters.");
    synchronized (state) {
      if (aiEnabled) {
        if (!ragEnabled || !nutritionEnabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        try {
          var result = grounded.answer(body.message().trim(),state.messages.stream()
              .map(m -> Map.of("role",m.role(),"content",m.content())).toList());
          state.lastTrace = result.trace();
          state.messages.add(new Message("user",body.message().trim(),List.of()));
          state.messages.add(new Message("assistant",result.answer(),result.citations()));
          while (state.messages.size() > 12) state.messages.remove(0);
          return result;
        } catch (GroundedChatService.ChatFailure e) { state.lastTrace=e.trace; throw e; }
      }
      String answer = "Preview: your message reached this chat session. Retrieval and AI answers are not connected yet. No AI call was made.";
      state.messages.add(new Message("user",body.message().trim(),List.of()));
      state.messages.add(new Message("assistant",answer,List.of()));
      while (state.messages.size() > 12) state.messages.remove(0);
      return Map.of("answer", answer, "mode", "preview", "ragUsed", false, "generationSent", false);
    }
  }

  @DeleteMapping
  public Map<String, Object> reset(HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    State state = access(request, true);
    synchronized (state) { state.messages.clear(); state.lastTrace=null; }
    return Map.of("cleared", true);
  }

  @PostMapping("/retrieval")
  public Map<String, Object> retrieve(@RequestBody ChatRequest body, HttpServletRequest request,
                                       HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    access(request, true);
    if (!ragEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    if (body.message() == null || body.message().isBlank() || body.message().length() > 2000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    var result = retrieval.retrieve(body.message().trim());
    return Map.of("sources", result.sources(), "retrievalMs", result.retrievalMs(),
        "index", result.index(), "corpusVersion", result.corpusVersion(),
        "ragUsed", false, "generationSent", false,
        "queryVectorized", result.queryVectorized(),
        "message", result.sources().isEmpty() ? "No matching demo passages were found. No answer-generation call was made."
            : "Supporting demo passages retrieved. They have not been sent to the answer-generation model.");
  }

  @ExceptionHandler(RagSearchService.RetrievalUnavailable.class)
  public ResponseEntity<Map<String, Object>> retrievalFailure() {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
        "error", "Document retrieval is unavailable. No AI answer was requested.",
        "ragUsed", false, "generationSent", false));
  }

  @ExceptionHandler(GroundedChatService.ChatFailure.class)
  public ResponseEntity<Map<String,Object>> generationFailure(GroundedChatService.ChatFailure failure) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error",failure.getMessage(),
        "ragUsed",failure.trace.get("ragUsed"),"generationSent",failure.trace.get("generationSent"),"trace",failure.trace));
  }

  @GetMapping("/nutrition")
  public NutritionService.Snapshot nutrition(HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Cache-Control","no-store");
    access(request,false);
    if (!nutritionEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return nutrition.snapshot();
  }

  @PostMapping("/portions")
  public NutritionService.Snapshot portion(@RequestBody Portion body, HttpServletRequest request,
                                           HttpServletResponse response) {
    response.setHeader("Cache-Control","no-store");
    access(request,true);
    if (!nutritionEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    nutrition.setPortion(body.itemId(),body.productId(),body.totalGrams());
    return nutrition.snapshot();
  }

  public record ChatRequest(String message) {}
  public record Portion(long itemId, String productId, BigDecimal totalGrams) {}
  public record Message(String role,String content,List<Map<String,Object>> citations) implements Serializable {}
  private static final class State implements Serializable {
    final String csrf = UUID.randomUUID().toString();
    final List<Message> messages = new ArrayList<>();
    Map<String,Object> lastTrace;
  }
}
