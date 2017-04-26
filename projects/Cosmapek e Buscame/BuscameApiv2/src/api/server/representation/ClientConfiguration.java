package api.server.representation;

import java.io.Serializable;

public class ClientConfiguration implements Serializable {
    /** Default serial version ID. */
    private static final long serialVersionUID = 1L;

    private String id;
    private String timeLapse;
    private String acceptMonitor;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getTimeLapse() {
        return timeLapse;
    }
    public void setTimeLapse(String timeLapse) {
        this.timeLapse = timeLapse;
    }
    public String getAcceptMonitor() {
        return acceptMonitor;
    }
    public void setAcceptMonitor(String acceptMonitor) {
        this.acceptMonitor = acceptMonitor;
    }

}