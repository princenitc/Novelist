package com.prince.novelist.repository;


import com.prince.novelist.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends Neo4jRepository<User, String> {

	@Query("MATCH(u:User {userId:$userId} ) OPTIONAL MATCH (u)<- [r:RATED]-(b:Book) RETURN u,r,b")
	Optional<User> findUserById(String userId);

	@Query("MATCH(u:User) OPTIONAL MATCH (u)<- [r:RATED]-(b:Book) RETURN u,r,b")
	Collection<User> getAllUsers();

	@Query("CREATE (user : User {name : $name, age: $age, userId: $userId}) RETURN user")
	User createNewUser(String name, Long age, String userId);

	@Query("MATCH (u:User {userId: $id}) SET u.name=$name, u.age=$age RETURN u")
	User updateUserDetails(String id, String name, Long age);

	@Query("MATCH (user:User {userId: $userId}) MATCH (book : Book {bookId:$bookId}) CREATE (user)<-[r :RATED {rating : $rating}]-(book) RETURN user,r,book")
	Optional<User> addReview(String userId, String bookId, String rating);

	@Query("MATCH (u:User{userId:$id}) DELETE u")
	void deleteUserById(String id);

}
