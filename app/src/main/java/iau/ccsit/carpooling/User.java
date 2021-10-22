/*
 * Copyright (c) PROG's Team (Mustafa AlSihati Team).
 * This Project is currently an academic project for educational purposes.
 * This Project May be used for benefits for the working team.
 * Fully owned by the application developers.
 */

package iau.ccsit.carpooling;

public class User {
    public String username, email, fname, lname, creation_date, phone;

    public User() { }

    public User(String username, String email, String fname, String lname, String creation_date, String phone) {
        this.username = username;
        this.email = email;
        this.fname = fname;
        this.lname = lname;
        this.creation_date = creation_date;
        this.phone = phone;
    }
}
