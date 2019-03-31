package com.smartpos.controller;

import com.mymvcframework.annotation.*;
import com.smartpos.service.LoginService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@CLController
@CLRequestMapping ("/login")
public class LoginController {
    @CLAutowired
    private LoginService loginService;

    @CLResponseBody
    @CLRequestMapping("/in")
    public String in(String name) {
        System.out.println(name);
        return loginService.get(name);

    }
    @CLResponseBody
    @CLRequestMapping("/in2")
    public String in2(HttpServletRequest request, HttpServletResponse response,
                      @CLRequestParam String name) {
        System.out.println(name);
        return " this is in2 :name="+name;

    }

    @CLRequestMapping("/in3")
    public void in3(HttpServletRequest request, HttpServletResponse response,
                      @CLRequestParam String name) {
        System.out.println(name);

        try {
            response.getWriter().write("this is in3. name is "+name);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
