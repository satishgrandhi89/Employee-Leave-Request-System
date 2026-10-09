import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static LeaveService service;
    private static final Manager manager = new Manager();

    public static void main(String[] args) {
        FileManager fileManager = new FileManager();
        try {
            fileManager.ensureDataFolder();
            ArrayList<Employee> employees = fileManager.loadEmployees();
            ArrayList<LeaveRequest> requests = fileManager.loadRequests(employees);
            service = new LeaveService(fileManager, employees, requests);
        } catch (IOException e) {
            System.out.println("Cannot start: " + e.getMessage());
            System.out.println("Fix the file in the data folder and run the program again.");
            return;
        }

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("MAIN MENU");
            System.out.println("1. Manager menu");
            System.out.println("2. Employee menu");
            System.out.println("0. Exit");
            switch (readChoice(0, 2)) {
                case 1 -> showManagerMenu();
                case 2 -> showEmployeeMenu();
                case 0 -> running = false;
            }
        }
        System.out.println("Goodbye.");
    }

    

    private static void showManagerMenu() {
        
        User user = manager;
        user.showProfile();
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("MANAGER MENU");
            System.out.println("1. Register employee");
            System.out.println("2. View employees");
            System.out.println("3. View leave requests");
            System.out.println("4. Approve request");
            System.out.println("5. Reject request");
            System.out.println("6. View reports");
            System.out.println("0. Back");
            switch (readChoice(0, 6)) {
                case 1 -> registerEmployee();
                case 2 -> viewEmployees();
                case 3 -> viewAllRequests();
                case 4 -> decide(true);
                case 5 -> decide(false);
                case 6 -> viewReports();
                case 0 -> back = true;
            }
        }
    }

    private static void registerEmployee() {
        String name = readText("Employee name: ");
        String department = readText("Department: ");
        try {
            Employee e = service.registerEmployee(name, department);
            System.out.println("Employee saved. ID: " + e.getId());
        } catch (IllegalArgumentException | IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void viewEmployees() {
        List<Employee> employees = service.getEmployees();
        if (employees.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (Employee e : employees) {
            System.out.println(e.getId() + " | " + e.getName() + " | " + e.getDepartment());
        }
    }

    private static void viewAllRequests() {
        List<LeaveRequest> requests = service.getRequests();
        if (requests.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (LeaveRequest r : requests) {
            Employee e = service.findEmployee(r.getEmployeeId());
            String who = (e == null) ? "?" : e.getName();
            System.out.println(r + " | Employee " + r.getEmployeeId() + " (" + who + ")");
        }
    }

    private static void decide(boolean approve) {
        if (service.getRequests().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        viewAllRequests();
        while (true) {
            int id = readInt("Enter request ID (0 to cancel): ");
            if (id == 0) {
                return;
            }
            if (service.findRequest(id) == null) {
                System.out.println("Request ID not found. Try again.");
                continue;
            }
            try {
                LeaveRequest r = approve ? service.approve(id) : service.reject(id);
                System.out.println("Request saved. Status: " + r.getStatus());
            } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
            return;
        }
    }

    private static void viewReports() {
        service.showReport();
        System.out.print("Save report? (y/n): ");
        String answer = scanner.nextLine().trim();
        if (answer.equalsIgnoreCase("y")) {
            try {
                service.saveReport();
                System.out.println("Report saved to data/report.txt");
            } catch (IOException e) {
                System.out.println("Error saving report: " + e.getMessage());
            }
        }
    }

    

    private static void showEmployeeMenu() {
        Employee employee = askEmployee();
        if (employee == null) {
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("EMPLOYEE MENU (" + employee.getName() + ")");
            System.out.println("1. View my profile");
            System.out.println("2. Submit leave request");
            System.out.println("3. View my requests");
            System.out.println("0. Back");
            switch (readChoice(0, 3)) {
                case 1 -> {
                    User user = employee;         
                    user.showProfile();
                }
                case 2 -> submitRequest(employee);
                case 3 -> viewMyRequests(employee);
                case 0 -> back = true;
            }
        }
    }

    private static Employee askEmployee() {
        while (true) {
            int id = readInt("Enter employee ID (0 to go back): ");
            if (id == 0) {
                return null;
            }
            Employee e = service.findEmployee(id);
            if (e != null) {
                return e;
            }
            System.out.println("Employee ID not found. Try again.");
        }
    }

    private static void submitRequest(Employee employee) {
        LocalDate start = readDate("Start (yyyy-MM-dd): ");
        LocalDate end = readDate("End (yyyy-MM-dd): ");
        System.out.println("Leave type:");
        for (int i = 0; i < LeaveRequest.LEAVE_TYPES.length; i++) {
            System.out.println((i + 1) + ". " + LeaveRequest.LEAVE_TYPES[i]);
        }
        String type = LeaveRequest.LEAVE_TYPES[readChoice(1, LeaveRequest.LEAVE_TYPES.length) - 1];
        String reason = readText("Reason: ");
        try {
            LeaveRequest r = service.submitRequest(employee.getId(), start, end, type, reason);
            System.out.println("Request " + r.getId() + " saved. Days: " + r.getDays()
                    + " | Status: " + r.getStatus());
        } catch (IllegalArgumentException | IOException e) {
            System.out.println("Request not saved. " + e.getMessage());
        }
    }

    private static void viewMyRequests(Employee employee) {
        List<LeaveRequest> mine = service.getRequestsFor(employee.getId());
        if (mine.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (LeaveRequest r : mine) {
            System.out.println(r);
        }
    }

    
    private static int readChoice(int min, int max) {
        while (true) {
            int value = readInt("Choice: ");
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Invalid choice. Enter a number from " + min + " to " + max + ".");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return User.requireText("Value", line);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage().replace("Value", "This field"));
            }
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use yyyy-MM-dd, for example 2026-10-12.");
            }
        }
    }
}