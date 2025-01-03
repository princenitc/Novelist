package com.prince.novelist.service;

import com.prince.novelist.model.Review;
import com.prince.novelist.model.User;
import com.prince.novelist.repository.BookRepository;
import com.prince.novelist.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
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
	public Collection<User> getAll() {
		return userRepository.getAllUsers();
	}

	public User createUser(User user) {
		String genId = user.getId() != null ? user.getId() : UUID.randomUUID().toString();
		user.setId(genId);
		Optional<User> createdUser =  userRepository.createNewUser(user.getName(), user.getAge(), user.getId());
		return createdUser.orElseThrow(() -> new IllegalArgumentException("User Creation Failed"));
	}

	public User findUserById(String id) {
		Optional<User> user = userRepository.getUserById(id);
		return user.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	public User updateUser(User user, String id) {
			if(userRepository.getUserById(id).isPresent()) {
				Optional<User> updatedUser = userRepository.updateUserDetails(id,user.getName(),user.getAge());
				return updatedUser.orElseThrow(() -> new IllegalArgumentException("User Update failed"));
			} else {
				throw new IllegalArgumentException("Invalid Arguments");
			}
	}

	public boolean deleteUser(String id) {
		if(userRepository.getUserById(id).isPresent()) {
			userRepository.deleteUserById(id);
			return true;
		} else {
			return false;
		}
	}

	/*
	object structure could be :
	userId,
	bookId,
	rating
	 */
	public User addReview(Review review) throws Exception {
		String userId = review.getUserId();
		String bookId = review.getBookId();
		String rating = review.getRating();
		if(userRepository.getUserById(userId).isPresent() && bookRepository.getBookById(bookId).isPresent()) {
			Optional<User> ratedUser = userRepository.addReview(userId, bookId, rating);
			return ratedUser.orElseThrow(()-> new IllegalArgumentException("rating addition failed"));
		} else {
			throw new Exception("Internal Error");
		}
	}
}
