import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileHandler {

    private static final String USERS_FILE = "users.txt";
    private static final String BUGS_FILE  = "Bug.txt";

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

    // ───────── USERS ─────────

    public static void saveUsers(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users) {
                String line =
                        user.getUsername() + "," +
                        user.getPassword() + "," +
                        getStoredRole(user);

                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }

    public static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);

        if (!file.exists()) return users;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue;

                String username = parts[0].trim();
                String password = parts[1].trim();
                String role     = parts[2].trim();

                User user = createUser(username, password, role);
                if (user != null) users.add(user);
            }

        } catch (IOException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }

        return users;
    }

    private static User createUser(String username, String password, String role) {
        if (role == null) {
            return null;
        }

        switch (role.trim().toLowerCase()) {
            case "tester":
                return new Tester(username, password);
            case "developer":
                return new Developer(username, password);
            case "pm":
            case "project manager":
                return new PM(username, password);
            case "admin":
                return new Admin(username, password);
            default:
                return null;
        }
    }

    private static String getStoredRole(User user) {
        if (user instanceof Tester) {
            return "tester";
        }
        if (user instanceof Developer) {
            return "developer";
        }
        if (user instanceof PM) {
            return "pm";
        }
        if (user instanceof Admin) {
            return "admin";
        }

        return user.getRole();
    }

    // ───────── BUGS ─────────

    public static void saveBugs(List<Bug> bugs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BUGS_FILE))) {

            for (Bug bug : bugs) {

                String date = (bug.getBugDate() != null)
                        ? sdf.format(bug.getBugDate())
                        : "";

                String line =
                        bug.getId() + "," +
                        bug.getBugName() + "," +
                        bug.getDescription() + "," +
                        bug.getBugType() + "," +
                        bug.getPriority() + "," +
                        bug.getBugLevel() + "," +
                        bug.getProjectName() + "," +
                        date + "," +
                        bug.getStatus() + "," +
                        bug.getAssignedTo() + "," +
                        bug.getReportedBy() + "," +
                        bug.getScreenshotPath();

                writer.write(line);
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println("Error saving bugs: " + e.getMessage());
        }
    }

    public static List<Bug> loadBugs() {
        List<Bug> bugs = new ArrayList<>();
        File file = new File(BUGS_FILE);

        if (!file.exists()) return bugs;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] p = line.split(",", 12);
                if (p.length < 12) continue;

                int id = Integer.parseInt(p[0].trim());

                Date date = null;
                try {
                    if (!p[7].trim().isEmpty()) {
                        date = sdf.parse(p[7].trim());
                    }
                } catch (ParseException ignored) {}

                Bug bug = new Bug(
                    id,
                    p[1].trim(),
                    p[2].trim(),
                    p[3].trim(),
                    p[4].trim(),
                    p[5].trim(),
                    p[6].trim(),
                    date,
                    normalizeStatus(p[8].trim()),
                    p[9].trim(),
                    p[10].trim(),
                    p.length == 12 ? p[11].trim() : ""
                );

                bugs.add(bug);
            }

        } catch (Exception e) {
            System.out.println("Error loading bugs: " + e.getMessage());
        }

        return bugs;
    }

    private static String normalizeStatus(String status) {
        if ("complete".equalsIgnoreCase(status)) {
            return "Closed";
        }

        return status;
    }
}