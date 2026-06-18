package com.prince.novelist.resource;

import com.prince.novelist.dto.request.AddRatingRequest;
import com.prince.novelist.dto.request.CreateUserRequest;
import com.prince.novelist.dto.request.UpdateUserRequest;
import com.prince.novelist.dto.response.PageResponse;
import com.prince.novelist.dto.response.RatingResponse;
import com.prince.novelist.dto.response.UserResponse;
import com.prince.novelist.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserResource {

	private final UserService userService;

	public UserResource(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "Create a user", description = "Create a new user resource")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "User created successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid request payload"),
		@ApiResponse(responseCode = "409", description = "User with email already exists")
	})
	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
	}

	@Operation(summary = "Get all users", description = "Retrieve paginated list of all users")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
		@ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
	})
	@GetMapping
	public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
		@PageableDefault(size = 20, sort = "name") Pageable pageable
	) {
		return ResponseEntity.ok(userService.getAllUsers(pageable));
	}

	@Operation(summary = "Get user by id", description = "Retrieve a single user by identifier")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "User retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "User not found")
	})
	@GetMapping("/{userId}")
	public ResponseEntity<UserResponse> getUserById(@PathVariable("userId") String userId) {
		return ResponseEntity.ok(userService.getUserById(userId));
	}

	@Operation(summary = "Update a user", description = "Update an existing user by identifier")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "User updated successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid request payload"),
		@ApiResponse(responseCode = "404", description = "User not found"),
		@ApiResponse(responseCode = "409", description = "User with email already exists")
	})
	@PutMapping("/{userId}")
	public ResponseEntity<UserResponse> updateUser(
			@PathVariable("userId") String userId,
			@Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(userService.updateUser(userId, request));
	}

	@Operation(summary = "Delete a user", description = "Delete a user by identifier")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "User deleted successfully"),
		@ApiResponse(responseCode = "404", description = "User not found")
	})
	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> deleteUser(@PathVariable("userId") String userId) {
		userService.deleteUser(userId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Add a rating", description = "Add a rating and optional review for a book by a user")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Rating created successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid request payload"),
		@ApiResponse(responseCode = "404", description = "User or book not found")
	})
	@PostMapping("/{userId}/ratings/{bookId}")
	public ResponseEntity<RatingResponse> addRating(
		@PathVariable String userId,
		@PathVariable String bookId,
		@Valid @RequestBody AddRatingRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(userService.addRating(userId, bookId, request));
	}

	@Operation(summary = "Search users", description = "Search users by query with pagination")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid search or pagination parameters")
	})
	@GetMapping("/search")
	public ResponseEntity<PageResponse<UserResponse>> searchUsers(
		@RequestParam("query") String query,
		@PageableDefault(size = 20, sort = "name") Pageable pageable
	) {
		return ResponseEntity.ok(userService.searchUsers(query, pageable));
	}
}
