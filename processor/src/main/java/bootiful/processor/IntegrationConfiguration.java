package bootiful.processor;

import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

import static bootiful.processor.Constants.AUTHORIZATION_HEADER_NAME;
import static bootiful.processor.Constants.RABBITMQ_DESTINATION_NAME;

@Configuration
class IntegrationConfiguration {


    // <.>
    @Bean
    IntegrationFlow inboundAmqpRequestsIntegrationFlow(
            @Qualifier(Constants.REQUESTS_MESSAGE_CHANNEL) MessageChannel requests,
            ConnectionFactory connectionFactory) {
        var inboundAmqpAdapter = Amqp
                .inboundAdapter(connectionFactory, RABBITMQ_DESTINATION_NAME);
        return IntegrationFlow
                .from(inboundAmqpAdapter)
                .channel(requests)
                .get();
    }

    // <.>
    @Bean
    IntegrationFlow requestsIntegrationFlow(
            @Qualifier(Constants.REQUESTS_MESSAGE_CHANNEL) MessageChannel requests) {

        var log = LoggerFactory.getLogger(getClass());

        return IntegrationFlow
                .from(requests)//
                .handle((payload, headers) -> {
                    log.info("----");
                    headers.forEach((key, value) -> log.info("{}={}", key, value));
                    return null;
                })//
                .get();
    }

    // <.>
    @Bean(Constants.REQUESTS_MESSAGE_CHANNEL)
    DirectChannelSpec requests(JwtAuthenticationProvider jwtAuthenticationProvider) {
        // <.>
        var jwtAuthInterceptor = new JwtAuthenticationInterceptor(
                AUTHORIZATION_HEADER_NAME, jwtAuthenticationProvider);
        // <.>
        var securityContextChannelInterceptor = new SecurityContextChannelInterceptor(
                AUTHORIZATION_HEADER_NAME);
        // <.>
        var authorizationChannelInterceptor = new AuthorizationChannelInterceptor(
                AuthenticatedAuthorizationManager.authenticated());
        return MessageChannels
                .direct()
                .interceptor(
                        jwtAuthInterceptor,
                        securityContextChannelInterceptor,
                        authorizationChannelInterceptor
                );
    }

}
