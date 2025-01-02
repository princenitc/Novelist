package com.prince.novelist.service;

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
	public Collection<User> getAll() {
		return userRepository.getAllUsers();
	}

	public User createUser(User user) {
		String name = user.getName();
		Long age = user.getAge();
		String userId = UUID.randomUUID().toString();
		return userRepository.createNewUser(name, age,userId);
	}

	public User findUserById(String id) {
		Optional<User> user = userRepository.findUserById(id);
		return user.orElse(null);
	}

	public User updateUser(User user, String id) {
		if( userRepository.findUserById(id).isPresent()) {
			String name = user.getName();
			Long age = user.getAge();
			return userRepository.updateUserDetails(id,name,age);
		}
		return null;
	}

	public boolean deleteUser(String id) {
		if(userRepository.findUserById(id).isPresent()) {
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
	public User addReview(Review review) {
		String userId = review.getUserId();
		String bookId = review.getBookId();
		String rating = review.getRating();
		if(userRepository.findUserById(userId).isPresent() && bookRepository.getBookById(bookId).isPresent()) {
			Optional<User> user = userRepository.addReview(userId, bookId, rating);
			if(user.isPresent()) {
				return user.get();
			}
		}
		return null;
	}
}
