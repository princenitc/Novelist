package com.prince.novelist.resource;

import com.prince.novelist.model.Review;
import com.prince.novelist.model.User;
import com.prince.novelist.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/users")
public class UserResource {

	@Autowired
	UserService userService;

	@PostMapping
	public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
		User createdUser = userService.createUser(user);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
	}

	@GetMapping
	public ResponseEntity<Collection<User>> getAllUsers() {
		Collection<User> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{userId}")
	public ResponseEntity<User> getUserById(@PathVariable("userId") String id) {
		User user = userService.getUserById(id);
		return ResponseEntity.ok(user);
	}

	@PutMapping("/{userId}")
	public ResponseEntity<User> updateUser(
			@Valid @RequestBody User user,
			@PathVariable("userId") String id) {
		User updatedUser = userService.updateUser(user, id);
		return ResponseEntity.ok(updatedUser);
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> deleteUser(@PathVariable("userId") String userId) {
		userService.deleteUser(userId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{userId}/reviews")
	public ResponseEntity<User> addReview(@Valid @RequestBody Review review) {
		User updatedUser = userService.addReview(review);
		return ResponseEntity.status(HttpStatus.CREATED).body(updatedUser);
	}
}
