public class User {
    protected String username;
    protected String password;
    protected String email;

    public User(String username, String password) {
        this(username, password, username + "@gmail.com");
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getRole() {
        return "User";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}