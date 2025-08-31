package org.example.moliyaapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.moliyaapp.exception.ResourceNotFoundException;
import org.example.moliyaapp.repository.UserRepository;
import org.example.moliyaapp.utils.RestConstants;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository.findByPhoneNumberAndDeletedFalse(username)
                .orElseThrow(() -> new ResourceNotFoundException(RestConstants.USER_NOT_FOUND));
    }
}
