package com.prince.novelist.service;

import com.prince.novelist.exception.InvalidRequestException;
import com.prince.novelist.exception.ResourceNotFoundException;
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
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void shouldCreateUser() {
		// Given
		User newUser = new User();
		newUser.setName("John");
		newUser.setAge(30L);
		newUser.setUserId("userId");
		
		Mockito.when(userRepository.createNewUser(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()))
				.thenReturn(Optional.of(newUser));

		// When
		User createdUser = userService.createUser(newUser);

		// Then
		assertNotNull(createdUser);
		assertEquals("John", createdUser.getName());
		assertEquals(30, createdUser.getAge());
		assertEquals("userId", createdUser.getUserId());
	}

	@Test
	void shouldThrowInvalidRequestExceptionWhenUserCreationFails() {
		// Given
		User newUser = new User();
		newUser.setName("John");
		newUser.setAge(30L);
		
		Mockito.when(userRepository.createNewUser(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString()))
				.thenReturn(Optional.empty());

		// When & Then
		assertThrows(InvalidRequestException.class, () -> userService.createUser(newUser));
	}

	@Test
	void shouldFindUserById() {
		// Given
		String userId = "existing-user-id";
		User existingUser = new User();
		existingUser.setUserId(userId);
		existingUser.setName("Jane");
		existingUser.setAge(25L);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(existingUser));

		// When
		User foundUser = userService.getUserById(userId);

		// Then
		assertNotNull(foundUser);
		assertEquals(userId, foundUser.getUserId());
		assertEquals("Jane", foundUser.getName());
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
		// Given
		String userId = "non-existent-id";
		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));
	}

	@Test
	void shouldUpdateUser() {
		// Given
		String userId = "existing-user-id";
		User existingUser = new User();
		existingUser.setUserId(userId);
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

	@Test
	void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentUser() {
		// Given
		String userId = "non-existent-id";
		User updatedUser = new User();
		updatedUser.setName("Test");
		updatedUser.setAge(25L);
		
		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(updatedUser, userId));
	}

	@Test
	void shouldDeleteUser() {
		// Given
		String userId = "existing-user-id";
		User existingUser = new User();
		existingUser.setUserId(userId);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(existingUser));

		// When
		userService.deleteUser(userId);

		// Then
		Mockito.verify(userRepository).deleteUserById(userId);
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentUser() {
		// Given
		String userId = "non-existent-id";
		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(userId));
	}

	@Test
	void shouldAddReview() {
		// Given
		String userId = "user-id";
		String bookId = "book-id";
		Integer rating = 5;

		Review review = new Review();
		review.setUserId(userId);
		review.setBookId(bookId);
		review.setRating(rating);

		User user = new User();
		user.setUserId(userId);
		Book book = new Book();
		book.setBookId(bookId);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
		Mockito.when(bookRepository.getBookById(bookId)).thenReturn(Optional.of(book));
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

		// When
		User result = userService.addReview(review);

		// Then
		assertNotNull(result);
		Mockito.verify(userRepository).save(Mockito.any(User.class));
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenAddingReviewForNonExistentUser() {
		// Given
		Review review = new Review();
		review.setUserId("invalid-user-id");
		review.setBookId("book-id");
		review.setRating(5);

		Mockito.when(userRepository.getUserById(review.getUserId())).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> userService.addReview(review));
	}

	@Test
	void shouldThrowResourceNotFoundExceptionWhenAddingReviewForNonExistentBook() {
		// Given
		String userId = "user-id";
		Review review = new Review();
		review.setUserId(userId);
		review.setBookId("invalid-book-id");
		review.setRating(5);

		User user = new User();
		user.setUserId(userId);

		Mockito.when(userRepository.getUserById(userId)).thenReturn(Optional.of(user));
		Mockito.when(bookRepository.getBookById(review.getBookId())).thenReturn(Optional.empty());

		// When & Then
		assertThrows(ResourceNotFoundException.class, () -> userService.addReview(review));
	}
}
