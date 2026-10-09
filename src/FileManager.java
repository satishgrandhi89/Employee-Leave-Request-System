import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class FileManager {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path EMPLOYEES_FILE = DATA_DIR.resolve("employees.txt");
    private static final Path REQUESTS_FILE = DATA_DIR.resolve("requests.txt");
    private static final Path REPORT_FILE = DATA_DIR.resolve("report.txt");

    public void ensureDataFolder() throws IOException {
        Files.createDirectories(DATA_DIR);
    }

    
    public ArrayList<Employee> loadEmployees() throws IOException {
        ArrayList<Employee> list = new ArrayList<>();
        if (!Files.exists(EMPLOYEES_FILE)) {
            return list;                                  
        }
        HashSet<Integer> seenIds = new HashSet<>();
        String name = EMPLOYEES_FILE.getFileName().toString();
        try (BufferedReader reader = Files.newBufferedReader(EMPLOYEES_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split("\\|", -1);
                if (f.length != 3) {
                    throw bad(name, lineNo, "expected 3 fields but found " + f.length);
                }
                try {
                    int id = parseId(f[0]);
                    if (!seenIds.add(id)) {
                        throw new IllegalArgumentException("duplicate ID " + id);
                    }
                    list.add(new Employee(id, f[1], f[2]));
                } catch (IllegalArgumentException e) {
                    throw bad(name, lineNo, e.getMessage());
                }
            }
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(name)) {
                throw e;
            }
            throw new IOException("Cannot read " + name + ": " + e, e);
        }
        return list;
    }

    
    public ArrayList<LeaveRequest> loadRequests(List<Employee> employees) throws IOException {
        ArrayList<LeaveRequest> list = new ArrayList<>();
        if (!Files.exists(REQUESTS_FILE)) {
            return list;
        }
        HashSet<Integer> employeeIds = new HashSet<>();
        for (Employee e : employees) {
            employeeIds.add(e.getId());
        }
        HashSet<Integer> seenIds = new HashSet<>();
        String name = REQUESTS_FILE.getFileName().toString();
        try (BufferedReader reader = Files.newBufferedReader(REQUESTS_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split("\\|", -1);
                if (f.length != 7) {
                    throw bad(name, lineNo, "expected 7 fields but found " + f.length);
                }
                try {
                    int id = parseId(f[0]);
                    int employeeId = parseId(f[1]);
                    if (!seenIds.add(id)) {
                        throw new IllegalArgumentException("duplicate request ID " + id);
                    }
                    if (!employeeIds.contains(employeeId)) {
                        throw new IllegalArgumentException("unknown employee ID " + employeeId);
                    }
                    LocalDate start = LocalDate.parse(f[2]);
                    LocalDate end = LocalDate.parse(f[3]);
                    LeaveStatus status = LeaveStatus.valueOf(f[6]);
                    list.add(new LeaveRequest(id, employeeId, start, end, f[4], f[5], status));
                } catch (DateTimeParseException e) {
                    throw bad(name, lineNo, "invalid date (use yyyy-MM-dd)");
                } catch (IllegalArgumentException e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.startsWith("No enum constant")) {
                        msg = "invalid status";
                    }
                    throw bad(name, lineNo, msg);
                }
            }
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(name)) {
                throw e;
            }
            throw new IOException("Cannot read " + name + ": " + e, e);
        }
        checkApprovedOverlaps(list, name);
        return list;
    }

   
    private void checkApprovedOverlaps(List<LeaveRequest> list, String fileName) throws IOException {
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                LeaveRequest a = list.get(i);
                LeaveRequest b = list.get(j);
                if (a.getStatus() == LeaveStatus.APPROVED && b.getStatus() == LeaveStatus.APPROVED
                        && a.getEmployeeId() == b.getEmployeeId() && a.overlaps(b)) {
                    throw new IOException(fileName + ": approved requests " + a.getId()
                            + " and " + b.getId() + " overlap for employee " + a.getEmployeeId());
                }
            }
        }
    }

    private int parseId(String text) {
        try {
            int id = Integer.parseInt(text.trim());
            if (id < 1) {
                throw new IllegalArgumentException("ID must be positive");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID '" + text + "' is not a number");
        }
    }

    private IOException bad(String file, int lineNo, String message) {
        return new IOException(file + " line " + lineNo + ": " + message);
    }

    // ----------------------------------------------------------------- saving

    public void saveEmployees(List<Employee> employees) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Employee e : employees) {
            lines.add(e.getId() + "|" + e.getName() + "|" + e.getDepartment());
        }
        writeLines(EMPLOYEES_FILE, lines);
    }

    public void saveRequests(List<LeaveRequest> requests) throws IOException {
        List<String> lines = new ArrayList<>();
        for (LeaveRequest r : requests) {
            lines.add(r.getId() + "|" + r.getEmployeeId() + "|" + r.getStart() + "|" + r.getEnd()
                    + "|" + r.getType() + "|" + r.getReason() + "|" + r.getStatus());
        }
        writeLines(REQUESTS_FILE, lines);
    }

    public void saveReport(List<String> reportLines) throws IOException {
        writeLines(REPORT_FILE, reportLines);
    }

    
    private void writeLines(Path target, List<String> lines) throws IOException {
        Files.createDirectories(DATA_DIR);
        Path temp = DATA_DIR.resolve(target.getFileName().toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }
}