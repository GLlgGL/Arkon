package com.fms.web;

import com.fms.domain.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserForm {

    @NotBlank(message = "Enter a first name.")
    @Size(max = 100)
    private String firstName;
    @NotBlank(message = "Enter a last name.")
    @Size(max = 100)
    private String lastName;
    @Email(message = "Enter a valid email address.")
    @Size(max = 200)
    private String email;
    @NotBlank(message = "Enter a username.")
    @Pattern(regexp = "[A-Za-z0-9._-]{3,100}", message = "Use 3 or more letters, digits, dots, dashes or underscores.")
    private String username;
    private int role = AppUser.ROLE_USER;
    /** Required for new users; leave empty to keep the current password. */
    private String newPassword;

    public static UserForm of(AppUser u) {
        UserForm f = new UserForm();
        f.firstName = u.getFirstName();
        f.lastName = u.getLastName();
        f.email = u.getEmail();
        f.username = u.getUsername();
        f.role = u.getRole();
        return f;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? null : username.trim(); }
    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
