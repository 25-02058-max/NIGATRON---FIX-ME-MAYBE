package fixmemaybe.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fixmemaybe.data.DataStore;
import fixmemaybe.data.SessionManager;

import java.io.IOException;
import java.nio.file.Path;

/** Serves photos from data/uploads, but ONLY to logged-in users. */
public class UploadsHandler implements HttpHandler {

    private final StaticFileHandler files;
    private final SessionManager sessions;
    private final DataStore store;

    public UploadsHandler(Path uploadsDir, SessionManager sessions, DataStore store) {
        this.files = new StaticFileHandler(uploadsDir, "/uploads");
        this.sessions = sessions;
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String userId = sessions.getUserId(HttpUtil.getCookie(ex, SessionManager.COOKIE));
        if (userId == null || store.findById(userId) == null) {
            ex.sendResponseHeaders(401, -1);
            ex.close();
            return;
        }
        files.handle(ex);
    }
}
