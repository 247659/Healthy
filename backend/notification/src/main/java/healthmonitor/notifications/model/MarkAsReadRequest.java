package healthmonitor.notifications.model;

import java.util.List;

public record MarkAsReadRequest(
        List<String> ids
) {
}
