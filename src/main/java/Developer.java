import java.util.List;
import java.util.Scanner;

public class Developer extends User {

    public Developer(String username, String password) {
        super(username, password);
    }

    @Override
    public String getRole() {
        return "Developer";
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        while (choice != 3) {
            System.out.println("\n=== DEVELOPER MENU ===");
            System.out.println("1. View Assigned Bugs");
            System.out.println("2. Close Bug");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
                continue;
            }

            if (choice == 1) {
                viewAssignedBugs();
            } else if (choice == 2) {
                updateBugStatus(scanner);
            } else if (choice == 3) {
                System.out.println("Exiting...");
            } else {
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
        scanner.close();
    }

    public void viewAssignedBugs() {
        List<Bug> bugs = BugService.getAllBugs();
        boolean found = false;

        System.out.println("\n=== YOUR ASSIGNED BUGS ===");
        for (Bug bug : bugs) {
            if (username.equalsIgnoreCase(bug.getAssignedTo())) {
                System.out.println("ID:     " + bug.getId());
                System.out.println("Name:   " + bug.getBugName());
                System.out.println("Status: " + bug.getStatus());
                System.out.println("-------------------------");
                found = true;
            }
        }

        if (!found) {
            System.out.println("No bugs assigned to you.");
        }
    }

    public void updateBugStatus(Scanner scanner) {
        System.out.print("\nEnter Bug ID: ");
        int bugId;

        try {
            bugId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid Bug ID.");
            return;
        }

        Bug targetBug = null;
        for (Bug bug : BugService.getAllBugs()) {
            if (bug.getId() == bugId) {
                targetBug = bug;
                break;
            }
        }

        if (targetBug == null) {
            System.out.println("Bug with ID " + bugId + " not found.");
            return;
        }

        if (!username.equalsIgnoreCase(targetBug.getAssignedTo())) {
            System.out.println("Bug not assigned to you.");
            return;
        }

        System.out.println("Enter New Status:");
        System.out.println("1. In Progress");
        System.out.println("2. Closed");
        System.out.print("Enter choice: ");

        int statusChoice;
        try {
            statusChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        if (statusChoice == 1) {
            targetBug.setStatus("In Progress");
            BugService.saveBugsToFile();
            System.out.println("Bug status updated to: In Progress");
        } else if (statusChoice == 2) {
            targetBug.setStatus("Closed");
            BugService.saveBugsToFile();
            System.out.println("Bug status updated to: Closed");
            sendEmailToTester(targetBug);
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private void sendEmailToTester(Bug bug) {
        String testerEmail = UserService.findEmailByUsername(bug.getReportedBy());
        if (testerEmail == null || testerEmail.trim().isEmpty()) {
            testerEmail = bug.getReportedBy().trim().toLowerCase().replaceAll("\\s+", ".") + "@gmail.com";
        }
        String subject = "Bug Closed Notification";
        String message = "Bug ID: " + bug.getId() + " has been marked as CLOSED by developer " + username + ".";

        try {
            EmailService.sendEmail(testerEmail, subject, message);
            System.out.println("Email sent to tester.");
        } catch (RuntimeException e) {
            System.out.println("Failed to send email: " + e.getMessage());
        }
    }
}