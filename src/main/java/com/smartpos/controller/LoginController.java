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

 /*   @CLResponseBody
    @CLRequestMapping("/in")
    public String in(String name) {
        System.out.println(name);
        return loginService.get(name);

    }
    @CLResponseBody
    @CLRequestMapping("/in2")
    public String in2(@CLRequestParam("name") String name) {
        System.out.println("进入了 login/in2 方法中  "+name);
        return " this is in2 :name="+name;

    }*/

 // 注意此处 如果使用CLRequestParam  必须得赋值
    @CLRequestMapping("/in3")
    public void in3(HttpServletRequest request, HttpServletResponse response,
                      @CLRequestParam("name") String name) {
        System.out.println("进入了 login/in3 方法中 :"+name);
        try {
            response.getWriter().write("SUCCESS   this is in3. name is "+name);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // v3

    @CLRequestMapping("/quuery.*")
    public void query(HttpServletResponse response,HttpServletRequest request,@CLRequestParam("name") String name){
        String result="My name is "+name;
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

@CLRequestMapping("/add")
    public void add(HttpServletRequest request,HttpServletResponse response,@CLRequestParam("a")Integer a,
                    @CLRequestParam("b")Integer b){
    try {
        response.getWriter().write(a+"+"+b+"="+(a+b));
    } catch (IOException e) {
        e.printStackTrace();
    }
}

@CLRequestMapping("/remove")
public String remove(@CLRequestParam("id")Integer id){
    return ""+id;
}




}
