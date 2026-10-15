import java.util.*;

public class PM extends User {

    public PM(String username, String password) {
        super(username, password);
    }

    @Override
    public String getRole() {
        return "PM";
    }

    public void monitorBugsByStatus() {
        List<Bug> bugs = BugService.getAllBugs();
        int open = 0, closed = 0;
        for (Bug b : bugs) {
            if ("open".equalsIgnoreCase(b.getStatus())) open++;
            if ("closed".equalsIgnoreCase(b.getStatus()) || "complete".equalsIgnoreCase(b.getStatus())) closed++;
        }
        System.out.println("Open bugs: " + open);
        System.out.println("Closed bugs: " + closed);
    }

    public void showDeveloperPerformance() {
        List<Bug> bugs = BugService.getAllBugs();
        List<User> users = UserService.getAllUsers();

        System.out.println("\n=== DEVELOPER PERFORMANCE REPORT ===");
        Map<String, Integer> assigned = new HashMap<>();
        Map<String, Integer> resolved = new HashMap<>();

        for (Bug b : bugs) {
            if (b.getAssignedTo() != null) {
                assigned.put(b.getAssignedTo(), assigned.getOrDefault(b.getAssignedTo(), 0) + 1);
                if ("Closed".equalsIgnoreCase(b.getStatus()) || "Complete".equalsIgnoreCase(b.getStatus())) {
                    resolved.put(b.getAssignedTo(), resolved.getOrDefault(b.getAssignedTo(), 0) + 1);
                }
            }
        }

        boolean found = false;
        for (User u : users) {
            if ("Developer".equalsIgnoreCase(u.getRole())) {
                found = true;
                int total = assigned.getOrDefault(u.getUsername(), 0);
                int done = resolved.getOrDefault(u.getUsername(), 0);
                double rate = total == 0 ? 0 : (done * 100.0 / total);
                System.out.printf("- %s: Assigned=%d, Resolved=%d, Completion=%.1f%%\n",
                        u.getUsername(), total, done, rate);
            }
        }
        if (!found) System.out.println("No developers found.");
    }

    public void showTesterPerformance() {
        List<Bug> bugs = BugService.getAllBugs();
        List<User> users = UserService.getAllUsers();

        System.out.println("\n=== TESTER PERFORMANCE REPORT ===");
        Map<String, Integer> reported = new HashMap<>();
        Map<String, Integer> verified = new HashMap<>();

        for (Bug b : bugs) {
            if (b.getReportedBy() != null) {
                reported.put(b.getReportedBy(), reported.getOrDefault(b.getReportedBy(), 0) + 1);
                if ("Closed".equalsIgnoreCase(b.getStatus()) || "Complete".equalsIgnoreCase(b.getStatus())) {
                    verified.put(b.getReportedBy(), verified.getOrDefault(b.getReportedBy(), 0) + 1);
                }
            }
        }

        boolean found = false;
        for (User u : users) {
            if ("Tester".equalsIgnoreCase(u.getRole())) {
                found = true;
                int total = reported.getOrDefault(u.getUsername(), 0);
                int closed = verified.getOrDefault(u.getUsername(), 0);
                double rate = total == 0 ? 0 : (closed * 100.0 / total);
                System.out.printf("- %s: Reported=%d, Closed=%d, Closure=%.1f%%\n",
                        u.getUsername(), total, closed, rate);
            }
        }
        if (!found) System.out.println("No testers found.");
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        while (choice != 4) {
            System.out.println("\n=== PROJECT MANAGER MENU ===");
            System.out.println("1. Check Developer Performance");
            System.out.println("2. Check Tester Performance");
            System.out.println("3. Monitor Open and Closed Bugs");
            System.out.println("4. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Please enter a number.");
                continue;
            }

            if (choice == 1) {
                showDeveloperPerformance();
            } else if (choice == 2) {
                showTesterPerformance();
            } else if (choice == 3) {
                monitorBugsByStatus();
            } else if (choice == 4) {
                System.out.println("Exiting...");
            } else {
                System.out.println("Invalid choice. Please enter 1-4.");
            }
        }
        scanner.close();
    }
}