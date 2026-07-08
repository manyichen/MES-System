package com.mes.jdbc;
/*userdaotest*/
/*1111111*/
/*22222222*/
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoTest {
    private static final String ADD_USERNAME = "jdbc_add_test";
    private static final String UPDATE_USERNAME = "jdbc_update_test";
    private static final String DELETE_USERNAME = "jdbc_delete_test";

    private final UserDao userDao = new UserDao();

    @BeforeAll
    static void createDatabaseAndTable() throws SQLException {
        DatabaseInitializer.initialize();
    }

    @BeforeEach
    void cleanTestRows() throws SQLException {
        userDao.deleteUser(ADD_USERNAME);
        userDao.deleteUser(UPDATE_USERNAME);
        userDao.deleteUser(DELETE_USERNAME);
    }

    @Test
    void addUserShouldInsertOneRecord() throws SQLException {
        userDao.addUser(ADD_USERNAME, "add_password");

        assertEquals("add_password", userDao.findPasswordByUsername(ADD_USERNAME).orElseThrow());
    }

    @Test
    void updatePasswordShouldModifyExistingRecord() throws SQLException {
        userDao.addUser(UPDATE_USERNAME, "old_password");

        boolean updated = userDao.updatePassword(UPDATE_USERNAME, "new_password");

        assertTrue(updated);
        assertEquals("new_password", userDao.findPasswordByUsername(UPDATE_USERNAME).orElseThrow());
    }

    @Test
    void deleteUserShouldRemoveExistingRecord() throws SQLException {
        userDao.addUser(DELETE_USERNAME, "delete_password");

        boolean deleted = userDao.deleteUser(DELETE_USERNAME);

        assertTrue(deleted);
        assertFalse(userDao.findPasswordByUsername(DELETE_USERNAME).isPresent());
    }
}
