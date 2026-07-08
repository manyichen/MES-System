package com.mes.jdbc;
/*1111111*/
/*22222222*/
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {
    public void addUser(String username, String password) throws SQLException {
        String sql = "INSERT INTO `user` (`username`, `password`) VALUES (?, ?)";

        try (Connection connection = DatabaseInitializer.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
        }
    }

    public boolean updatePassword(String username, String newPassword) throws SQLException {
        String sql = "UPDATE `user` SET `password` = ? WHERE `username` = ?";

        try (Connection connection = DatabaseInitializer.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setString(2, username);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteUser(String username) throws SQLException {
        String sql = "DELETE FROM `user` WHERE `username` = ?";

        try (Connection connection = DatabaseInitializer.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<String> findPasswordByUsername(String username) throws SQLException {
        String sql = "SELECT `password` FROM `user` WHERE `username` = ?";

        try (Connection connection = DatabaseInitializer.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getString("password"));
                }
                return Optional.empty();
            }
        }
    }
}
