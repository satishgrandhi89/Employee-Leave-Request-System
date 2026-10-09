import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class LeaveService {
    
    private final ArrayList<Employee> employees;
    private final ArrayList<LeaveRequest> requests;
    private final FileManager fileManager;
    private int nextEmployeeId;
    private int nextRequestId;

    public LeaveService(FileManager fileManager, ArrayList<Employee> employees,
                        ArrayList<LeaveRequest> requests) {
        this.fileManager = fileManager;
        this.employees = employees;
        this.requests = requests;
        this.nextEmployeeId = 1;
        for (Employee e : employees) {
            nextEmployeeId = Math.max(nextEmployeeId, e.getId() + 1);  
        }
        this.nextRequestId = 1;
        for (LeaveRequest r : requests) {
            nextRequestId = Math.max(nextRequestId, r.getId() + 1);
        }
    }

    

    public Employee registerEmployee(String name, String department) throws IOException {
        Employee employee = new Employee(nextEmployeeId, name, department); 
        employees.add(employee);
        try {
            fileManager.saveEmployees(employees);
        } catch (IOException e) {
            employees.remove(employee);                   
            throw e;
        }
        nextEmployeeId++;
        return employee;
    }

    public Employee findEmployee(int id) {
        for (Employee e : employees) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    public List<Employee> getEmployees() {
        return employees;
    }

   

    public LeaveRequest submitRequest(int employeeId, LocalDate start, LocalDate end,
                                      String type, String reason) throws IOException {
        if (findEmployee(employeeId) == null) {
            throw new IllegalArgumentException("Employee " + employeeId + " does not exist.");
        }
        LeaveRequest request = new LeaveRequest(nextRequestId, employeeId, start, end,
                type, reason, LeaveStatus.PENDING);      
        requests.add(request);
        try {
            fileManager.saveRequests(requests);
        } catch (IOException e) {
            requests.remove(request);
            throw e;
        }
        nextRequestId++;
        return request;
    }

    public LeaveRequest findRequest(int id) {
        for (LeaveRequest r : requests) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    public List<LeaveRequest> getRequests() {
        return requests;
    }

    public List<LeaveRequest> getRequestsFor(int employeeId) {
        List<LeaveRequest> result = new ArrayList<>();
        for (LeaveRequest r : requests) {
            if (r.getEmployeeId() == employeeId) {
                result.add(r);
            }
        }
        return result;
    }

    
    public boolean overlapsApproved(LeaveRequest request) {
        for (LeaveRequest other : requests) {
            if (other != request
                    && other.getEmployeeId() == request.getEmployeeId()
                    && other.getStatus() == LeaveStatus.APPROVED
                    && other.overlaps(request)) {
                return true;
            }
        }
        return false;
    }

    public LeaveRequest approve(int requestId) throws IOException {
        LeaveRequest request = mustFind(requestId);
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Request " + requestId + " is already "
                    + request.getStatus() + " and cannot be decided again.");
        }
        if (overlapsApproved(request)) {
            throw new IllegalStateException("Cannot approve: dates overlap an approved request "
                    + "of the same employee.");
        }
        request.approve();
        saveOrUndo(request);
        return request;
    }

    public LeaveRequest reject(int requestId) throws IOException {
        LeaveRequest request = mustFind(requestId);
        request.reject();                                  
        saveOrUndo(request);
        return request;
    }

    private LeaveRequest mustFind(int requestId) {
        LeaveRequest request = findRequest(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request " + requestId + " not found.");
        }
        return request;
    }

    private void saveOrUndo(LeaveRequest request) throws IOException {
        try {
            fileManager.saveRequests(requests);
        } catch (IOException e) {
            request.undoDecision();                        
            throw e;
        }
    }

   
    public List<String> buildReport() {
        int[] counts = new int[3];                         
        int approvedDays = 0;
        HashSet<Integer> approvedEmployees = new HashSet<>();
        for (LeaveRequest request : requests) {
            switch (request.getStatus()) {
                case PENDING -> counts[0]++;
                case APPROVED -> {
                    counts[1]++;
                    approvedDays += request.getDays();
                    approvedEmployees.add(request.getEmployeeId());
                }
                case REJECTED -> counts[2]++;
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("LEAVE REPORT");
        lines.add("Pending requests: " + counts[0]);
        lines.add("Approved requests: " + counts[1]);
        lines.add("Rejected requests: " + counts[2]);
        lines.add("Total approved calendar days: " + approvedDays);
        lines.add("Distinct employees with approved leave: " + approvedEmployees.size());
        return lines;
    }

    public void showReport() {
        for (String line : buildReport()) {
            System.out.println(line);
        }
    }

    public void saveReport() throws IOException {
        fileManager.saveReport(buildReport());
    }
}