import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BugService {

    private static List<Bug> listOfBugs = new ArrayList<>();

    static {
    listOfBugs.addAll(FileHandler.loadBugs());

    if (listOfBugs.isEmpty()) {
        listOfBugs.add(new Bug(1, "Login Crash", "App crashes on login", "UI", "High",
            "Critical", "BugTrack", new Date(), "Open", "developer", "tester", ""));
        listOfBugs.add(new Bug(2, "Stats Missing", "Dashboard stats not loading", "Backend", "Medium",
            "Major", "BugTrack", new Date(), "Closed", "developer", "tester", ""));
        listOfBugs.add(new Bug(3, "Email Bug", "Email notification not sent", "Backend", "Medium",
        "Minor", "BugTrack", new Date(), "Closed", "developer", "tester", ""));
    }
    }

    public static List<Bug> getAllBugs() {
        return listOfBugs;
    }

    public static void addBug(Bug bug) {
        listOfBugs.add(bug);
        FileHandler.saveBugs(listOfBugs);
    }

    public static void saveBugsToFile() {
        FileHandler.saveBugs(listOfBugs);
    }
}