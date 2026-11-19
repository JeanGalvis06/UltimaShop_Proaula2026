package com.ultimashop.worker;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE="ultima.events";
    public static final String KEY="venta.registrada";
    public static final String COMPROBANTES="ultima.comprobantes";
    public static final String DASHBOARD="ultima.dashboard";
    @Bean public Declarables topologia() {
        TopicExchange eventos=new TopicExchange(EXCHANGE,true,false);
        DirectExchange errores=new DirectExchange("ultima.errores",true,false);
        Queue comprobantes=QueueBuilder.durable(COMPROBANTES).deadLetterExchange("ultima.errores").deadLetterRoutingKey(COMPROBANTES).build();
        Queue dashboard=QueueBuilder.durable(DASHBOARD).deadLetterExchange("ultima.errores").deadLetterRoutingKey(DASHBOARD).build();
        Queue errorComprobantes=QueueBuilder.durable(COMPROBANTES+".errores").build();
        Queue errorDashboard=QueueBuilder.durable(DASHBOARD+".errores").build();
        return new Declarables(eventos,errores,comprobantes,dashboard,errorComprobantes,errorDashboard,
            BindingBuilder.bind(comprobantes).to(eventos).with(KEY),BindingBuilder.bind(dashboard).to(eventos).with(KEY),
            BindingBuilder.bind(errorComprobantes).to(errores).with(COMPROBANTES),BindingBuilder.bind(errorDashboard).to(errores).with(DASHBOARD));
    }
    @Bean public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        var factory=new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory,connectionFactory);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless().maxAttempts(3)
            .backOffOptions(1000,2,4000).recoverer(new RejectAndDontRequeueRecoverer()).build());
        return factory;
    }
}
