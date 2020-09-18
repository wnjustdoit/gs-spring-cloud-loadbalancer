package hello;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.Resource;

/**
 * @author Olga Maciaszek-Sharma
 */
@Configuration
public class SayHelloConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SayHelloConfiguration.class);

    @Resource
    private DynamicListOfServer dynamicListOfServer;

//    @Bean
//    @Primary
//    ServiceInstanceListSupplier serviceInstanceListSupplier() {
//        return new DemoServiceInstanceListSuppler("say-hello");
//    }


    class DemoServiceInstanceListSuppler implements ServiceInstanceListSupplier {

        private final String serviceId;

        DemoServiceInstanceListSuppler(String serviceId) {
            this.serviceId = serviceId;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public Flux<List<ServiceInstance>> get() {
            List<ServiceInstance> serviceInstances = dynamicListOfServer.getAvailableSocketAddresses().parallelStream()
                    .map(socketAddress -> new DefaultServiceInstance(serviceId + "1", serviceId, socketAddress.getHostName(), socketAddress.getPort(), false))
                    .collect(Collectors.toList());
            return Flux.just(serviceInstances);
        }
    }


}
