package com.pinganfu.travelmall.common.advice;

import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BsvInterfaceMarketFinder implements ApplicationContextAware {

    private volatile Map<Class<?>, Map<String,Object>> cached = new HashMap<>();

    private ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public BsvInterfaceMarketFinder(){

    }

    public <T> T init(Class<T> targetInterface){
       synchronized (cached){
           if(!cached.containsKey(targetInterface)){
               Collection<T> list =  getImplementors(targetInterface);
               Map<String,Object> objectMap  = new ConcurrentHashMap<>();
               for (T  t : list){
                   BsvInterfaceMarker bsvInterfaceMarker = AopUtils.getTargetClass(t).getAnnotation(BsvInterfaceMarker.class);
                   if(Objects.nonNull(bsvInterfaceMarker)){
                       String markName = bsvInterfaceMarker.value();
                       if(StringUtils.isNotEmpty(markName)){
                           objectMap.putIfAbsent(markName,t);
                       }
                   }
               }
               if(!objectMap.isEmpty()){
                   cached.putIfAbsent(targetInterface, objectMap);
               }
           }
       }
       return null;
    }

    public <T> T find(Class<T> targetInterface, String markName){
        if(!cached.containsKey(targetInterface)){
            init(targetInterface);
        }
        if(cached.containsKey(targetInterface)){
            Map<String,Object> map =  cached.get(targetInterface);
            if(Objects.nonNull(map)){
                return (T) map.get(markName);
            }
        }
        return null;
    }

    // 根据Class类型从spring容器中查询出所有的对象
    public <T> Collection<T> getImplementors(Class<T> targetInterface){
        return this.applicationContext.getBeansOfType(targetInterface).values();
    }
}

