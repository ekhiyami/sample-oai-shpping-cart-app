package com.microsoft.azure.samples.aishoppingcartservice.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Retrieval is explicit application orchestration; generation still uses Chat Completions. */
@Service
public class RagSearchService {
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ObjectMapper mapper = new ObjectMapper();
  private static final Set<String> QUESTION_WORDS = Set.of("the","and","for","with","from","about",
      "what","which","why","how","can","could","should","would","does","did","are","was","were",
      "this","that","these","those","there","they","them","please","tell","show","more","have","has","your","our");
  private final String endpoint, key, index, corpusVersion;
  public RagSearchService(@Value("${rag.search.endpoint:}") String endpoint,
                          @Value("${rag.search.key:}") String key,
                          @Value("${rag.search.index:nutrition-v1}") String index,
                          @Value("${rag.corpus-version:1.0}") String corpusVersion) {
    this.endpoint = endpoint.replaceAll("/+$", ""); this.key = key;
    this.index = index; this.corpusVersion = corpusVersion;
  }

  public Retrieval retrieve(String question) {
    long started = System.nanoTime();
    if (endpoint.isBlank() || key.isBlank() || !index.matches("[a-z0-9-]+") || !corpusVersion.matches("[0-9.]+"))
      throw new RetrievalUnavailable();
    String filter = "access_scope eq 'demo-owner' and status eq 'approved-for-demo' " +
        "and dataset eq 'synthetic-demo' and corpus_version eq '" + corpusVersion + "'";
    // Vector similarity alone always returns nearest neighbors, even for an unrelated question.
    // Require a lexical match as well for this deliberately small, controlled demonstration corpus.
    String keywords = String.join(" ",Arrays.stream(question.toLowerCase(Locale.ROOT).split("[^a-z0-9-]+"))
        .filter(w -> w.length()>2 && !QUESTION_WORDS.contains(w)).distinct().limit(30).toList());
    if (keywords.isBlank()) return new Retrieval(List.of(),elapsed(started),index,corpusVersion,false);
    JsonNode lexical = search(Map.of("search", keywords, "searchFields", "content,title",
        "select", "chunk_id", "top", 50, "filter", filter));
    Set<String> matched = new HashSet<>();
    lexical.path("value").forEach(v -> matched.add(v.path("chunk_id").asText()));
    if (matched.isEmpty()) return new Retrieval(List.of(), elapsed(started), index, corpusVersion, false);
    JsonNode hybrid = search(Map.of(
        "search", question, "searchFields", "content,title", "top", 12, "filter", filter,
        "select", "chunk_id,document_id,title,content,version,updated_date,product_ids,source_file",
        "vectorFilterMode", "preFilter",
        "vectorQueries", List.of(Map.of("kind", "text", "text", question,
            "fields", "content_vector", "k", 20))));
    List<Source> sources = new ArrayList<>();
    for (JsonNode row : hybrid.path("value")) {
      if (!matched.contains(row.path("chunk_id").asText()) || row.path("content").asText().isBlank()) continue;
      List<String> products = new ArrayList<>();
      row.path("product_ids").forEach(p -> products.add(p.asText()));
      sources.add(new Source("S" + (sources.size() + 1), row.path("chunk_id").asText(),
          row.path("document_id").asText(), row.path("title").asText(), row.path("content").asText(),
          row.path("version").asText(), row.path("updated_date").asText(), products));
      if (sources.size() == 4) break;
    }
    return new Retrieval(List.copyOf(sources), elapsed(started), index, corpusVersion, true);
  }

  private JsonNode search(Map<String, Object> body) {
    try {
      var request = HttpRequest.newBuilder(URI.create(endpoint + "/indexes/" + index + "/docs/search?api-version=2024-07-01"))
          .timeout(Duration.ofSeconds(30)).header("api-key", key).header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) throw new RetrievalUnavailable();
      JsonNode result = mapper.readTree(response.body());
      if (result == null || !result.path("value").isArray()) throw new RetrievalUnavailable();
      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); throw new RetrievalUnavailable();
    } catch (java.io.IOException | IllegalArgumentException e) { throw new RetrievalUnavailable(); }
  }
  private long elapsed(long started) { return Math.max(0, (System.nanoTime() - started) / 1_000_000); }
  public record Source(String id, String chunkId, String documentId, String title, String content,
                       String version, String updatedDate, List<String> productIds) {}
  public record Retrieval(List<Source> sources, long retrievalMs, String index, String corpusVersion, boolean queryVectorized) {}
  public static class RetrievalUnavailable extends RuntimeException {}
}
