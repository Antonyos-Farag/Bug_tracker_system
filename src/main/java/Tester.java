import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class Tester extends User {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public Tester(String username, String password) {
        super(username, password);
    }

    @Override
    public String getRole() {
        return "Tester";
    }

    public void reportBug(Bug bug) {
        BugService.addBug(bug);
        BugService.saveBugsToFile();
        System.out.println("Bug reported successfully!");
    }

    public void assignBug(Bug bug, String developerName) {
        User developer = UserService.findUserByUsername(developerName);
        if (!(developer instanceof Developer)) {
            System.out.println("Developer '" + developerName + "' not found.");
            return;
        }

        bug.setAssignedTo(developer.getUsername());
        BugService.saveBugsToFile();
        System.out.println("Bug assigned to " + developer.getUsername());
    }



    public void viewOpenBugs() {
        boolean found = false;
        System.out.println("=== OPEN BUGS ===");
        for (Bug b : BugService.getAllBugs()) {
            if (b.getStatus().equalsIgnoreCase("Open")) {
                b.printBug();
                found = true;
            }
        }
        if (!found) System.out.println("No open bugs.");
    }

    public void viewClosedBugs() {
        boolean found = false;
        System.out.println("=== CLOSED BUGS ===");
        for (Bug b : BugService.getAllBugs()) {
            if (b.getStatus().equalsIgnoreCase("Closed") || b.getStatus().equalsIgnoreCase("Complete")) {
                b.printBug();
                found = true;
            }
        }
        if (!found) System.out.println("No closed bugs.");
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        while (choice != 6) {
            System.out.println("\n=== TESTER MENU ===");
            System.out.println("1. Report a Bug");
            System.out.println("2. Assign Bug to Developer");
            System.out.println("3. Attach Screenshot to Bug");
            System.out.println("4. View Open Bugs");
            System.out.println("5. View Closed Bugs");
            System.out.println("6. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Please enter a number.");
                continue;
            }

            if (choice == 1) {
                reportBugFromInput(scanner);
            } else if (choice == 2) {
                assignBugFromInput(scanner);
            } else if (choice == 3) {
                attachScreenshotFromInput(scanner);
            } else if (choice == 4) {
                viewOpenBugs();
            } else if (choice == 5) {
                viewClosedBugs();
            } else if (choice == 6) {
                System.out.println("Exiting...");
            } else {
                System.out.println("Invalid choice. Please enter 1-6.");
            }
        }
        scanner.close();
    }

    private void reportBugFromInput(Scanner scanner) {
        System.out.println("\n=== REPORT A BUG ===");
        System.out.print("Bug Name: ");
        String bugName = scanner.nextLine().trim();
        System.out.print("Bug Type (UI / Backend / Performance): ");
        String bugType = scanner.nextLine().trim();
        System.out.print("Priority (High / Medium / Low): ");
        String priority = scanner.nextLine().trim();
        System.out.print("Bug Level (Critical / Major / Minor): ");
        String bugLevel = scanner.nextLine().trim();
        System.out.print("Project Name: ");
        String projectName = scanner.nextLine().trim();
        System.out.print("Bug Date (yyyy-MM-dd, leave blank for today): ");
        String bugDateInput = scanner.nextLine().trim();
        System.out.print("Status (Open / In Progress / Closed): ");
        String status = scanner.nextLine().trim();
        if (status.isEmpty()) {
            status = "Open";
        }

        Date bugDate = new Date();
        if (!bugDateInput.isEmpty()) {
            try {
                bugDate = DATE_FORMAT.parse(bugDateInput);
            } catch (ParseException e) {
                System.out.println("Invalid date. Using today's date instead.");
            }
        }

        int id = 1;
        for (Bug bug : BugService.getAllBugs()) {
            if (bug.getId() >= id) {
                id = bug.getId() + 1;
            }
        }
        Bug newBug = new Bug(id, bugName, "", bugType, priority, bugLevel,
                projectName, bugDate, status, "", username, "");
        reportBug(newBug);
    }

    private void assignBugFromInput(Scanner scanner) {
        System.out.print("\nEnter Bug ID to assign: ");
        int bugID;
        try {
            bugID = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid Bug ID.");
            return;
        }
        Bug targetBug = null;
        for (Bug b : BugService.getAllBugs()) {
            if (b.getId() == bugID) { targetBug = b; break; }
        }

        if (targetBug == null) {
            System.out.println("Bug with ID " + bugID + " not found.");
            return;
        }

        System.out.print("Enter Developer name/username: ");
        String developerName = scanner.nextLine().trim();
        assignBug(targetBug, developerName);
        if (developerName.equalsIgnoreCase(targetBug.getAssignedTo())) {
            sendEmailToDeveloper(targetBug, developerName);
        }
    }

    private void attachScreenshotFromInput(Scanner scanner) {
        Screenshot screenshot = new Screenshot();
        screenshot.attachScreenshot(scanner);
        BugService.saveBugsToFile();
    }

    private void sendEmailToDeveloper(Bug bug, String developerName) {
        String developerEmail = UserService.findEmailByUsername(developerName);
        if (developerEmail == null || developerEmail.trim().isEmpty()) {
            developerEmail = developerName.trim().toLowerCase().replaceAll("\\s+", ".") + "@gmail.com";
        }
        String subject = "New Bug Assigned to You";
        String message = "Bug ID: " + bug.getId()
                + " | Name: " + bug.getBugName()
                + " | Priority: " + bug.getPriority()
                + " | Level: " + bug.getBugLevel()
                + " | Project: " + bug.getProjectName()
                + " | Reported by: " + bug.getReportedBy();

        try {
            EmailService.sendEmail(developerEmail, subject, message);
            System.out.println("Email sent to developer.");
        } catch (RuntimeException e) {
            System.out.println("Failed to send email: " + e.getMessage());
        }
    }
}