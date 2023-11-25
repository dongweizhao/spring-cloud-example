package io.wz.userservice.config;

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