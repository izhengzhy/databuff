package com.databuff.apm.web.ai.platform.skill;

import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import com.databuff.apm.web.ai.platform.AiPlatformApiException;
import com.databuff.apm.web.ai.platform.runtime.SkillFileSyncService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Service
public class SkillPackageService {

    private static final Pattern SKILL_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");
    private static final long MAX_ZIP_BYTES = 20L * 1024 * 1024;
    private static final String SKILL_MD = "SKILL.md";

    private final AgentRuntimeConfig agentRuntimeConfig;
    private final SkillManagementService skillManagementService;
    private final SkillFileSyncService skillFileSyncService;

    public SkillPackageService(
            AgentRuntimeConfig agentRuntimeConfig,
            SkillManagementService skillManagementService,
            SkillFileSyncService skillFileSyncService) {
        this.agentRuntimeConfig = agentRuntimeConfig;
        this.skillManagementService = skillManagementService;
        this.skillFileSyncService = skillFileSyncService;
    }

    public SkillImportPreview previewZip(MultipartFile file) {
        Path tempZip = storeUpload(file);
        try {
            ZipLayout layout = analyzeZip(tempZip);
            Path tempDir = Files.createTempDirectory("skill-preview-");
            try {
                extractZip(tempZip, tempDir, layout.stripPrefix());
                Path skillMd = tempDir.resolve(layout.skillMdRelativePath()).normalize();
                if (!Files.isRegularFile(skillMd)) {
                    throw AiPlatformApiException.badRequest("SKILL.md not found in zip package");
                }
                String markdown = Files.readString(skillMd, StandardCharsets.UTF_8);
                Map<String, String> frontMatter = parseFrontMatter(markdown);
                String skillId = normalizeSkillId(frontMatter.get("name"));
                if (skillId == null) {
                    throw AiPlatformApiException.badRequest("SKILL.md frontmatter must contain name");
                }
                String description = frontMatter.getOrDefault("description", "");
                return new SkillImportPreview(
                        skillId,
                        skillId,
                        description,
                        listFilesUnder(tempDir));
            } finally {
                deleteDirectory(tempDir);
            }
        } catch (IOException e) {
            throw AiPlatformApiException.badRequest("failed to read skill zip: " + e.getMessage());
        } finally {
            deleteQuietly(tempZip);
        }
    }

    public AiSkillDefinition importZip(
            MultipartFile file,
            String displayName,
            String category,
            String description,
            Boolean enabled) {
        if (displayName == null || displayName.isBlank()) {
            throw AiPlatformApiException.badRequest("name is required");
        }
        Path tempZip = storeUpload(file);
        try {
            ZipLayout layout = analyzeZip(tempZip);
            String skillId = readSkillIdFromZip(tempZip, layout);
            if (skillManagementService.find(skillId).isPresent()) {
                throw AiPlatformApiException.conflict("skill_exists", "skill already exists: " + skillId);
            }
            Path targetDir = agentRuntimeConfig.customSkillsDirectory().resolve(skillId).normalize();
            if (Files.exists(targetDir)) {
                throw AiPlatformApiException.conflict("skill_exists", "skill package directory already exists: " + skillId);
            }
            Files.createDirectories(targetDir);
            extractZip(tempZip, targetDir, layout.stripPrefix());
            Path skillMd = targetDir.resolve(layout.skillMdRelativePath()).normalize();
            if (!skillMd.startsWith(targetDir) || !Files.isRegularFile(skillMd)) {
                deleteDirectory(targetDir);
                throw AiPlatformApiException.badRequest("SKILL.md not found after extracting zip package");
            }
            Map<String, String> frontMatter = parseFrontMatter(Files.readString(skillMd, StandardCharsets.UTF_8));
            String resolvedDescription = description == null || description.isBlank()
                    ? frontMatter.getOrDefault("description", "")
                    : description.trim();
            String contentUri = "file:" + skillMd.toAbsolutePath().normalize();
            String filePath = agentRuntimeConfig.customSkillsDirectory()
                    .resolve(skillId)
                    .resolve("SKILL.md")
                    .toString();
            boolean resolvedEnabled = enabled == null || enabled;
            AiSkillDefinition definition = new AiSkillDefinition(
                    skillId,
                    displayName.trim(),
                    category,
                    resolvedDescription,
                    contentUri,
                    filePath,
                    resolvedEnabled,
                    false,
                    1L,
                    "",
                    Instant.now(),
                    Instant.now());
            return skillManagementService.save(definition);
        } catch (IOException e) {
            throw AiPlatformApiException.badRequest("failed to import skill zip: " + e.getMessage());
        } finally {
            deleteQuietly(tempZip);
        }
    }

    public List<SkillFileEntry> listFiles(String skillId) {
        AiSkillDefinition skill = skillManagementService.find(skillId)
                .orElseThrow(() -> AiPlatformApiException.notFound("skill", skillId));
        Path skillDir = resolveSkillDirectory(skillId);
        if (Files.isDirectory(skillDir)) {
            List<SkillFileEntry> files = listFilesUnder(skillDir);
            if (!files.isEmpty()) {
                return files;
            }
        }
        try {
            String markdown = skillFileSyncService.readSkillContent(skill);
            if (markdown != null && !markdown.isBlank()) {
                return List.of(new SkillFileEntry("SKILL.md", markdown.getBytes(StandardCharsets.UTF_8).length));
            }
        } catch (IOException e) {
            throw AiPlatformApiException.badRequest("failed to read skill content: " + e.getMessage());
        }
        return List.of();
    }

    public SkillFileContent readFile(String skillId, String relativePath) {
        AiSkillDefinition skill = skillManagementService.find(skillId)
                .orElseThrow(() -> AiPlatformApiException.notFound("skill", skillId));
        Path skillDir = resolveSkillDirectory(skillId);
        Path target = resolveRelativeFile(skillDir, relativePath);
        if (Files.isRegularFile(target)) {
            try {
                String content = Files.readString(target, StandardCharsets.UTF_8);
                return new SkillFileContent(relativePath.replace('\\', '/'), content, detectContentType(relativePath));
            } catch (IOException e) {
                throw AiPlatformApiException.badRequest("failed to read skill file: " + e.getMessage());
            }
        }
        if ("SKILL.md".equalsIgnoreCase(relativePath.replace('\\', '/'))) {
            try {
                String markdown = skillFileSyncService.readSkillContent(skill);
                if (markdown == null || markdown.isBlank()) {
                    throw AiPlatformApiException.notFound("skill file", relativePath);
                }
                return new SkillFileContent("SKILL.md", markdown, "text/markdown");
            } catch (IOException e) {
                throw AiPlatformApiException.badRequest("failed to read skill file: " + e.getMessage());
            }
        }
        throw AiPlatformApiException.notFound("skill file", relativePath);
    }

    Path resolveSkillDirectory(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            throw AiPlatformApiException.badRequest("skillId is required");
        }
        for (Path root : agentRuntimeConfig.skillSearchDirectories()) {
            Path candidate = root.resolve(skillId).normalize();
            if (Files.isDirectory(candidate) && Files.isRegularFile(candidate.resolve("SKILL.md"))) {
                return candidate;
            }
        }
        return agentRuntimeConfig.customSkillsDirectory().resolve(skillId).normalize();
    }

    private void ensureSkillExists(String skillId) {
        skillManagementService.find(skillId)
                .orElseThrow(() -> AiPlatformApiException.notFound("skill", skillId));
    }

    private Path storeUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AiPlatformApiException.badRequest("zip file is required");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".zip")) {
            throw AiPlatformApiException.badRequest("only .zip packages are supported");
        }
        if (file.getSize() > MAX_ZIP_BYTES) {
            throw AiPlatformApiException.badRequest("zip file exceeds max size: 20MB");
        }
        try {
            Path tempZip = Files.createTempFile("skill-import-", ".zip");
            file.transferTo(tempZip);
            return tempZip;
        } catch (IOException e) {
            throw AiPlatformApiException.badRequest("failed to store uploaded zip: " + e.getMessage());
        }
    }

    private String readSkillIdFromZip(Path zipPath, ZipLayout layout) throws IOException {
        Path tempDir = Files.createTempDirectory("skill-id-");
        try {
            extractZip(zipPath, tempDir, layout.stripPrefix());
            Path skillMd = tempDir.resolve(layout.skillMdRelativePath()).normalize();
            String markdown = Files.readString(skillMd, StandardCharsets.UTF_8);
            Map<String, String> frontMatter = parseFrontMatter(markdown);
            String skillId = normalizeSkillId(frontMatter.get("name"));
            if (skillId == null) {
                throw AiPlatformApiException.badRequest("SKILL.md frontmatter must contain name");
            }
            return skillId;
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private ZipLayout analyzeZip(Path zipPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.stream()
                    .map(ZipEntry::getName)
                    .filter(this::isRelevantEntry)
                    .forEach(entries::add);
        }
        if (entries.isEmpty()) {
            throw AiPlatformApiException.badRequest("zip package is empty");
        }
        String skillMdPath = entries.stream()
                .filter(name -> name.endsWith("/" + SKILL_MD) || SKILL_MD.equals(name))
                .min(Comparator.comparingInt(String::length))
                .orElseThrow(() -> AiPlatformApiException.badRequest("SKILL.md is required in zip package"));
        String stripPrefix = "";
        int slashIdx = skillMdPath.lastIndexOf('/');
        if (slashIdx >= 0) {
            stripPrefix = skillMdPath.substring(0, slashIdx + 1);
            String prefix = stripPrefix;
            boolean allUnderPrefix = entries.stream().allMatch(name -> name.startsWith(prefix));
            if (!allUnderPrefix) {
                stripPrefix = "";
            }
        }
        String relativeSkillMd = stripPrefix.isBlank()
                ? skillMdPath
                : skillMdPath.substring(stripPrefix.length());
        return new ZipLayout(stripPrefix, relativeSkillMd);
    }

    private void extractZip(Path zipPath, Path targetDir, String stripPrefix) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !isRelevantEntry(entry.getName())) {
                    continue;
                }
                String entryName = entry.getName();
                if (!stripPrefix.isBlank() && entryName.startsWith(stripPrefix)) {
                    entryName = entryName.substring(stripPrefix.length());
                } else if (!stripPrefix.isBlank()) {
                    continue;
                }
                if (entryName.isBlank()) {
                    continue;
                }
                Path dest = targetDir.resolve(entryName).normalize();
                if (!dest.startsWith(targetDir.normalize())) {
                    throw new IOException("zip entry escapes target directory: " + entry.getName());
                }
                Files.createDirectories(dest.getParent());
                Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private List<SkillFileEntry> listFilesUnder(Path rootDir) {
        List<SkillFileEntry> files = new ArrayList<>();
        try (var paths = Files.walk(rootDir)) {
            paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> rootDir.relativize(path).toString()))
                    .forEach(path -> {
                        String relative = rootDir.relativize(path).toString().replace('\\', '/');
                        try {
                            files.add(new SkillFileEntry(relative, Files.size(path)));
                        } catch (IOException e) {
                            files.add(new SkillFileEntry(relative, 0L));
                        }
                    });
        } catch (IOException e) {
            throw AiPlatformApiException.badRequest("failed to list skill files: " + e.getMessage());
        }
        return files;
    }

    private Path resolveRelativeFile(Path skillDir, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw AiPlatformApiException.badRequest("path is required");
        }
        String normalized = relativePath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            throw AiPlatformApiException.badRequest("path must not contain '..'");
        }
        return skillDir.resolve(normalized).normalize();
    }

    static Map<String, String> parseFrontMatter(String markdown) {
        if (markdown == null || !markdown.startsWith("---")) {
            return Map.of();
        }
        int end = markdown.indexOf("\n---", 3);
        if (end < 0) {
            return Map.of();
        }
        String block = markdown.substring(3, end).trim();
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : block.split("\n")) {
            int idx = line.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            result.put(key, value);
        }
        return result;
    }

    private static String normalizeSkillId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return SKILL_ID_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    private static String detectContentType(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "text/markdown";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            return "text/yaml";
        }
        if (lower.endsWith(".py")) {
            return "text/x-python";
        }
        if (lower.endsWith(".sh")) {
            return "text/x-shellscript";
        }
        return "text/plain";
    }

    private boolean isRelevantEntry(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !name.startsWith("__MACOSX/")
                && !name.endsWith("/")
                && !name.endsWith(".DS_Store");
    }

    private static void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> deleteQuietly(path));
        } catch (IOException ignored) {
            deleteQuietly(dir);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort cleanup
        }
    }

    private record ZipLayout(String stripPrefix, String skillMdRelativePath) {
    }

    public record SkillImportPreview(
            String skillId,
            String skillName,
            String description,
            List<SkillFileEntry> files) {
    }

    public record SkillFileEntry(String path, long size) {
    }

    public record SkillFileContent(String path, String content, String contentType) {
    }
}
