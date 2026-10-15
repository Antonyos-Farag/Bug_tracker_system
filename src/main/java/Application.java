import java.util.Scanner;

public class Application {

    private AuthService authService;

    public Application() {
        authService = new AuthService();
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Press Enter to Start...");
        scanner.nextLine();

        User user = null;

        while (user == null) {
            System.out.print("Enter Username: ");
            String username = scanner.nextLine();

            System.out.print("Enter Password: ");
            String password = scanner.nextLine();

            user = authService.login(username, password);

            if (user == null) {
                System.out.println("Username or Password are Incorrect. Please try again!!\n");
            }
        }

        System.out.println("Welcome " + user.getRole() + "!");

        if (user instanceof Tester) {
            ((Tester) user).showMenu();
        } else if (user instanceof Developer) {
            ((Developer) user).showMenu();
        } else if (user instanceof PM) {
            ((PM) user).showMenu();
        } else if (user instanceof Admin) {
            ((Admin) user).showMenu();
        }

        scanner.close();
    }
}
