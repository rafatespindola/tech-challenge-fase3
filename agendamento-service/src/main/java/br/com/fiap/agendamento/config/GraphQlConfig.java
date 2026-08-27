package br.com.fiap.agendamento.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlConfig {

    /**
     * O scalar DateTime declarado no schema nao existe no GraphQL padrao:
     * precisa ser ligado a implementacao de graphql-java-extended-scalars,
     * que serializa/desserializa OffsetDateTime em ISO-8601.
     */
    @Bean
    public RuntimeWiringConfigurer scalarsConfigurer() {
        return wiring -> wiring.scalar(ExtendedScalars.DateTime);
    }
}
