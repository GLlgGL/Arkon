package com.fms.web;

import com.fms.domain.Party;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Form for merchants, companies and clients. */
public class PartyForm {

    @NotBlank(message = "Enter a name.")
    @Size(max = 200)
    private String name;
    @Size(max = 50)
    private String tin;
    @Size(max = 300)
    private String address;
    @Email(message = "Enter a valid email address.")
    @Size(max = 200)
    private String email;
    @Size(max = 50)
    private String phone;

    public static PartyForm of(Party p) {
        PartyForm f = new PartyForm();
        f.name = p.getName();
        f.tin = p.getTin();
        f.address = p.getAddress();
        f.email = p.getEmail();
        f.phone = p.getPhone();
        return f;
    }

    public void applyTo(Party p) {
        p.setName(name);
        p.setTin(tin);
        p.setAddress(address);
        p.setEmail(email);
        p.setPhone(phone);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getTin() { return tin; }
    public void setTin(String tin) { this.tin = tin; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
