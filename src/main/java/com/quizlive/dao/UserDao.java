package com.quizlive.dao;

import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;

import java.sql.SQLException;
import java.util.List;

public interface UserDao {

    AppUser findById(int id) throws SQLException;

    AppUser findByEmail(String email) throws SQLException;

    AppUser create(AppUser user) throws SQLException;

    boolean update(AppUser user) throws SQLException;

    default boolean updateRank(int id, String rank) throws SQLException {
        return false;
    }

    boolean delete(int id) throws SQLException;

    List<AppUser> listAll() throws SQLException;

    List<AppUser> listByRole(Role role) throws SQLException;
}
