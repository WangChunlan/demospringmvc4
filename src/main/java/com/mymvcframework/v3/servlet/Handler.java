package com.mymvcframework.v3.servlet;

import com.mymvcframework.annotation.CLRequestParam;



import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/*
*
 * @author wangchunlan
 * @ClassName Handler.java
 * @Description
 * @createTime 2019年04月02日 09:30:00
*/


// 保存 一个url 和一个method 的关系
public class Handler{
    private Pattern pattern; // 正则
    private Method method;
    private Object controller;
    private Class<?> [] paramTypes;

    private Map<String,Integer> paramIndexMapping;// 形参列表，参数的名字作为key,参数顺序，位置作为值

    public Handler(Pattern pattern, Object controller,Method method) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
        paramTypes=method.getParameterTypes();
        paramIndexMapping=new HashMap<String,Integer>();
        putParamIndexMapping(method);

    }

    private void putParamIndexMapping(Method method) {
        // 提取方法中的注解参数
        // 把方法上的注解拿到，得到的是一个二维数组
        // 因为一个参数可以有多个注解，而一个方法又有多个参数
        // todo :注意这儿不要导错包  java.lang.annotation
        Annotation[][] pa=method.getParameterAnnotations();
        for (int i=0;i<pa.length;i++){
            for (Annotation a:pa[i]) {
                if(a instanceof CLRequestParam){
                    String paramName=((CLRequestParam) a).value();
                    if(!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName,i);
                    }
                }

            }
        }
        // 提取方法中的 request 和 response 参数
        Class<?>[] paramsType=method.getParameterTypes();
        for (int i = 0; i < paramsType.length; i++) {
            Class<?> type=paramsType[i];
            if(type==HttpServletRequest.class||type==HttpServletResponse.class){
                paramIndexMapping.put(type.getName(),i);
            }

        }


    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Method getMethod() {
        return method;
    }

    public Object getController() {
        return controller;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }
}
