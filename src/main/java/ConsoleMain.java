/**
 * ConsoleMain — launcher for the CONSOLE (text-menu) version of the Bug Tracker.
 *
 * The project ships two user interfaces on top of the same core services:
 *
 *   1. JavaFX GUI      -> launched by Main.java        (mvn javafx:run)
 *   2. Console menus   -> launched by this class       (mvn exec:java)
 *
 * The console flow lives in Application.java, which only exposes a start()
 * method and has no main() of its own — this tiny launcher provides the
 * missing entry point:
 *
 *     login loop (AuthService)  ->  role-specific menu (Tester / Developer / PM / Admin)
 *
 * NOTE: The console version's "Attach Screenshot" option (Screenshot.java)
 * opens a Swing JFileChooser dialog, so a graphical desktop environment is
 * still needed for that single feature; everything else is pure text I/O.
 */
public class ConsoleMain {

    public static void main(String[] args) {
        new Application().start();
    }
}
