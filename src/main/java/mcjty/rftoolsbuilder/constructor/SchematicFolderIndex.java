package mcjty.rftoolsbuilder.constructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Shared index of locally available schematic files.
 *
 * The Constructor is deliberately format-neutral. Files are discovered here,
 * then normalized by SchematicPlanLoader through a format adapter.
 */
public final class SchematicFolderIndex {
    public enum Format {
        VANILLA_NBT("vanilla_nbt", ".nbt", "Create / Structure NBT"),
        SPONGE_SCHEM("sponge_schem", ".schem", "WorldEdit / Sponge"),
        LITEMATICA("litematica", ".litematic", "Litematica"),
        LEGACY_SCHEMATIC("legacy_schematic", ".schematic", "Legacy MCEdit / Schematica");

        private final String id;
        private final String extension;
        private final String label;

        Format(String id, String extension, String label) {
            this.id = id;
            this.extension = extension;
            this.label = label;
        }

        public String id() { return id; }
        public String extension() { return extension; }
        public String label() { return label; }

        public static Format fromId(String id) {
            if (id == null) return null;
            // dev.6 compatibility
            if (id.equals("create_nbt")) return VANILLA_NBT;
            for (Format format : values()) {
                if (format.id.equals(id)) return format;
            }
            return null;
        }

        public static Format fromFileName(String fileName) {
            if (fileName == null) return null;
            String lower = fileName.toLowerCase(Locale.ROOT);
            for (Format format : values()) {
                if (lower.endsWith(format.extension)) return format;
            }
            return null;
        }
    }

    public record Entry(String fileName, Format format) {
        public String displayName() {
            int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
            return slash >= 0 ? fileName.substring(slash + 1) : fileName;
        }
    }

    private static final Path DIRECTORY = Path.of("schematics").toAbsolutePath().normalize();
    private static final int MAX_SCAN_DEPTH = 8;

    private SchematicFolderIndex() {}

    public static Path directory() {
        return DIRECTORY;
    }

    public static List<Entry> list() {
        try {
            Files.createDirectories(DIRECTORY);
            ArrayList<Entry> entries = new ArrayList<>();
            try (var stream = Files.walk(DIRECTORY, MAX_SCAN_DEPTH)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    Path relative = DIRECTORY.relativize(path.toAbsolutePath().normalize());
                    String name = relative.toString().replace('\\', '/');
                    Format format = Format.fromFileName(name);
                    if (format != null) entries.add(new Entry(name, format));
                });
            }
            entries.sort(Comparator.comparing(e -> naturalKey(e.fileName())));
            return List.copyOf(entries);
        } catch (IOException | RuntimeException ignored) {
            return List.of();
        }
    }

    public static Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        Path path = DIRECTORY.resolve(fileName.replace('\\', '/')).normalize();
        if (!path.startsWith(DIRECTORY)) return null;
        return path;
    }

    private static String naturalKey(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (Format format : Format.values()) {
            if (lower.endsWith(format.extension())) {
                lower = lower.substring(0, lower.length() - format.extension().length());
                break;
            }
        }
        StringBuilder key = new StringBuilder(lower.length() + 16);
        int i = 0;
        while (i < lower.length()) {
            char c = lower.charAt(i);
            if (!Character.isDigit(c)) {
                key.append(c);
                i++;
                continue;
            }
            int start = i;
            while (i < lower.length() && Character.isDigit(lower.charAt(i))) i++;
            String number = lower.substring(start, i);
            try {
                key.append('#').append(String.format("%012d", Long.parseLong(number)));
            } catch (NumberFormatException ignored) {
                key.append('#').append(number);
            }
        }
        return key.toString();
    }
}
