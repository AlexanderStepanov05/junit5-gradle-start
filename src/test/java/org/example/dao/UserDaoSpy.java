package org.example.dao;

import org.mockito.stubbing.Answer1;

import java.util.HashMap;
import java.util.Map;

public class UserDaoSpy extends UserDao {
    private Map<Integer, Boolean> answers = new HashMap<>();
//    private Answer1<Integer, Boolean> answer1;

    private final UserDao userDao;

    public UserDaoSpy(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public boolean delete(Integer userId) {
        return answers.getOrDefault(userId, false);
    }
}
