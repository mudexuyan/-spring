package com.spring;

import javax.print.DocFlavor;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DemoApplicationContext {
    private Class config;
    //单例池，处理单例bean和原型bean
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionHashMap = new ConcurrentHashMap<>();

    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public DemoApplicationContext(Class config) {
        this.config = config;
        //解析配置类
        //解析componentScan-》扫描路径-》扫描-》生成BeanDefinition-》放入map中
        scan(config);

        //容器启动后，生成所有的单例bean
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionHashMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }

    }

    public Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();

            //属性赋值，依赖注入
            for (Field declaredField : clazz.getDeclaredFields()) {

                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    Object bean = getBean(declaredField.getName());
                    declaredField.setAccessible(true);
                    declaredField.set(instance, bean);
                }
            }

            //aware回调
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            //beanPostProcess前置处理，对bean进行扩展
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            //初始化
            if (instance instanceof InitializingBean) {

                try {
                    ((InitializingBean) instance).afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //beanPostProcess后置处理，对bean进行扩展
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }


            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void scan(Class config) {
        ComponentScan componentScan = (ComponentScan) config.getDeclaredAnnotation(ComponentScan.class);
        //com.demo.service
        String path = componentScan.value();
        //com/demo/service
        path = path.replace(".", "/");

        //扫描
        //找到路径下的类
        //bootstrap----jre/lib
        //ext----jre/ext/lib
        //app----classpath-----target/classes
        //此处的是app加载器，通过相对路径获得资源，target/classes/相对路径
        ClassLoader classLoader = DemoApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource(path);
        File file = new File(resource.getFile());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                //D:\work\workspace\springdemo\target\classes\com\demo\service\UserService.class
                String fileName = file1.getAbsolutePath();
                if (fileName.endsWith(".class")) {
                    //com\demo\service\UserService
                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    //com.demo.service.UserService
                    className = className.replace("\\", ".");

                    try {
                        Class<?> aClass = classLoader.loadClass(className);
                        if (aClass.isAnnotationPresent(Component.class)) {
                            //当前类是一个bean对象

                            if (BeanPostProcessor.class.isAssignableFrom(aClass)) {
                                BeanPostProcessor instance = (BeanPostProcessor) aClass.getDeclaredConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }

                            //判断当前是单例bean还是原型bean
                            //解析类，生成beanDefinition
                            Component componentAnnotation = aClass.getDeclaredAnnotation(Component.class);
                            String beanName = componentAnnotation.value();

                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(aClass);
                            //原型
                            if (aClass.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = aClass.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                //单例
                                beanDefinition.setScope("singleton");
                            }
                            //放入bdMap中
                            beanDefinitionHashMap.put(beanName, beanDefinition);

                        }
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    public Object getBean(String beanName) {
        if (beanDefinitionHashMap.containsKey(beanName)) {
            BeanDefinition beanDefinition = beanDefinitionHashMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")) {
                Object o = singletonObjects.get(beanName);
                return o;
            } else {
                //创建bean
                Object o = createBean(beanName, beanDefinition);
                return o;
            }

        } else {
            //不存在对应的bean
            throw new NullPointerException();
        }
    }
}
