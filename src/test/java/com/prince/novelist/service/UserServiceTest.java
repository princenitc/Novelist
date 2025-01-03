package com.prince.novelist.service;

import com.prince.novelist.model.Book;
import com.prince.novelist.model.Review;
import com.prince.novelist.model.User;
import com.prince.novelist.repository.BookRepository;
import com.prince.novelist.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private BookRepository bookRepository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this); // Initializes mocks
	}

	// Test case for creating a user
	@Test
	void testCreateUser() {
		// Given
		User newUser = new User();
		newUser.setName("John");
		newUser.setAge(30L);
		newUser.setId("userId");
		Mockito.when(userRepository.createNewUser(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()))
				.thenReturn(Optional.of(newUser));

		// When
		User createdUser = userService.createUser(newUser);

		// Then
		assertNotNull(createdUser);
		assertEquals("John", createdUser.getName());
		assertEquals(30, createdUser.getAge());
		assertEquals("userId", createdUser.getId());
	}

	// Test case for finding user by ID
	@Test
	void testFindUserById() {
		// Given
		String userId = "existing-user-id";
		User existingUser = new User();
		existingUser.setId(userId);
		existingUser.setName("Jane");
		existingUser.setAge(25L);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(existingUser));

		// When
		User foundUser = userService.findUserById(userId);

		// Then
		assertNotNull(foundUser);
		assertEquals(userId, foundUser.getId());
		assertEquals("Jane", foundUser.getName());
	}

	// Test case for updating a user
	@Test
	void testUpdateUser() {
		// Given
		String userId = "existing-user-id";
		User existingUser = new User();
		existingUser.setId(userId);
		existingUser.setName("Jane");
		existingUser.setAge(25L);

		User updatedUser = new User();
		updatedUser.setName("John Updated");
		updatedUser.setAge(26L);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(existingUser));
		Mockito.when(userRepository.updateUserDetails(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
				.thenReturn(Optional.of(updatedUser));

		// When
		User result = userService.updateUser(updatedUser, userId);

		// Then
		assertNotNull(result);
		assertEquals("John Updated", result.getName());
		assertEquals(26L, result.getAge());
	}

	// Test case for deleting a user
	@Test
	void testDeleteUser() {
		// Given
		String userId = "existing-user-id";
		User existingUser = new User();
		existingUser.setId(userId);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(existingUser));

		// When
		boolean isDeleted = userService.deleteUser(userId);

		// Then
		assertTrue(isDeleted);
		Mockito.verify(userRepository).deleteUserById(userId);
	}

	// Test case for adding a review
	@Test
	void testAddReview() throws Exception {
		// Given
		String userId = "user-id";
		String bookId = "book-id";
		String rating = "5";

		Review review = new Review();
		review.setUserId(userId);
		review.setBookId(bookId);
		review.setRating(rating);

		User user = new User();
		user.setId(userId);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.of(new Book())); // Mocking book presence
		Mockito.when(userRepository.addReview(userId, bookId, rating)).thenReturn(Optional.of(user));

		// When
		User result = userService.addReview(review);

		// Then
		assertNotNull(result);
		Mockito.verify(userRepository).addReview(userId, bookId, rating);
	}

	// Test case for addReview when user or book does not exist
	@Test
	void testAddReviewUserOrBookNotFound() {
		// Given
		Review review = new Review();
		review.setUserId("invalid-user-id");
		review.setBookId("invalid-book-id");
		review.setRating("5");

		Mockito.when(userRepository.getUserById(review.getUserId())).thenReturn(Optional.empty());
		Mockito.when(bookRepository.getBookById(review.getBookId())).thenReturn(Optional.empty());

		// When & Then
		Exception exception = assertThrows(Exception.class, () -> {
			userService.addReview(review);
		});

		assertEquals("Internal Error", exception.getMessage());
	}
}
