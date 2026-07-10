package com.nomlog.backend.record;

import com.nomlog.backend.auth.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    public RecordResponse create(@RequestBody @Valid RecordCreateRequest request) {
        return recordService.create(CurrentUser.id(), request);
    }

    @GetMapping
    public List<RecordResponse> list(@RequestParam LocalDate date) {
        return recordService.listByDate(CurrentUser.id(), date);
    }

    @PatchMapping("/{id}")
    public RecordResponse update(@PathVariable Long id, @RequestBody RecordUpdateRequest request) {
        return recordService.update(CurrentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recordService.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam AutocompleteField field, @RequestParam String q) {
        return recordService.autocomplete(CurrentUser.id(), field, q);
    }
}
