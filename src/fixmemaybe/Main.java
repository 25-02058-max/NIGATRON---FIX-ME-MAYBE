package fixmemaybe;

import fixmemaybe.data.AppConfig;
import fixmemaybe.data.DataStore;
import fixmemaybe.data.PhotoStorage;
import fixmemaybe.data.SessionManager;
import fixmemaybe.server.ApiHandler;
import fixmemaybe.server.UploadsHandler;
import fixmemaybe.server.WebServer;
import fixmemaybe.service.AuthService;
import fixmemaybe.service.FacilityService;
import fixmemaybe.service.NotificationService;
import fixmemaybe.service.RequestService;
import fixmemaybe.service.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FIX ME MAYBE: School Facility Repair System
 * Program entry point. Run from the project folder (the one that contains
 * config/, src/, web/).
 */
public class Main {

    public static void main(String[] args) {
        Path base = Paths.get("").toAbsolutePath();
        if (!Files.isDirectory(base.resolve("web"))) {
            System.out.println("[ERROR] Cannot find the 'web' folder in " + base);
            System.out.println("        Run the program from the FixMeMaybe project folder.");
            return;
        }
        try {
            // 1. Settings + local storage
            AppConfig config = new AppConfig(base.resolve("config").resolve("app.properties"));
            DataStore store = new DataStore(base.resolve("data"), config);
            store.ensureAdminAccounts();
            SessionManager sessions = new SessionManager(base.resolve("data").resolve("sessions.json"));
            PhotoStorage photos = new PhotoStorage(store.getUploadsDir());

            // 2. Services (the features)
            AuthService auth = new AuthService(store, config);
            NotificationService notifications = new NotificationService(store);
            RequestService requests = new RequestService(store, config, photos, notifications);
            FacilityService facilities = new FacilityService(store);
            UserService users = new UserService(store, config, photos, sessions);

            // 3. Web server
            ApiHandler api = new ApiHandler(config, store, sessions, auth, requests, facilities, notifications, users);
            UploadsHandler uploads = new UploadsHandler(store.getUploadsDir(), sessions, store);
            new WebServer(config, base.resolve("web"), base.resolve("data"), api, uploads).start();
        } catch (java.net.BindException e) {
            System.out.println("[ERROR] The port is already in use. Close the other running copy, "
                    + "or change server.httpPort in config/app.properties.");
        } catch (Exception e) {
            System.out.println("[ERROR] Could not start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
