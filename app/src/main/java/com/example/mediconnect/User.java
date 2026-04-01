package com.example.mediconnect;

public class User {
    public String firstName, lastName, email, phone, dob, sex, imageUrl;

    public User() {
    }

    public User(String firstName, String lastName, String email,
                String phone, String dob, String sex, String imageUrl) {

        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.dob = dob;
        this.sex = sex;
        this.imageUrl = imageUrl;
    }
}