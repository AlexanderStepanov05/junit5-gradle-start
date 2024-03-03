package org.example;

import org.example.dto.User;

import java.util.*;

import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;

public class UserService {

    private final List<User> users = new ArrayList<>();

    public void add(User... users) {
        this.users.addAll(Arrays.asList(users));
    }

    public List<User> getAll() {
        return users;
    }

    public Optional<User> login(String username, String password) {
        if (username == null || password == null) throw new IllegalArgumentException("username or password is null");
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .filter(user -> user.getPassword().equals(password))
                .findFirst();
    }

    public Map<Integer, User> getAllConvertedById() {
        return users.stream()
                .collect(toMap(User::getId, identity()));
    }
}
