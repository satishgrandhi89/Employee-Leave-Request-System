import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


public class LeaveRequest {
    public static final String[] LEAVE_TYPES = {"Personal", "Medical", "Other"};
    public static final int MAX_DAYS = 10;

    private final int id;
    private final int employeeId;
    private final LocalDate start;
    private final LocalDate end;
    private final String type;
    private final String reason;
    private LeaveStatus status;

    public LeaveRequest(int id, int employeeId, LocalDate start, LocalDate end,
                        String type, String reason, LeaveStatus status) {
        if (id < 1 || employeeId < 1) {
            throw new IllegalArgumentException("IDs must be positive.");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_DAYS) {
            throw new IllegalArgumentException("A request can contain 1 to " + MAX_DAYS + " days.");
        }
        if (!isValidType(type)) {
            throw new IllegalArgumentException("Leave type must be Personal, Medical, or Other.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        this.id = id;
        this.employeeId = employeeId;
        this.start = start;
        this.end = end;
        this.type = type;
        this.reason = User.requireText("Reason", reason);
        this.status = status;
    }

    public static boolean isValidType(String type) {
        for (String t : LEAVE_TYPES) {
            if (t.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /** Inclusive calendar days (both endpoints counted). */
    public int getDays() {
        return (int) (ChronoUnit.DAYS.between(start, end) + 1);
    }

    /** Two ranges overlap unless one ends before the other starts (shared endpoint overlaps). */
    public boolean overlaps(LeaveRequest other) {
        return !(end.isBefore(other.start) || other.end.isBefore(start));
    }

    public void approve() {
        requirePending();
        status = LeaveStatus.APPROVED;
    }

    public void reject() {
        requirePending();
        status = LeaveStatus.REJECTED;
    }

    
    void undoDecision() {
        status = LeaveStatus.PENDING;
    }

    private void requirePending() {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Request " + id + " is already " + status
                    + " and cannot be decided again.");
        }
    }

    public int getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public LocalDate getStart() { return start; }
    public LocalDate getEnd() { return end; }
    public String getType() { return type; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }

    @Override
    public String toString() {
        return id + " | " + start + " to " + end + " | " + getDays() + " days | " + status
                + " | " + type + " | " + reason;
    }
}