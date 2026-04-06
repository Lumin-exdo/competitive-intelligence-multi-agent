package com.ci.api;

import com.ci.model.PipelineState;
import com.ci.workflow.CIPipeline;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CIController {

    private final CIPipeline pipeline;

    public CIController(CIPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/analyze")
    public ResponseEntity<PipelineState> analyze(@RequestBody AnalyzeRequest request) {
        PipelineState result = pipeline.run(request.competitor());
        return ResponseEntity.ok(result);
    }

    public record AnalyzeRequest(String competitor) {}
}
