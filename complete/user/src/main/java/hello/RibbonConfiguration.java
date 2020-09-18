package hello;

import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.loadbalancer.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author wangnan
 * @since 1.0.0, 2020/9/18
 **/
public class RibbonConfiguration {

    @Resource
    private DynamicListOfServer dynamicListOfServer;

    public class BazServiceList extends ConfigurationBasedServerList {

        public BazServiceList(IClientConfig config) {
            config.set(IClientConfigKey.Keys.ListOfServers, DynamicListOfServer.convertToConnectStr(dynamicListOfServer.getAvailableSocketAddresses(true)));
            super.initWithNiwsConfig(config);
        }

        @Override
        public List<Server> getUpdatedListOfServers() {
            return super.derive(DynamicListOfServer.convertToConnectStr(dynamicListOfServer.getAvailableSocketAddresses(true)));
        }
    }

    @Bean
    public IPing ribbonPing() {
        return new PingUrl();
    }

    @Bean
    public IRule ribbonRule() {
        return new RoundRobinRule();
    }

    @Bean
    public ServerList<Server> ribbonServerList(IClientConfig config) {
        return new BazServiceList(config);
    }

    @Bean
    public ServerListSubsetFilter serverListFilter() {
        ServerListSubsetFilter filter = new ServerListSubsetFilter();
        return filter;
    }

}
