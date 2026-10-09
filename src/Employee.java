
public class Employee extends User {
    private final String department;

    public Employee(int id, String name, String department) {
        super(id, name);
        if (id < 1) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.department = requireText("Department", department);
    }

    public String getDepartment() {
        return department;
    }

    @Override
    public void showProfile() {
        System.out.println("Role      : Employee");
        System.out.println("ID        : " + getId());
        System.out.println("Name      : " + getName());
        System.out.println("Department: " + department);
    }
}