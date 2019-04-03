package com.mymvcframework.v3.servlet;

import com.mymvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class CLDispatcherServlet extends HttpServlet {
    // 保存 application.properties 配置文件中的内容
    private Properties contextConfig = new Properties();
    // 保存扫描到的所有的类名
    private List<String> classNames = new ArrayList<String>();
    // ioc容器，为了简化程序，暂时不考虑ConcurrentHashMap
    private Map<String, Object> ioc = new HashMap<>();

    // 保存url 和methods 对应关系
//    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    // todo :v3 优化使用 list
    /**
     * 思考：为什么不用Map?
     * 你用Map 的话，key, 只能对应 url
     *
     * @Description
     * @author wangchunlan
     * @createTime 2019/4/2 9:26
     */
    private List<Handler> handlerMapping = new ArrayList<Handler>();

    // 重写三个方法

    /**
     * init()
     * doGet()
     * doPost()
     */


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 6. 调用，运行阶段
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exection,Detail : " + Arrays.toString(e.getStackTrace()));

        }
    }

    private Handler getHandler(HttpServletRequest request) {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        String url = request.getRequestURI(); // 绝对路径
        String contextPath = request.getContextPath();// 处理成相对路径
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : this.handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }

    private Object convert(Class<?> type,String value){
        if(Integer.class==type){
            return Integer.valueOf(value);
        }else if(Double.class==type){
            return Double.valueOf(value);
        }
//如果是double 或者其他类型，还需要加if
        // todo :这儿可以使用策略模式了  暂时不实现了
        return value;

    }
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {

        Handler handler = getHandler(req);
        if(null==handler){
            resp.getWriter().write("404 NOT FOUND!!");
            return;
        }

        // 获得方法的形参列表
        Class<?>[] paramTypes=handler.getParamTypes();
        Object[] paramValues=new Object[paramTypes.length];
        Map<String,String[]> params=req.getParameterMap();

        for (Map.Entry<String,String[]> param:params.entrySet()) {
            String value=Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s",",");
            if(!handler.getParamIndexMapping().containsKey(param.getKey())){
                    continue;
            }
            int index=handler.getParamIndexMapping().get(param.getKey());
            paramValues[index]=convert(paramTypes[index],value);
        }




        if(handler.getParamIndexMapping().containsKey(HttpServletRequest.class.getName())){
            int reqIndex=handler.getParamIndexMapping().get(HttpServletRequest.class.getName());
            paramValues[reqIndex]=req;
        }

        if(handler.getParamIndexMapping().containsKey(HttpServletResponse.class.getName())){
            int respIndex=handler.getParamIndexMapping().get(HttpServletResponse.class.getName());
            paramValues[respIndex]=resp;
        }
        Object returnVlaue=handler.getMethod().invoke(handler.getController(),paramValues);
        if(returnVlaue==null|| returnVlaue instanceof  Void){
            return;
        }
        resp.getWriter().write(returnVlaue.toString());
    }

    // 初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        //  1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //  2.扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //  3.初始化扫描到的类，并且将之放入到IOC容器中
        doInstance();
        //  4.完成依赖注入
        doAutowired();
        //  5.初始化HandlerMapping
        initHandlerMapping();
        //   初始化完成，-----> 进入运行阶段
        System.out.println("------------->>>>> CL Spring framework is init. <<<<<--------------- ");

    }

    // 5.初始化url 和Method 的一对一关系
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(CLController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(CLRequestMapping.class)) {
                CLRequestMapping requestMappinge = clazz.getAnnotation(CLRequestMapping.class);
                baseUrl = requestMappinge.value();


            }

            // 默认获取所有的public 方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(CLRequestMapping.class)) {
                    continue;
                }
                CLRequestMapping requestMapping = method.getAnnotation(CLRequestMapping.class);
                String regex = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                // todo : v3 优化 ------
//                handlerMapping.put(url, method);
                Pattern pattern = Pattern.compile(regex);
                // url controller method
                this.handlerMapping.add(new Handler(pattern, entry.getValue(), method));
                System.out.println("Mapper :" + pattern + ",method=" + method);
            }


        }
    }

    // 自动依赖注入
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        // todo ?:Entry
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(CLAutowired.class)) {
                    continue;
                }
                CLAutowired autowired = field.getAnnotation(CLAutowired.class);

                // 如果用户没有自定义beanname 默认就根据类型注入
                // 这个地方省去了对类名首字母小写的情况判断，todo 课后作业
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    // 获得接口的类型，作为key 待会去拿这个key 去ioc 去取值
//                    beanName = field.getType().getName(); // todo :tom
                    //  beanName = toLowerFirstCase(field.getType().getSimpleName());// todo :修改
                    beanName = field.getName(); // todo :me   调试  --->修改tom 老师的错误
                }
                // 如果是public 以外的修饰符，只要加了@Autowired 注解，都要强制赋值
                // 反射中叫暴力访问
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                    System.out.println(entry.getValue() + " is autowired ,object is " + ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

            }


        }
    }

    // 3.将扫描到的类 放入容器中，并且实例化容器
    private void doInstance() {
        // 初始化，为DI做准备
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {

                Class<?> clazz = Class.forName(className);

                // 什么样的类 才需要被初始化呢？
                // 怎么判断？ 添加注解的
                if (clazz.isAnnotationPresent(CLController.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(CLService.class)) {
                    // 1.自定义的beanName
                    CLService service = clazz.getAnnotation(CLService.class);
                    String beanName = service.value();
                    // 2.默认类名首字母小写
                    if ("".equals(beanName.trim())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    // 3.根据类型自动赋值，投机取巧
                    // 接口中的实现类是否已经在ioc 容器中存在
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("the " + i.getName() + " is exists!");

                        }
                        // 把接口的类型直接当成key
                        ioc.put(i.getName(), instance); // todo :  tom
//                        ioc.put(i.getSimpleName(), instance);// todo 1:  修改

                    }

                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 如果类名本事是小写字母，肯定会出现问题
     * 声明：这个地方是我自己用的，，类遵循驼峰命名法，
     *
     * @param simpleName
     * @return
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    // 2. 扫描相关的类
    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }

                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                classNames.add(className);
            }
        }
    }

    // 1.加载配置文件
    private void doLoadConfig(String contextConfigLocation) {
        // 加载appLication.properties  将其保存到内存中
        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            contextConfig.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
