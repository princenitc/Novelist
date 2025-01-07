package com.prince.novelist;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;

import javax.sql.DataSource;

@TestConfiguration
public class TestSetup {

	@Container
	private static final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:latest")
			                                                        .withEnv("NEO4J_AUTH", "neo4j/password123")
			                                                        .withEnv("NEO4J_dbms.active_database", "mydatabase");

	@Bean
	public Driver driver() {
		neo4jContainer.start();
		return GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.basic("neo4j", "password123"));
	}

	@Bean
	public Neo4jTransactionManager neo4jTransactionManager() {
		return new Neo4jTransactionManager(driver());
	}
}
