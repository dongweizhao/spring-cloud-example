package io.wz.userservice.config;


import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Configuration;

/**
 * @description:feign 配置
 */
@Configuration
@RibbonClients(defaultConfiguration = {FeignRule.class})
public class FeignConfig {
}