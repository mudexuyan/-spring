package com.demo.service;

import com.spring.Autowired;
import com.spring.Component;
import com.spring.Scope;

@Component("orderService")
//@Scope("prototype")
public class OrderService {

    @Autowired
    private UserService userService;
}
