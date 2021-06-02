package org.example.config;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.CommonsPool2TargetSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.data.redis.RedisStateMachineContextRepository;
import org.springframework.statemachine.data.redis.RedisStateMachinePersister;
import org.springframework.statemachine.persist.RepositoryStateMachinePersist;

@Configuration
@EnableStateMachine
public class StateMachineConfig {

    @Autowired
    public RedisConnectionFactory connectionFactory;

    @Bean(name = "stateMachineTarget")
    @Scope(scopeName = "prototype")
    StateMachine<States, Events> stateMachineTarget() throws Exception {
        StateMachineBuilder.Builder<States, Events> builder = StateMachineBuilder.builder();
        // General configuration
        builder.configureConfiguration()
                .withConfiguration()
                .autoStartup(true);

        builder.configureStates().withStates()
                .initial(States.SI)
                .end(States.SF)
                .state(States.S0)
                .state(States.S1);

        builder.configureTransitions().withExternal()
                .source(States.SI)
                .target(States.S0)
                .event(Events.NEXT).and().withExternal()
                .source(States.S0)
                .target(States.S1)
                .event(Events.NEXT).and().withExternal()
                .source(States.S1)
                .target(States.SF)
                .event(Events.NEXT);

        return builder.build();
    }

    @Bean
    public StateMachinePersist<States, Events, String> stateMachinePersist() {
        RedisStateMachineContextRepository<States, Events> repository =
                new RedisStateMachineContextRepository<>(connectionFactory);
        return new RepositoryStateMachinePersist<>(repository);
    }

    @Bean
    public RedisStateMachinePersister<States, Events> redisStateMachinePersister(
            StateMachinePersist<States, Events, String> stateMachinePersist) {
        return new RedisStateMachinePersister<>(stateMachinePersist);
    }

    @Bean
    public ProxyFactoryBean stateMachine() {
        ProxyFactoryBean pfb = new ProxyFactoryBean();
        pfb.setTargetSource(poolTargetSource());
        return pfb;
    }

    @Bean
    public CommonsPool2TargetSource poolTargetSource() {
        CommonsPool2TargetSource pool = new CommonsPool2TargetSource();
        pool.setMaxSize(3);
        pool.setTargetBeanName("stateMachineTarget");
        return pool;
    }
}
