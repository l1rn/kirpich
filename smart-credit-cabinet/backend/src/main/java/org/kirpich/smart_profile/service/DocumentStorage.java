package org.kirpich.smart_profile.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.kirpich.smart_profile.config.AppProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Файлы документов на диске (том Docker). В проде — объектное хранилище, см. README → «Как развивать». */
@Component
public class DocumentStorage {

    private final Path root;

    public DocumentStorage(AppProperties props) {
        this.root = Path.of(props.uploadDir()).toAbsolutePath().normalize();
    }

    public record StoredFile(String path, long size, String sha256) {
    }

    public StoredFile store(Long applicationId, MultipartFile file) {
        try {
            Path dir = root.resolve(String.valueOf(applicationId));
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename()));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(in, target);
            }
            return new StoredFile(root.relativize(target).toString(), Files.size(target), HexFormat.of().formatHex(digest.digest()));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить документ", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Resource load(String storagePath) {
        Path file = root.resolve(storagePath).normalize();
        if (!file.startsWith(root)) {
            throw new NotFoundException("Документ не найден");
        }
        return new FileSystemResource(file);
    }

    static String sanitize(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "document";
        }
        String name = originalName.substring(Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return name.length() > 100 ? name.substring(name.length() - 100) : name;
    }
}
