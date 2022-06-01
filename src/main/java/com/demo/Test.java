package com.demo;

import com.demo.service.UserInterface;
import com.demo.service.UserService;
import com.spring.DemoApplicationContext;

public class Test {
    public static void main(String[] args) {
        DemoApplicationContext applicationContext = new DemoApplicationContext(AppConfig.class);

        UserInterface object = (UserInterface) applicationContext.getBean("userService");
        object.test();


    }
}
