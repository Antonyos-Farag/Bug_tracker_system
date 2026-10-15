import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static List<User> listOfUsers = new ArrayList<>();

    static {
        listOfUsers.addAll(FileHandler.loadUsers());

        if (listOfUsers.isEmpty()) {
            listOfUsers.add(new Tester("tester", "123"));
            listOfUsers.add(new Developer("developer", "123"));
            listOfUsers.add(new PM("pm", "123"));
            listOfUsers.add(new Admin("admin", "123"));
        }
    }

    public static void addUser(String username, String password, String role) {
        if (findUserByUsername(username) != null) {
            System.out.println("User '" + username + "' already exists.");
            return;
        }

        User newUser = createUser(username, password, role);
        if (newUser == null) {
            System.out.println("Invalid role. Use: tester / developer / pm / admin");
            return;
        }

        listOfUsers.add(newUser);
        FileHandler.saveUsers(listOfUsers);
    }

    public static void updateUser(String username, String newPassword, String newRole) {
        User existingUser = findUserByUsername(username);
        if (existingUser == null) {
            System.out.println("User '" + username + "' not found.");
            return;
        }

        User updatedUser = createUser(existingUser.getUsername(), newPassword, newRole);
        if (updatedUser == null) {
            System.out.println("Invalid role. Use: tester / developer / pm / admin");
            return;
        }

        int index = listOfUsers.indexOf(existingUser);
        listOfUsers.set(index, updatedUser);
        FileHandler.saveUsers(listOfUsers);
    }

    public static boolean deleteUser(String username) {
        for (int i = 0; i < listOfUsers.size(); i++) {
            if (listOfUsers.get(i).getUsername().equals(username)) {
                listOfUsers.remove(i);
                FileHandler.saveUsers(listOfUsers);
                return true;
            }
        }
        return false;
    }

    public static List<User> getAllUsers() {
        return listOfUsers;
    }

    public static User findUserByUsername(String username) {
        for (User user : listOfUsers) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }

        return null;
    }

    public static String findEmailByUsername(String username) {
        for (User user : listOfUsers) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user.getEmail();
            }
        }
        return null;
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
                return new PM(username, password);
            case "admin":
                return new Admin(username, password);
            default:
                return null;
        }
    }

    public static void saveUsersToFile() {
        FileHandler.saveUsers(listOfUsers);
    }
}