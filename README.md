#### 目的
Spring Cloud 线上微服务实例都是2个起步，如果出问题后，在没有ELK等日志分析平台，如何确定调用到了目标服务的那个实例，以此来排查问题

![](https://files.mdnice.com/user/35072/a5c40ce9-d1cf-4d74-ad6b-c308f477d5a5.png)


#### 技术栈
- Spring Cloud: 2021.0.4
- Spring Boot: 2.7.17
- Spring-Cloud-Openfeign: 3.1.4(spring cloud依赖内置，不用指定版本)
#### 效果
可以看到服务有几个实例是上线，并且最终调用了那个实例

![](https://files.mdnice.com/user/35072/f88a239c-366b-4ab0-9bc2-650366e3d1db.png)

### 实现方案
#### 1. 继承ReactorServiceInstanceLoadBalancer，并重写相关方法
``` java
@Slf4j
public class CustomRoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    final AtomicInteger position;
    final String serviceId;
    ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    public CustomRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this(serviceInstanceListSupplierProvider, serviceId, (new Random()).nextInt(1000));
    }

    public CustomRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId, int seedPosition) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.position = new AtomicInteger(seedPosition);
    }

    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = this.serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().map((serviceInstances) -> {
            return this.processInstanceResponse(supplier, serviceInstances);
        });
    }

    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier, List<ServiceInstance> serviceInstances) {
        Response<ServiceInstance> serviceInstanceResponse = this.getInstanceResponse(serviceInstances);
        if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
            ((SelectedInstanceCallback)supplier).selectedServiceInstance((ServiceInstance)serviceInstanceResponse.getServer());
        }

        return serviceInstanceResponse;
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("No servers available for service: " + this.serviceId);
            }

            return new EmptyResponse();
        } else {

            int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
            ServiceInstance instance = instances.get(pos % instances.size());
            log.info("serverName:{} upCount:{}",instance.getServiceId(),instances.size());
            log.info("feign rule ---> serverName:{}, final server ip:{}:{}", instance.getServiceId(), instance.getHost(),instance.getPort());
            return new DefaultResponse(instance);
        }
    }
}

```

#### 2.修改LoadBalancerClients配置
``` java
@Configuration
@LoadBalancerClients(defaultConfiguration = CustomLoadBalancerConfiguration.class)
public class CustomLoadBalancerConfig {
}

@Configuration
class CustomLoadBalancerConfiguration {
    /**
     * 参考默认实现
     *
     * @see org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration#reactorServiceInstanceLoadBalancer
     * @return
     */
    @Bean
    public ReactorLoadBalancer<ServiceInstance> reactorServiceInstanceLoadBalancer(Environment environment, LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new CustomRoundRobinLoadBalancer(loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class), name);
    }
}
```

以上两部完成大功告成！



源码下载：https://github.com/dongweizhao/spring-cloud-example/tree/EurekaOpenFeign

## 欢迎关注我的公众号
有更多内容带给您
![img_1.png](img_1.png)
