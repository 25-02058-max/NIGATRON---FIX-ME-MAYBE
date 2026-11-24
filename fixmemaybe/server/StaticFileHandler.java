package fixmemaybe.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves files from a folder (the  web/  folder for pages,
 * and data/uploads for photos).
 */
public class StaticFileHandler implements HttpHandler {

    private static final Map<String, String> MIME = new HashMap<>();
    static {
        MIME.put("html", "text/html; charset=utf-8");
        MIME.put("css", "text/css; charset=utf-8");
        MIME.put("js", "application/javascript; charset=utf-8");
        MIME.put("json", "application/json; charset=utf-8");
        MIME.put("svg", "image/svg+xml");
        MIME.put("png", "image/png");
        MIME.put("jpg", "image/jpeg");
        MIME.put("jpeg", "image/jpeg");
        MIME.put("gif", "image/gif");
        MIME.put("webp", "image/webp");
        MIME.put("ico", "image/x-icon");
        MIME.put("woff2", "font/woff2");
        MIME.put("woff", "font/woff");
        MIME.put("ttf", "font/ttf");
        MIME.put("mp4", "video/mp4");
        MIME.put("webmanifest", "application/manifest+json");
    }

    private final Path root;
    private final String urlPrefix;

    public StaticFileHandler(Path root, String urlPrefix) {
        this.root = root.toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equals(ex.getRequestMethod()) && !"HEAD".equals(ex.getRequestMethod())) {
                sendText(ex, 405, "Method not allowed");
                return;
            }
            String path = URLDecoder.decode(ex.getRequestURI().getPath(), StandardCharsets.UTF_8);
            if (path.startsWith(urlPrefix)) path = path.substring(urlPrefix.length());
            if (path.isEmpty() || path.endsWith("/")) path += "index.html";

            Path file = root.resolve(path.replaceFirst("^/+", "")).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {   // blocks ../ tricks
                sendText(ex, 404, "404 - Page not found");
                return;
            }
            String name = file.getFileName().toString();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
            byte[] bytes = Files.readAllBytes(file);
            ex.getResponseHeaders().set("Content-Type", MIME.getOrDefault(ext, "application/octet-stream"));
            ex.getResponseHeaders().set("Cache-Control", "no-cache"); // edits show immediately
            ex.sendResponseHeaders(200, "HEAD".equals(ex.getRequestMethod()) ? -1 : bytes.length);
            if (!"HEAD".equals(ex.getRequestMethod())) {
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(bytes);
                }
            }
        } finally {
            ex.close();
        }
    }

    private void sendText(HttpExchange ex, int status, String text) throws IOException {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }
}
