
public class Manager extends User {

    public Manager() {
        super(0, "Manager");
    }

    @Override
    public void showProfile() {
        System.out.println("Role      : Manager");
        System.out.println("ID        : " + getId());
        System.out.println("Name      : " + getName());
    }
}