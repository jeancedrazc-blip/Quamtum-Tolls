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
 * Mirrors Create's user-facing convention of reading from the instance-level
 * schematics/ folder.
 */
public final class SchematicFolderIndex {
    public enum Format {
        CREATE_NBT("create_nbt", ".nbt");

        private final String id;
        private final String extension;

        Format(String id, String extension) {
            this.id = id;
            this.extension = extension;
        }

        public String id() { return id; }
        public String extension() { return extension; }
    }

    public record Entry(String fileName, Format format) {}

    private static final Path DIRECTORY = Path.of("schematics").toAbsolutePath().normalize();

    private SchematicFolderIndex() {}

    public static Path directory() {
        return DIRECTORY;
    }

    public static List<Entry> list() {
        try {
            Files.createDirectories(DIRECTORY);
            ArrayList<Entry> entries = new ArrayList<>();
            try (var stream = Files.list(DIRECTORY)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    String name = path.getFileName().toString();
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (lower.endsWith(Format.CREATE_NBT.extension())) {
                        entries.add(new Entry(name, Format.CREATE_NBT));
                    }
                });
            }
            entries.sort(Comparator.comparing(e -> naturalKey(e.fileName())));
            return List.copyOf(entries);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    public static Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        Path path = DIRECTORY.resolve(fileName).normalize();
        if (!path.startsWith(DIRECTORY)) return null;
        return path;
    }

    private static String naturalKey(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".nbt")) lower = lower.substring(0, lower.length() - 4);
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
            key.append('#').append(String.format("%012d", Long.parseLong(number)));
        }
        return key.toString();
    }
}
