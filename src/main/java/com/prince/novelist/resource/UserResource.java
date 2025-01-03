package com.prince.novelist.resource;

import com.prince.novelist.model.Review;
import com.prince.novelist.model.User;
import com.prince.novelist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/users")
public class UserResource {

	@Autowired
	UserService userService;

	@PostMapping("/create")
	public User createUser(@RequestBody User user) {
		return userService.createUser(user);
	}
	@GetMapping("/all")
	public Collection<User> getAll() {
		return userService.getAll();
	}

	@GetMapping("{userId}")
	public User get(@PathVariable("userId") String id) {
		return userService.findUserById(id);
	}

	@PutMapping("/update/{userId}")
	public User update(@RequestBody User user, @PathVariable("userId") String id) {
		return userService.updateUser(user, id);
	}

	@DeleteMapping("/delete/{userId}")
	public boolean deleteUser(@PathVariable("userId") String userId) {
		return userService.deleteUser(userId);
	}

	@PostMapping("/addReview")
	public User addReview(@RequestBody Review review) throws Exception {
		return userService.addReview(review);
	}
}
