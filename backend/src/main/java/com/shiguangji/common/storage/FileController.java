package com.shiguangji.common.storage;

import com.shiguangji.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storageService;

    public record UploadResult(String url) {
    }

    @PostMapping
    public Result<UploadResult> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(new UploadResult(storageService.store(file)));
    }
}
