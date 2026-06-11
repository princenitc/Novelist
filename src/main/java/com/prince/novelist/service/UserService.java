package com.prince.novelist.service;

import com.prince.novelist.exception.InvalidRequestException;
import com.prince.novelist.exception.ResourceNotFoundException;
import com.prince.novelist.model.Book;
import com.prince.novelist.model.RatingRelation;
import com.prince.novelist.model.Review;
import com.prince.novelist.model.User;
import com.prince.novelist.repository.BookRepository;
import com.prince.novelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

	@Autowired
	UserRepository userRepository;

	@Autowired
	BookRepository bookRepository;

	public Collection<User> getAllUsers() {
		return userRepository.getAllUsers();
	}

	public User createUser(User user) {
		String genId = user.getUserId() != null ? user.getUserId() : UUID.randomUUID().toString();
		user.setUserId(genId);
		Optional<User> createdUser = userRepository.createNewUser(user.getName(), user.getAge(), user.getUserId());
		return createdUser.orElseThrow(() ->
			new InvalidRequestException("Failed to create user with provided data"));
	}

	public User getUserById(String id) {
		Optional<User> user = userRepository.getUserById(id);
		return user.orElseThrow(() ->
			new ResourceNotFoundException("User not found with id: " + id));
	}

	public User updateUser(User user, String id) {
		// Check if user exists first
		if (!userRepository.getUserById(id).isPresent()) {
			throw new ResourceNotFoundException("User not found with id: " + id);
		}
		
		Optional<User> updatedUser = userRepository.updateUserDetails(id, user.getName(), user.getAge());
		return updatedUser.orElseThrow(() ->
			new InvalidRequestException("Failed to update user with provided data"));
	}

	public void deleteUser(String id) {
		if (!userRepository.getUserById(id).isPresent()) {
			throw new ResourceNotFoundException("User not found with id: " + id);
		}
		userRepository.deleteUserById(id);
	}

	public User addReview(Review review) {
		String userId = review.getUserId();
		String bookId = review.getBookId();
		Integer rating = review.getRating();
		
		// Verify user exists
		User user = userRepository.getUserById(userId)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		
		// Verify book exists
		Book book = bookRepository.getBookById(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
		
		// Create rating relationship
		RatingRelation ratingRelation = new RatingRelation(book, rating);
		user.getRatedBooks().add(ratingRelation);
		
		// Save and return updated user
		return userRepository.save(user);
	}
}
