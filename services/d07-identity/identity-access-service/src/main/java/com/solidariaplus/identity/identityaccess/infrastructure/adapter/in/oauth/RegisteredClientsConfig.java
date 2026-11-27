package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.oauth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Keeps the registered clients in {@code oauth2_registered_client}. Spring Boot still maps the clients of
 * {@code application.yml}; this replaces its in-memory repository with the JDBC one after saving them, so
 * the configuration stays the single source of the clients.
 */
@Configuration(proxyBeanMethods = false)
class RegisteredClientsConfig {

    @Bean
    static BeanPostProcessor jdbcRegisteredClients(
            ObjectProvider<JdbcOperations> jdbc, ObjectProvider<OAuth2AuthorizationServerProperties> properties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return bean instanceof InMemoryRegisteredClientRepository configured
                        ? stored(configured, jdbc.getObject(), properties.getObject())
                        : bean;
            }
        };
    }

    private static RegisteredClientRepository stored(
            RegisteredClientRepository configured,
            JdbcOperations jdbc,
            OAuth2AuthorizationServerProperties properties) {
        JdbcRegisteredClientRepository clients = new JdbcRegisteredClientRepository(jdbc);
        properties.getClient().values().stream()
                .map(client ->
                        configured.findByClientId(client.getRegistration().getClientId()))
                .forEach(clients::save);
        return clients;
    }
}
