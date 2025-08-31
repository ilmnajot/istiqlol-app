package org.example.moliyaapp.utils;

import org.example.moliyaapp.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserSession {



    public Long getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            System.out.println("Principal class : " + principal.getClass().getName());
            if (principal instanceof User user)
                return user.getId();
        }
        return null;
    }

}
