package org.example;

import org.example.dao.UserDao;
import org.example.dto.User;

import org.example.extension.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({UserServiceParamResolver.class,
        GlobalExtension.class,
        PostProcessingExtension.class,
        ConditionalExtension.class,
        MockitoExtension.class
//        ThrowableExtension.class
})
public class UserServiceTest {
    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "111");

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<Integer> argumentCaptor;

    @Mock
    private UserDao userDao;

    @BeforeAll
    void init() {
        System.out.println("Before all:" + this);
    }

    @BeforeEach
    void prepare() {
        System.out.println("Before each:" + this);
        Mockito.doReturn(true).when(userDao).delete(IVAN.getId());
//        this.userDao = Mockito.spy(UserDao.class);
//        this.userService = new UserService(userDao);
    }

    @Test
    void throwExceptionIfDatabaseIsNotAvailable() {
        Mockito.doThrow(RuntimeException.class).when(userDao).delete(IVAN.getId());

        assertThrows(RuntimeException.class, () -> userService.delete(IVAN.getId()));
    }

    @Test
    void shouldDeleteExistedUser() {
        userService.add(IVAN);
//        Mockito.doReturn(true).when(userDao).delete(IVAN.getId());
//        Mockito.when(userDao.delete(IVAN.getId())).thenReturn(true); - не гибкий вариант, первый предпочтительнее
//        Mockito.doReturn(true).when(userDao).delete(Mockito.any(); dummy object
        var deleteResult = userService.delete(IVAN.getId());
        var captor = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(userDao, Mockito.times(1)).delete(captor.capture());
        assertThat(captor.getValue()).isEqualTo(IVAN.getId());

//        Mockito.reset(userDao);
        assertThat(deleteResult).isTrue();
    }

    @Test
    void usersEmptyIfNoUsersAdded() throws IOException {
        System.out.println("Test 1:" + this);
        var users = userService.getAll();

        if (true) {
            throw new IOException();
        }

        MatcherAssert.assertThat(users, IsEmptyCollection.empty());
        assertTrue(users.isEmpty(), "User list should be empty");
    }

    @Test
    void usersSizeIfUserAdded () {
        System.out.println("Test 2:" + this);
        var userService = new UserService(new UserDao());
        userService.add(IVAN);
        userService.add(PETR);
        var users = userService.getAll();

        assertThat(users).hasSize(2);
//        assertEquals(2, users.size());
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN, PETR);
        Map<Integer, User> users = userService.getAllConvertedById();

        MatcherAssert.assertThat(users, hasKey(IVAN.getId()));

        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)

        );
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("After each:" + this);
    }

    @AfterAll
    void close() {
        System.out.println("After all:" + this);
    }

    @Nested
    @Tag("login")
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    class LoginTest {
        @Test
        @Disabled("flaky, need to see")
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);
            var dummy = userService.login(IVAN.getUsername(), "dummy");

            assertTrue(dummy.isEmpty());
        }

//        @Test
        @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
        void loginFailIfUserIsNotExist() {
            userService.add(IVAN);
            var dummy = userService.login("dummy", IVAN.getPassword());

            assertTrue(dummy.isEmpty());
        }

        @Test
        void checkLoginFunctionalityPerformance() {
            System.out.println("******* " + Thread.currentThread().getName() + " *******");
            assertTimeoutPreemptively(Duration.ofMillis(200L), () -> {
                System.out.println("******* " + Thread.currentThread().getName() + " *******");
//                Thread.sleep(300L);
                return userService.login("dummy", IVAN.getPassword());
            });
        }

        @Test
        void loginSuccessIfUserExists() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUsername(), IVAN.getPassword());

            assertThat(maybeUser).isPresent();
            maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));

        }

        @Test
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy")),
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null))
            );
        }

        @ParameterizedTest
        @MethodSource("org.example.UserServiceTest#getArgumentsForLoginTest")
//        @NullSource, @EmptySource, @NullAndEmptySource, @CsvFileSource, CsvSource
        @DisplayName("login param test")
        void loginParameterizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);
            var maybeUser = userService.login(username, password);
            assertThat(maybeUser).isEqualTo(user);
        }
    }
    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "123", Optional.of(IVAN)),
                Arguments.of("Petr", "111", Optional.of(PETR)),
                Arguments.of("dummy", "123", Optional.empty()),
                Arguments.of("Ivan", "dummy", Optional.empty())
                );
    }
}
