import java.util.List;
import java.util.Scanner;

public class Admin extends User {

    public Admin(String username, String password) {
        super(username, password);
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    public void viewAllBugs() {
        List<Bug> bugs = BugService.getAllBugs();
        if (bugs.isEmpty()) {
            System.out.println("No bugs found.");
            return;
        }
        System.out.println("=== All Bugs ===");
        for (Bug bug : bugs) {
            System.out.println(bug);
        }
    }

    public void addUser(String username, String password, String role) {
        int before = UserService.getAllUsers().size();
        UserService.addUser(username, password, role);
        UserService.saveUsersToFile();
        if (UserService.getAllUsers().size() > before) {
            System.out.println("User added successfully!");
        }
    }

    public void updateUser(String username, String newPassword, String newRole) {
        User existingUser = UserService.findUserByUsername(username);
        if (existingUser == null) {
            System.out.println("User '" + username + "' not found.");
            return;
        }

        UserService.updateUser(username, newPassword, newRole);
        UserService.saveUsersToFile();
        User updatedUser = UserService.findUserByUsername(username);
        if (updatedUser != null && (!existingUser.getPassword().equals(updatedUser.getPassword())
                || !existingUser.getRole().equalsIgnoreCase(updatedUser.getRole()))) {
            System.out.println("User updated successfully!");
        }
    }

    public void deleteUser(String username) {
        boolean deleted = UserService.deleteUser(username);
        UserService.saveUsersToFile();
        if (deleted) {
            System.out.println("User deleted successfully!");
        } else {
            System.out.println("User '" + username + "' not found.");
        }
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        while (choice != 5) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. View All Bugs");
            System.out.println("2. Add User");
            System.out.println("3. Update User");
            System.out.println("4. Delete User");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Please enter a number.");
                continue;
            }

            if (choice == 1) {
                viewAllBugs();
            } else if (choice == 2) {
                System.out.print("Username: ");
                String u = scanner.nextLine().trim();
                System.out.print("Password: ");
                String p = scanner.nextLine().trim();
                System.out.print("Role (tester / developer / pm / admin): ");
                String r = scanner.nextLine().trim();
                addUser(u, p, r);
            } else if (choice == 3) {
                System.out.print("Username to update: ");
                String u = scanner.nextLine().trim();
                System.out.print("New password: ");
                String p = scanner.nextLine().trim();
                System.out.print("New role (tester / developer / pm / admin): ");
                String r = scanner.nextLine().trim();
                updateUser(u, p, r);
            } else if (choice == 4) {
                System.out.print("Username to delete: ");
                String u = scanner.nextLine().trim();
                deleteUser(u);
            } else if (choice == 5) {
                System.out.println("Exiting...");
            } else {
                System.out.println("Invalid choice. Please enter 1-5.");
            }
        }
            scanner.close();
    }
}