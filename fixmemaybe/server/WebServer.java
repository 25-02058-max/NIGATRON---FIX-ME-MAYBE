package fixmemaybe.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import fixmemaybe.data.AppConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Starts two servers that share the same pages and API:
 *   HTTP  (default 8080) -> for the laptop
 *   HTTPS (default 8443) -> for phones (browsers only allow the camera on HTTPS)
 * The HTTPS certificate is created automatically the first time.
 */
public class WebServer {

    private static final String KEYSTORE_PASS = "fixmemaybe";

    private final AppConfig config;
    private final Path webRoot;
    private final Path dataDir;
    private final ApiHandler api;
    private final UploadsHandler uploads;

    public WebServer(AppConfig config, Path webRoot, Path dataDir, ApiHandler api, UploadsHandler uploads) {
        this.config = config;
        this.webRoot = webRoot;
        this.dataDir = dataDir;
        this.api = api;
        this.uploads = uploads;
    }

    public void start() throws Exception {
        int httpPort = config.getInt("server.httpPort", 8080);
        int httpsPort = config.getInt("server.httpsPort", 8443);

        HttpServer http = HttpServer.create(new InetSocketAddress("0.0.0.0", httpPort), 0);
        registerRoutes(http);
        http.setExecutor(Executors.newCachedThreadPool());
        http.start();

        boolean httpsOk = false;
        if (config.getBool("server.enableHttps", true)) {
            try {
                HttpsServer https = HttpsServer.create(new InetSocketAddress("0.0.0.0", httpsPort), 0);
                https.setHttpsConfigurator(new HttpsConfigurator(createSslContext()));
                registerRoutes(https);
                https.setExecutor(Executors.newCachedThreadPool());
                https.start();
                httpsOk = true;
                httpsRunning = true;
            } catch (Exception e) {
                System.out.println("[WARN] HTTPS could not start (" + e.getMessage() + "). Phone camera will use the file picker.");
            }
        }
        printBanner(httpPort, httpsPort, httpsOk);
    }

    private void registerRoutes(HttpServer server) {
        server.createContext("/api/", api);
        server.createContext("/uploads/", uploads);
        server.createContext("/", new StaticFileHandler(webRoot, ""));
    }

    // ------------------------------------------------------------------ HTTPS certificate
    private SSLContext createSslContext() throws Exception {
        Path ks = dataDir.resolve("keystore.p12");
        if (!Files.exists(ks)) generateKeystore(ks);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(ks)) {
            keyStore.load(in, KEYSTORE_PASS.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASS.toCharArray());
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), null, null);
        return ssl;
    }

    /** Uses the JDK's own "keytool" program to make a self-signed certificate. */
    private void generateKeystore(Path ks) throws Exception {
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "keytool.exe" : "keytool";
        File keytool = new File(System.getProperty("java.home"), "bin" + File.separator + exe);
        String tool = keytool.exists() ? keytool.getAbsolutePath() : "keytool";

        StringBuilder san = new StringBuilder("SAN=dns:localhost,ip:127.0.0.1");
        for (String ip : lanAddresses()) san.append(",ip:").append(ip);

        Process p = new ProcessBuilder(tool, "-genkeypair", "-alias", "fixmemaybe",
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "3650",
                "-storetype", "PKCS12", "-keystore", ks.toAbsolutePath().toString(),
                "-storepass", KEYSTORE_PASS, "-keypass", KEYSTORE_PASS,
                "-dname", "CN=Fix Me Maybe Local, O=School Project",
                "-ext", san.toString())
                .redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        if (p.waitFor() != 0 || !Files.exists(ks)) throw new IllegalStateException("keytool failed");
        System.out.println("[INFO] Created HTTPS certificate: " + ks);
    }

    // ------------------------------------------------------------------ helpers
    /** True when HTTPS started successfully (used by the Connect Phone page). */
    public static volatile boolean httpsRunning = false;

    /** Names of fake/virtual adapters that phones can never reach. */
    private static final String[] VIRTUAL_HINTS = {
        "virtualbox", "vmware", "vethernet", "hyper-v", "wsl", "docker", "loopback",
        "bluetooth", "tap-", "tunnel", "vpn", "teredo", "pseudo", "npcap", "zerotier", "tailscale", "hamachi"
    };

    /** Phone-reachable IPv4 addresses, best (Wi-Fi / Ethernet) first. */
    public static List<String> lanAddresses() {
        List<String> good = new ArrayList<>();
        List<String> other = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                String name = (ni.getDisplayName() + " " + ni.getName()).toLowerCase();
                boolean virtual = false;
                for (String hint : VIRTUAL_HINTS) if (name.contains(hint)) virtual = true;
                if (virtual) continue;
                boolean wifiOrLan = name.contains("wi-fi") || name.contains("wifi") || name.contains("wireless")
                        || name.contains("wlan") || name.contains("802.11") || name.contains("ethernet") || name.startsWith("en");
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip.startsWith("192.168.56.")) continue;  // VirtualBox host-only
                    (wifiOrLan ? good : other).add(ip);
                }
            }
        } catch (Exception ignored) { }
        good.addAll(other);
        return good;
    }

    private void printBanner(int httpPort, int httpsPort, boolean httpsOk) {
        String line = "=".repeat(62);
        System.out.println();
        System.out.println(line);
        System.out.println("   " + config.get("app.name", "Fix Me Maybe") + " is running!");
        System.out.println(line);
        System.out.println("   On this laptop:   http://localhost:" + httpPort);
        List<String> ips = lanAddresses();
        if (ips.isEmpty()) {
            System.out.println("   (No Wi-Fi/LAN address found - phones cannot connect yet)");
        }
        for (String ip : ips) {
            if (httpsOk) {
                System.out.println("   On phones:        https://" + ip + ":" + httpsPort + "   <- camera works here");
            } else {
                System.out.println("   On phones:        http://" + ip + ":" + httpPort);
            }
        }
        System.out.println();
        System.out.println("   EASIEST: open  http://localhost:" + httpPort + "/connect.html  on the laptop");
        System.out.println("            and scan the QR code with your phone camera.");
        System.out.println();
        System.out.println("   Phone and laptop must be on the SAME Wi-Fi.");
        System.out.println("   Phone says 'refused' / 'can't be reached'? Run ALLOW-PHONE.bat once.");
        System.out.println("   First time on phone: tap 'Advanced' > 'Proceed' on the warning.");
        System.out.println("   Admin emails: " + config.getAdminEmails());
        System.out.println("   Press Ctrl + C to stop the server.");
        System.out.println(line);
    }
}
