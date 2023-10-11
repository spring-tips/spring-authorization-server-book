package bootiful.processor;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static bootiful.processor.Constants.RABBITMQ_DESTINATION_NAME;

@Configuration
class AmqpConfiguration {

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
