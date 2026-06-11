package com.prince.novelist.repository;

import com.prince.novelist.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends Neo4jRepository<User, String> {

	@Query("MATCH(u:User {userId:$userId}) OPTIONAL MATCH (u)-[r:RATED]->(b:Book) RETURN u,r,b")
	Optional<User> getUserById(String userId);

	@Query("MATCH(u:User) OPTIONAL MATCH (u)-[r:RATED]->(b:Book) RETURN u,r,b")
	Collection<User> getAllUsers();

	@Query("CREATE (user:User {name: $name, age: $age, userId: $userId}) RETURN user")
	Optional<User> createNewUser(String name, Long age, String userId);

	@Query("MATCH (u:User {userId: $id}) SET u.name=$name, u.age=$age RETURN u")
	Optional<User> updateUserDetails(String id, String name, Long age);

	@Query("MATCH (u:User{userId:$id}) DETACH DELETE u")
	void deleteUserById(String id);
}
