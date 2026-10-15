public class AuthService {

    public User login(String username, String password) {
        for (User user : UserService.getAllUsers()) {
            if (user.getUsername().equalsIgnoreCase(username) && user.getPassword().equals(password)) {
                return user;
            }
        }

        return null;
    }
}