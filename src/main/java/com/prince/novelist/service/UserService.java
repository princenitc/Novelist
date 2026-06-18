package com.prince.novelist.service;

import com.prince.novelist.dto.request.AddRatingRequest;
import com.prince.novelist.dto.request.CreateUserRequest;
import com.prince.novelist.dto.request.UpdateUserRequest;
import com.prince.novelist.dto.response.PageResponse;
import com.prince.novelist.dto.response.RatingResponse;
import com.prince.novelist.dto.response.UserResponse;
import com.prince.novelist.exception.BadRequestException;
import com.prince.novelist.exception.DuplicateResourceException;
import com.prince.novelist.exception.ResourceNotFoundException;
import com.prince.novelist.mapper.BookMapper;
import com.prince.novelist.mapper.UserMapper;
import com.prince.novelist.model.Book;
import com.prince.novelist.model.RatingRelation;
import com.prince.novelist.model.User;
import com.prince.novelist.repository.BookRepository;
import com.prince.novelist.repository.UserRepository;
import com.prince.novelist.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final BookRepository bookRepository;
	private final UserMapper userMapper;
	private final BookMapper bookMapper;
	private final com.prince.novelist.publisher.DomainEventPublisher eventPublisher;

	public UserService(UserRepository userRepository, BookRepository bookRepository, UserMapper userMapper, BookMapper bookMapper, com.prince.novelist.publisher.DomainEventPublisher eventPublisher) {
		this.userRepository = userRepository;
		this.bookRepository = bookRepository;
		this.userMapper = userMapper;
		this.bookMapper = bookMapper;
		this.eventPublisher = eventPublisher;
	}

	public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
		log.debug("Fetching all users with pagination: page={}, size={}, sort={}",
			pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

		Page<User> usersPage = userRepository.findAll(pageable);
		return PaginationUtil.createPageResponse(usersPage, userMapper::toResponse);
	}

	public UserResponse getUserById(String userId) {
		validateUserId(userId);
		log.debug("Fetching user by id: {}", userId);

		User user = findUserById(userId);
		return userMapper.toResponse(user);
	}

	@Transactional
	public UserResponse createUser(CreateUserRequest request) {
		if (request == null) {
			throw new BadRequestException("Create user request must not be null");
		}

		log.info("Creating user with name='{}', email='{}'", request.getName(), request.getEmail());

		validateDuplicateEmail(request.getEmail(), null);

		User user = userMapper.toEntity(request);
		User savedUser = userRepository.save(user);

		log.info("Created user with id={}", savedUser.getUserId());
		return userMapper.toResponse(savedUser);
	}

	@Transactional
	public UserResponse updateUser(String userId, UpdateUserRequest request) {
		validateUserId(userId);

		if (request == null) {
			throw new BadRequestException("Update user request must not be null");
		}

		log.info("Updating user with id={}", userId);

		User existingUser = findUserById(userId);
		validateDuplicateEmail(request.getEmail(), existingUser.getEmail());

		userMapper.updateEntity(existingUser, request);
		User updatedUser = userRepository.save(existingUser);

		log.info("Updated user with id={}", userId);
		return userMapper.toResponse(updatedUser);
	}

	@Transactional
	public void deleteUser(String userId) {
		validateUserId(userId);
		log.info("Deleting user with id={}", userId);

		User existingUser = findUserById(userId);
		userRepository.delete(existingUser);

		log.info("Deleted user with id={}", userId);
	}

	@Transactional
	public RatingResponse addRating(String userId, String bookId, AddRatingRequest request) {
		validateUserId(userId);
		validateBookId(bookId);

		if (request == null) {
			throw new BadRequestException("Add rating request must not be null");
		}

		log.info("Adding/Updating rating for userId={}, bookId={}, rating={}", userId, bookId, request.getRating());

		User user = findUserById(userId);
		Book book = findBookById(bookId);

		if (user.getRatedBooks() == null) {
			throw new BadRequestException("User ratings collection is not initialized");
		}

		Optional<RatingRelation> existingRatingOpt = user.getRatedBooks().stream()
			.filter(relation -> relation.getBook() != null && bookId.equals(relation.getBook().getBookId()))
			.findFirst();

		RatingRelation savedRating;
		if (existingRatingOpt.isPresent()) {
			savedRating = existingRatingOpt.get();
			savedRating.setRating(request.getRating());
			savedRating.setReview(request.getReview());
			log.info("Updated existing rating for userId={}, bookId={}", userId, bookId);
		} else {
			RatingRelation ratingRelation = new RatingRelation(book, request.getRating(), request.getReview());
			ratingRelation.setHelpful(0);
			user.getRatedBooks().add(ratingRelation);
			savedRating = ratingRelation;
			log.info("Added new rating for userId={}, bookId={}", userId, bookId);
		}

		userRepository.save(user);

		eventPublisher.publishRatingEvent(new com.prince.novelist.event.RatingEvent(null, userId, bookId, request.getRating(), request.getReview(), 0));

		return mapRatingResponse(savedRating);
	}

	public PageResponse<UserResponse> searchUsers(String query, Pageable pageable) {
		if (query == null || query.trim().isEmpty()) {
			throw new BadRequestException("Search query must not be blank");
		}

		String normalizedQuery = query.trim();
		log.debug("Searching users with query='{}', page={}, size={}, sort={}",
			normalizedQuery, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

		Page<User> usersPage = userRepository.findByNameContaining(normalizedQuery, pageable);
		return PaginationUtil.createPageResponse(usersPage, userMapper::toResponse);
	}

	private User findUserById(String userId) {
		Optional<User> user = userRepository.getUserById(userId);
		return user.orElseThrow(() -> {
			log.warn("User not found with id={}", userId);
			return new ResourceNotFoundException("User not found with id: " + userId);
		});
	}

	private Book findBookById(String bookId) {
		Optional<Book> book = bookRepository.getBookById(bookId);
		return book.orElseThrow(() -> {
			log.warn("Book not found with id={}", bookId);
			return new ResourceNotFoundException("Book not found with id: " + bookId);
		});
	}

	private void validateUserId(String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			throw new BadRequestException("User id must not be blank");
		}
	}

	private void validateBookId(String bookId) {
		if (bookId == null || bookId.trim().isEmpty()) {
			throw new BadRequestException("Book id must not be blank");
		}
	}

	private void validateDuplicateEmail(String email, String currentEmail) {
		if (email == null || email.trim().isEmpty()) {
			return;
		}

		String normalizedEmail = email.trim();
		if (normalizedEmail.equalsIgnoreCase(currentEmail)) {
			return;
		}

		if (userRepository.existsByEmail(normalizedEmail)) {
			log.warn("Duplicate email detected: {}", normalizedEmail);
			throw new DuplicateResourceException("User", normalizedEmail);
		}
	}

	private RatingResponse mapRatingResponse(RatingRelation relation) {
		RatingResponse response = new RatingResponse();
		response.setBook(relation.getBook() == null ? null : bookMapper.toResponse(relation.getBook()));
		response.setRating(relation.getRating());
		response.setReview(relation.getReview());
		response.setTimestamp(relation.getTimestamp());
		response.setHelpfulCount(relation.getHelpful());
		return response;
	}
}
