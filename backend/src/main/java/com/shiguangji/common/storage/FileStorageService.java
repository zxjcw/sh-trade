package com.shiguangji.common.storage;

import com.shiguangji.common.config.AppProperties;
import com.shiguangji.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path root;

    public FileStorageService(AppProperties properties) {
        this.root = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(ext)) {
            throw BizException.badRequest("仅支持 jpg/png/webp/gif 图片");
        }
        LocalDate today = LocalDate.now();
        String relativeDir = today.getYear() + "/" + String.format("%02d", today.getMonthValue());
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = root.resolve(relativeDir);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
        } catch (IOException ex) {
            throw BizException.badRequest("图片保存失败，请重试");
        }
        return "/uploads/" + relativeDir + "/" + filename;
    }
}
