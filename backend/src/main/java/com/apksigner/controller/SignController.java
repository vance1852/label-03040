package com.apksigner.controller;

import com.apksigner.dto.BatchSignRequest;
import com.apksigner.dto.R;
import com.apksigner.dto.SignRequest;
import com.apksigner.entity.SignHistory;
import com.apksigner.service.SignHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sign")
@RequiredArgsConstructor
public class SignController {

    private final SignHistoryService signHistoryService;

    @PostMapping("/upload")
    public R<SignHistory> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(signHistoryService.uploadFile(file));
    }

    @PostMapping("/execute")
    public R<SignHistory> execute(@Valid @RequestBody SignRequest request) {
        return R.ok(signHistoryService.executeSign(request));
    }

    @PostMapping("/batch")
    public R<List<SignHistory>> batchSign(@Valid @RequestBody BatchSignRequest request) {
        return R.ok(signHistoryService.executeBatchSign(request));
    }

    @GetMapping("/status/{id}")
    public R<SignHistory> getStatus(@PathVariable Long id) {
        SignHistory history = signHistoryService.getSanitizedById(id);
        if (history == null) {
            return R.fail(404, "记录不存在");
        }
        return R.ok(history);
    }
}
