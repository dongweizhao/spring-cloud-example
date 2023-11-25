#### 目的
Spring Cloud 线上微服务实例都是2个起步，如果出问题后，在没有ELK等日志分析平台，如何确定调用到了目标服务的那个实例，以此来排查问题

![](https://files.mdnice.com/user/35072/a5c40ce9-d1cf-4d74-ad6b-c308f477d5a5.png)


#### 技术栈
- Spring Cloud: Hoxton.SR6
- Spring Boot: 2.3.1.RELEASE
- Spring-Cloud-Openfeign: 2.2.3.RELEASE(spring cloud依赖内置，不用指定版本)
#### 效果
可以看到服务有几个实例是上线，并且最终调用了那个实例

![](https://files.mdnice.com/user/35072/f88a239c-366b-4ab0-9bc2-650366e3d1db.png)

### 实现方案
#### 1. 继承RoundRobinRule，并重写`choose`方法
``` java

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.RoundRobinRule;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * 因为调用目标机器的时候，如果目标机器本身假死或者调用目标不通无法数据返回，那么feign无法打印目标机器。这种场景下我们需要在调用失败（目标机器没有返回）的时候也能把目标机器的ip打印出来，这种场景需要我们切入feign选择机器的逻辑，注入我们自己的调度策略（默认是roundrobin），在里面打印选择的机器即可。
 * 添加一个feignRuleConfigurations目录，定义FeignRule和FeignRuleConfig。
 * ref:https://github.com/spring-cloud/spring-cloud-netflix/issues/2686
 * https://cloud.spring.io/spring-cloud-static/Edgware.SR1/single/spring-cloud.html#_customizing_default_for_all_ribbon_clients
 *
 * @RibbonClients(defaultConfiguration = FeignRule.class)
 * /**
 * @ RibbonClient
 * * Declarative configuration for a ribbon client. Add this annotation to any
 * * <code>@Configuration</code> and then inject a {@link org.springframework.cloud.netflix.ribbon.SpringClientFactory} to access the
 * * client that is created.
 * *
 * * @author Dave Syer
 * @ RibbonClients
 * * Convenience annotation that allows user to combine multiple <code>@RibbonClient</code>
 * * annotations on a single class (including in Java 7).
 */

@Slf4j
public class FeignRule extends RoundRobinRule {

    @Override
    public Server choose(Object key) {
        Server server = super.choose(key);
        if (Objects.isNull(server)) {
            log.info("server is null");
            return null;
        }
        log.info("feign rule ---> serverName:{}, choose key:{}, final server ip:{}", server.getMetaInfo().getAppName(), key, server.getHostPort());
        return server;
    }

    @Override
    public Server choose(ILoadBalancer lb, Object key) {
        Server chooseServer = super.choose(lb, key);

        List<Server> reachableServers = lb.getReachableServers();
        List<Server> allServers = lb.getAllServers();
        int upCount = reachableServers.size();
        int serverCount = allServers.size();
        log.info("serverName:{} upCount:{}, serverCount:{}", Objects.nonNull(chooseServer) ? chooseServer.getMetaInfo().getAppName() : "", upCount, serverCount);
        for (Server server : allServers) {
            if (server instanceof DiscoveryEnabledServer) {
                DiscoveryEnabledServer dServer = (DiscoveryEnabledServer) server;
                InstanceInfo instanceInfo = dServer.getInstanceInfo();
                if (instanceInfo != null) {
                    InstanceInfo.InstanceStatus status = instanceInfo.getStatus();
                    if (status != null) {
                        log.info("serverName:{} server:{}, status:{}", server.getMetaInfo().getAppName(), server.getHostPort(), status);
                    }
                }
            }
        }

        return chooseServer;
    }
}
```
#### 2.修改RibbonClients配置
``` java
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Configuration;
/**
 * @description:feign 配置
 */
@Configuration
@RibbonClients(defaultConfiguration = {FeignRule.class})
public class FeignConfig {
}
```

以上两部完成大功告成！

源码下载：
https://github.com/dongweizhao/spring-cloud-example/tree/SR6-OpenFeign

## 欢迎关注我的公众号
有更多内容带给您
![img_1.png](img_1.png)
