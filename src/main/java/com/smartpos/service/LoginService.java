package com.smartpos.service;

import com.mymvcframework.annotation.CLService;
import com.smartpos.service.impl.ILoginService;

@CLService
public class LoginService implements ILoginService {
    @Override
    public String get(String name) {
        return "姓名 "+name;
    }
}
