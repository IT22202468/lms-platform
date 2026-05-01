package com.lms.course_service.service;

import org.springframework.beans.factory.annotation.Value;
import com.lms.course_service.dto.ServedResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class CourseFileStorageService {

    private final Path root;

    public CourseFileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory: " + this.root, e);
        }
    }

    public Path courseDir(String courseId) {
        return root.resolve("courses").resolve(safeSegment(courseId));
    }

    public void saveThumbnail(String courseId, MultipartFile file, String extension) throws IOException {
        Path dir = courseDir(courseId);
        Files.createDirectories(dir);
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith("thumbnail."))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
        Path target = dir.resolve("thumbnail." + extension.toLowerCase(Locale.ROOT));
        Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * @return null when no thumbnail stored on disk
     */
    public ServedResource resolveThumbnail(String courseId) throws IOException {
        Path dir = courseDir(courseId);
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            Path thumb = stream.filter(p -> p.getFileName().toString().startsWith("thumbnail.")).findFirst().orElse(null);
            if (thumb == null || !Files.isRegularFile(thumb)) {
                return null;
            }
            String probe = Files.probeContentType(thumb);
            MediaType mt = probe != null ? MediaType.parseMediaType(probe) : MediaType.APPLICATION_OCTET_STREAM;
            return new ServedResource(toResource(thumb), mt);
        }
    }

    public void saveMaterialFile(String courseId, String materialId, MultipartFile file) throws IOException {
        Path dir = courseDir(courseId).resolve("materials");
        Files.createDirectories(dir);
        Path target = dir.resolve(safeSegment(materialId));
        Files.copy(file.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public ServedResource resolveMaterialFile(String courseId, String materialId, String mimeForHeaders) throws IOException {
        Path target = courseDir(courseId).resolve("materials").resolve(safeSegment(materialId));
        if (!Files.isRegularFile(target)) {
            return null;
        }
        MediaType mt;
        try {
            mt = mimeForHeaders != null && !mimeForHeaders.isBlank()
                    ? MediaType.parseMediaType(mimeForHeaders)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mt = MediaType.APPLICATION_OCTET_STREAM;
        }
        return new ServedResource(toResource(target), mt);
    }

    public void deleteCourseFiles(String courseId) {
        Path dir = courseDir(courseId);
        try {
            if (Files.isDirectory(dir)) {
                FileSystemUtils.deleteRecursively(dir);
            }
        } catch (IOException ignored) {
        }
    }

    private static Resource toResource(Path path) throws MalformedURLException {
        return new UrlResource(path.toUri());
    }

    private static String safeSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Invalid path segment");
        }
        if (raw.contains("..") || raw.contains("/") || raw.contains("\\")) {
            throw new IllegalArgumentException("Invalid path segment");
        }
        return raw;
    }

    public static final Set<String> THUMB_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    public static String thumbnailExtension(String originalFilename) {
        String ext = extension(originalFilename);
        if (ext == null || !THUMB_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Thumbnail must be jpg, png, gif, or webp");
        }
        return ext.toLowerCase(Locale.ROOT);
    }

    public static String extension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return null;
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
    }
}
