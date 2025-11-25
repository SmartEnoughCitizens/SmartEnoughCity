package com.trinity.hermes.UserManagement.Repo;

import com.trinity.hermes.UserManagement.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserManagementRepo extends JpaRepository<User,Integer> {

    Optional<User> findByUserId(String givenUserID);
}
