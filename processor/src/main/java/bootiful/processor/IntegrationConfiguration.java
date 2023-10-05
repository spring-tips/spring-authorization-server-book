package bootiful.processor;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import static bootiful.processor.ProcessorApplication.AUTHORIZATION_HEADER_NAME;
import static bootiful.processor.ProcessorApplication.RABBITMQ_DESTINATION_NAME;

@Configuration
class IntegrationConfiguration {


    @Bean
    IntegrationFlow inboundAmqpRequestsIntegrationFlow(
            MessageChannel requests,
            ConnectionFactory connectionFactory) {
        var inboundAmqpAdapter = Amqp
                .inboundAdapter(connectionFactory, RABBITMQ_DESTINATION_NAME);
        return IntegrationFlow
                .from(inboundAmqpAdapter)
                .channel(requests)
                .get();
    }

    @Bean
    IntegrationFlow requestsIntegrationFlow(MessageChannel requests) {
        return IntegrationFlow
                .from(requests)//
                .handle((payload, headers) -> {
                    System.out.println("----------------");
                    headers.forEach((key, value) ->
                            System.out.println(key + '=' + value));
                    return null;
                })//
                .get();
    }

    @Bean
    DirectChannelSpec requests(JwtAuthenticationProvider jwtAuthenticationProvider) {
        return MessageChannels
                .direct()
                .interceptor(
                        new JwtAuthenticationInterceptor(AUTHORIZATION_HEADER_NAME, jwtAuthenticationProvider),
                        new SecurityContextChannelInterceptor(AUTHORIZATION_HEADER_NAME),
                        new AuthorizationChannelInterceptor(AuthenticatedAuthorizationManager.authenticated()));
    }

    @Bean
    Queue queue() {
        return QueueBuilder.durable(RABBITMQ_DESTINATION_NAME).build();
    }

    @Bean
    Exchange exchange() {
        return ExchangeBuilder.directExchange(RABBITMQ_DESTINATION_NAME).build();
    }

    @Bean
    Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with(RABBITMQ_DESTINATION_NAME).noargs();
    }

}
