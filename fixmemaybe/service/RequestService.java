package fixmemaybe.service;

import fixmemaybe.data.AppConfig;
import fixmemaybe.data.DataStore;
import fixmemaybe.data.PhotoStorage;
import fixmemaybe.exception.ApiException;
import fixmemaybe.exception.InvalidInputException;
import fixmemaybe.model.RepairRequest;
import fixmemaybe.model.Status;
import fixmemaybe.model.User;
import fixmemaybe.util.Json;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Submit, view, search/filter, update status, delete and export repair requests. */
public class RequestService {

    private final DataStore store;
    private final AppConfig config;
    private final PhotoStorage photos;
    private final NotificationService notifications;

    public RequestService(DataStore store, AppConfig config, PhotoStorage photos, NotificationService notifications) {
        this.store = store;
        this.config = config;
        this.photos = photos;
        this.notifications = notifications;
    }

    /** Facility types shown in the report form (editable in config). */
    public List<String> getTypes() {
        List<String> types = new ArrayList<>();
        for (String t : config.get("report.types",
                "Chair,Table,Electrical / Light,Electric Fan,Air Conditioner,Door,Window,Computer,Projector / TV,Plumbing,Other").split(",")) {
            if (!t.isBlank()) types.add(t.trim());
        }
        return types;
    }

    public boolean isPhotoRequired() {
        return config.getBool("report.requirePhoto", true);
    }

    // ------------------------------------------------------------ submit
    public RepairRequest submit(User reporter, Map<String, Object> body) {
        String photoData = Json.str(body, "photo");
        if (photoData.isEmpty() && isPhotoRequired()) throw new InvalidInputException("Please take or choose a photo of the problem.");
        String location = Json.str(body, "location");
        if (!location.isEmpty() && store.findFacilityByName(location) == null && !"Other".equalsIgnoreCase(location)) {
            // custom location typed by the user is allowed, just trim it
            if (location.length() > 60) throw new InvalidInputException("Location name is too long.");
        }

        RepairRequest r = store.newRequest();
        r.setProblem(Json.str(body, "problem"));
        r.setDescription(Json.str(body, "description"));
        r.setLocation(location);
        r.setFacilityType(Json.str(body, "facilityType"));
        String date = Json.str(body, "dateReported");
        r.setDateReported(date.isEmpty() ? LocalDate.now().toString() : date);
        r.submitRequest(reporter, DataStore.now());     // validates (throws InvalidInputException)

        if (!photoData.isEmpty()) r.setPhoto(photos.save(photoData, "request-" + String.format("%03d", r.getRequestId())));
        store.addRequest(r);
        notifications.newRequest(r);
        System.out.println("[INFO] New request: " + r.displayRequest());
        return r;
    }

    // ------------------------------------------------------------ view / search
    /** Users only see their own requests; admins see everything. */
    public List<RepairRequest> list(User user, String status, String location, String type, String query, boolean mineOnly) {
        List<RepairRequest> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (RepairRequest r : store.getAllRequests()) {
            if ((!user.isAdmin() || mineOnly) && !user.getId().equals(r.getReporterId())) continue;
            if (notBlank(status) && !"All".equalsIgnoreCase(status) && r.getStatus() != Status.fromLabel(status)) continue;
            if (notBlank(location) && !location.equalsIgnoreCase(r.getLocation())) continue;
            if (notBlank(type) && !type.equalsIgnoreCase(r.getFacilityType())) continue;
            if (!q.isEmpty()) {
                String hay = (r.getCode() + " " + r.getRequestId() + " " + r.getProblem() + " " + r.getDescription() + " "
                        + r.getLocation() + " " + r.getFacilityType() + " " + r.getReporterName() + " " + r.getStatus().getLabel()).toLowerCase();
                if (!hay.contains(q)) continue;
            }
            out.add(r);
        }
        out.sort(Comparator.comparing(RepairRequest::getRequestId).reversed()); // newest first
        return out;
    }

    public RepairRequest get(User user, int id) {
        RepairRequest r = store.findRequest(id);
        if (r == null) throw ApiException.notFound("Request");
        if (!user.isAdmin() && !user.getId().equals(r.getReporterId())) throw ApiException.forbidden();
        return r;
    }

    // ------------------------------------------------------------ update / delete
    public RepairRequest updateStatus(User staff, int id, Map<String, Object> body) {
        if (!staff.isAdmin()) throw ApiException.forbidden();
        RepairRequest r = get(staff, id);
        String note = Json.str(body, "note");
        synchronized (store) {
            r.updateStatus(Status.fromLabel(Json.str(body, "status")), note, staff, DataStore.now());
            store.saveRequests();
        }
        notifications.statusChanged(r, note);
        System.out.println("[INFO] Status update: " + r.displayRequest());
        return r;
    }

    /** Admin can delete any; a user can cancel their own while still "Reported". */
    public void delete(User user, int id) {
        RepairRequest r = get(user, id);
        if (!user.isAdmin() && r.getStatus() != Status.REPORTED) {
            throw new ApiException(400, "You can only cancel a request that is still Reported.");
        }
        photos.delete(r.getPhoto());
        store.removeRequest(r);
    }

    // ------------------------------------------------------------ statistics
    public Map<String, Object> stats(User user) {
        List<RepairRequest> list = list(user, null, null, null, null, false);
        int reported = 0, progress = 0, fixed = 0;
        Map<String, Integer> byType = new LinkedHashMap<>();
        Map<String, Integer> byLocation = new LinkedHashMap<>();
        Map<String, Integer> byMonth = new LinkedHashMap<>();
        DateTimeFormatter mf = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        YearMonth thisMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) byMonth.put(thisMonth.minusMonths(i).toString(), 0);

        for (RepairRequest r : list) {
            switch (r.getStatus()) {
                case REPORTED: reported++; break;
                case IN_PROGRESS: progress++; break;
                default: fixed++;
            }
            byType.merge(r.getFacilityType(), 1, Integer::sum);
            byLocation.merge(r.getLocation(), 1, Integer::sum);
            String ym = r.getDateReported() != null && r.getDateReported().length() >= 7 ? r.getDateReported().substring(0, 7) : "";
            if (byMonth.containsKey(ym)) byMonth.merge(ym, 1, Integer::sum);
        }
        List<Object> months = new ArrayList<>();
        for (Map.Entry<String, Integer> e : byMonth.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", YearMonth.parse(e.getKey()).format(mf));
            m.put("count", e.getValue());
            months.add(m);
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("reported", reported);
        s.put("inProgress", progress);
        s.put("fixed", fixed);
        s.put("total", list.size());
        s.put("byMonth", months);
        s.put("byType", topN(byType, 6));
        s.put("byLocation", topN(byLocation, 6));
        return s;
    }

    private static List<Object> topN(Map<String, Integer> map, int n) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, entries.size()); i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", entries.get(i).getKey());
            m.put("count", entries.get(i).getValue());
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------ CSV export (opens in Excel)
    public String exportCsv() {
        StringBuilder sb = new StringBuilder("﻿"); // BOM so Excel reads UTF-8
        sb.append("ID,Problem,Details,Location,Type,Date Reported,Reported By,Role,Status,Last Updated,Last Note,Photo\n");
        List<RepairRequest> all = store.getAllRequests();
        all.sort(Comparator.comparing(RepairRequest::getRequestId));
        for (RepairRequest r : all) {
            String lastNote = r.getHistory().isEmpty() ? "" : r.getHistory().get(r.getHistory().size() - 1).getNote();
            sb.append(csv(r.getCode())).append(',').append(csv(r.getProblem())).append(',')
              .append(csv(r.getDescription())).append(',').append(csv(r.getLocation())).append(',')
              .append(csv(r.getFacilityType())).append(',').append(csv(r.getDateReported())).append(',')
              .append(csv(r.getReporterName())).append(',').append(csv(r.getReporterRole())).append(',')
              .append(csv(r.getStatus().getLabel())).append(',').append(csv(r.getUpdatedAt())).append(',')
              .append(csv(lastNote)).append(',').append(csv(r.getPhoto())).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"");
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) s = "'" + s; // Excel safety
        return "\"" + s + "\"";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
