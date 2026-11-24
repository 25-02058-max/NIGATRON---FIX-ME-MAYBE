package fixmemaybe.data;

import fixmemaybe.exception.InvalidInputException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Saves photos sent from the phone (as "data:image/jpeg;base64,....")
 * into data/uploads/ on the laptop.
 */
public class PhotoStorage {

    private static final int MAX_BYTES = 10 * 1024 * 1024; // 10 MB per photo

    private final Path uploadsDir;

    public PhotoStorage(Path uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    /** Returns the web path of the saved photo, e.g. /uploads/request-001-1700000000.jpg */
    public String save(String dataUrl, String namePrefix) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/") || !dataUrl.contains(";base64,")) {
            throw new InvalidInputException("Invalid photo. Please take or choose the picture again.");
        }
        String mime = dataUrl.substring(5, dataUrl.indexOf(';'));
        String ext;
        switch (mime) {
            case "image/jpeg": case "image/jpg": ext = "jpg"; break;
            case "image/png": ext = "png"; break;
            case "image/webp": ext = "webp"; break;
            default: throw new InvalidInputException("Only JPG, PNG or WEBP photos are allowed.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(",") + 1));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Photo data is damaged. Please try again.");
        }
        if (bytes.length > MAX_BYTES) throw new InvalidInputException("Photo is too large (max 10 MB).");
        if (!looksLikeImage(bytes)) throw new InvalidInputException("That file is not a real picture.");

        String fileName = namePrefix.replaceAll("[^A-Za-z0-9_-]", "") + "-" + System.currentTimeMillis() + "." + ext;
        try {
            Files.write(uploadsDir.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save photo: " + e.getMessage(), e);
        }
        return "/uploads/" + fileName;
    }

    /** Deletes a previously saved photo (ignored if missing). */
    public void delete(String webPath) {
        if (webPath == null || !webPath.startsWith("/uploads/")) return;
        try {
            Path file = uploadsDir.resolve(webPath.substring("/uploads/".length())).normalize();
            if (file.startsWith(uploadsDir)) Files.deleteIfExists(file);
        } catch (IOException ignored) { }
    }

    private static boolean looksLikeImage(byte[] b) {
        if (b.length < 12) return false;
        boolean jpg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean webp = b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E';
        return jpg || png || webp;
    }
}
