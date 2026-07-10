package com.example.messystem.auth;

import com.example.messystem.master.entity.MesUser;

public class LoginSession {
    public String token;
    public MesUser user;

    public LoginSession() {
    }

    public LoginSession(String token, MesUser user) {
        this.token = token;
        this.user = user;
    }
}
