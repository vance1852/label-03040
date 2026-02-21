package com.apksigner.controller;

import com.apksigner.dto.R;
import com.apksigner.entity.SignHistory;
import com.apksigner.service.SignHistoryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final SignHistoryService signHistoryService;

    @GetMapping("/list")
    public R<Page<SignHistory>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(signHistoryService.getHistoryPage(page, size));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        signHistoryService.deleteHistory(id);
        return R.ok();
    }

    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        signHistoryService.batchDelete(ids);
        return R.ok();
    }
}
