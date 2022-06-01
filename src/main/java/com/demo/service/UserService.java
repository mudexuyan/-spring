package com.demo.service;

import com.spring.*;

@Component("userService")
//@Scope("prototype")
public class UserService implements InitializingBean, BeanNameAware, UserInterface {

    @Autowired
    private OrderService orderService;

    private String beanName;

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public void test() {
        System.out.println("userService逻辑");
        System.out.println(orderService);
        System.out.println(beanName);
        System.out.println(name);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(beanName + "初始化");
    }

    @Override
    public void setBeanName(String name) {
        beanName = name;
        System.out.println(beanName + "aware回调");

    }
}
