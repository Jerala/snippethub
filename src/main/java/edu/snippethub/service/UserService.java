package edu.snippethub.service;

import edu.snippethub.dao.UserDAO;
import edu.snippethub.dto.UserDTO;
import edu.snippethub.entity.User;
import edu.snippethub.exception.ValueAlreadyExistException;
import edu.snippethub.model.UserModel;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.Arrays;

@Service(value = "userService")
public class UserService implements UserDetailsService {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private UserDTO userDTO;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = null;
        try {
            user = userDAO.getUserByName(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (user == null) {
            throw new UsernameNotFoundException("Invalid name or password.");
        }
        System.out.println(user.getRole());
        return new org.springframework.security.core.userdetails.User(user.getUserName(), user.getPassword(),
                Arrays.asList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }

    public void save(User user) {
        try {
            userDAO.addUser(user);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public User getUserByName(String userName) {
        try {
            return userDAO.getUserByName(userName);
        } catch (NullPointerException | SQLException e) {
            return null;
        }
    }

    public User registerUser(UserModel userModel) throws NullPointerException, ValueAlreadyExistException {
        User user = null;
        if(!isEmailOrUsernameExists(userModel.getUserName(), userModel.getEmail())) {
            user = userDTO.createUserFromUserModel(userModel);
            save(user);
        }
        else {
            throw new ValueAlreadyExistException("user name or email already exist");
        }
        return user;
    }

    private boolean isEmailOrUsernameExists(String userName, String email) {
        User user = null;
        try {
            user = userDAO.getUserByEmailOrUsername(userName, email);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user == null ? false : true;
    }

}
