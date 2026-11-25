package fixmemaybe.service;

import fixmemaybe.data.DataStore;
import fixmemaybe.exception.ApiException;
import fixmemaybe.exception.InvalidInputException;
import fixmemaybe.model.Facility;
import fixmemaybe.model.RepairRequest;
import fixmemaybe.model.Status;
import fixmemaybe.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Manages the list of rooms / places and computes their condition. */
public class FacilityService {

    private final DataStore store;

    public FacilityService(DataStore store) {
        this.store = store;
    }

    /** All facilities with their condition updated from current requests. */
    public List<Facility> list() {
        List<Facility> list = store.getAllFacilities();
        List<RepairRequest> requests = store.getAllRequests();
        for (Facility f : list) {
            int open = 0, inProgress = 0;
            for (RepairRequest r : requests) {
                if (!r.getLocation().equalsIgnoreCase(f.getName()) || !r.isOpen()) continue;
                open++;
                if (r.getStatus() == Status.IN_PROGRESS) inProgress++;
            }
            f.updateCondition(open, inProgress);
        }
        list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return list;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (Facility f : list()) names.add(f.getName());
        return names;
    }

    public Facility create(Map<String, Object> body) {
        String name = Json.str(body, "name");
        if (store.findFacilityByName(name) != null) throw new InvalidInputException("A facility with that name already exists.");
        return store.addFacility(name, Json.str(body, "location"), Json.str(body, "type"));
    }

    public Facility update(int id, Map<String, Object> body) {
        Facility f = store.findFacility(id);
        if (f == null) throw ApiException.notFound("Facility");
        String name = Json.str(body, "name");
        Facility same = store.findFacilityByName(name);
        if (same != null && same != f) throw new InvalidInputException("A facility with that name already exists.");
        synchronized (store) {
            f.setName(name);
            f.setLocation(Json.str(body, "location"));
            f.setType(Json.str(body, "type"));
            store.saveFacilities();
        }
        return f;
    }

    public void delete(int id) {
        Facility f = store.findFacility(id);
        if (f == null) throw ApiException.notFound("Facility");
        store.removeFacility(f);
    }
}
