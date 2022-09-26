package com.works.services;

import com.works.entities.Role;
import com.works.entities.User;
import com.works.repositories.UserRepository;
import com.works.utils.JwtUtil;
import com.works.utils.ERest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.works.props.JWTLogin;

@Service
@Transactional
public class UserDetailService implements UserDetailsService {

    final UserRepository userRepository;
    final JwtUtil jwtUtil;
    final AuthenticationManager authenticationManager;
    public UserDetailService(UserRepository userRepository, JwtUtil jwtUtil, @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> optionalJWTUser = userRepository.findByEmailEqualsIgnoreCase(username);
        if (optionalJWTUser.isPresent()) {
            User u = optionalJWTUser.get();
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    u.getEmail(),
                    u.getPassword(),
                    u.isEnabled(),
                    u.isTokenExpired(),
                    true,
                    true,
                    roles(u.getRoles())
            );
            return userDetails;
        }else {
            throw new UsernameNotFoundException("Böyle bir kullanıcı yok");
        }
    }


    public Collection roles(List<Role> rolex ) {
        List<GrantedAuthority> ls = new ArrayList<>();
        for ( Role role : rolex ) {
            ls.add( new SimpleGrantedAuthority( role.getName() ));
        }
        return ls;
    }

    public ResponseEntity register(User jwtUser) {
        Optional<User> optionalJWTUser = userRepository.findByEmailEqualsIgnoreCase(jwtUser.getEmail());
        Map<ERest, Object> hm = new LinkedHashMap();
        if ( !optionalJWTUser.isPresent() ) {
            jwtUser.setPassword(encoder().encode( jwtUser.getPassword() )  );
            User user = userRepository.save(jwtUser);
            hm.put(ERest.status, true);
            hm.put(ERest.result, user);
            return new ResponseEntity( hm , HttpStatus.OK);
        }else {
            hm.put(ERest.status, false);
            hm.put(ERest.message, "Bu mail daha kayıt edilmiş");
            hm.put(ERest.result, jwtUser);
            return new ResponseEntity( hm, HttpStatus.NOT_ACCEPTABLE );
        }
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }


    // aut
    // jwt almak için login işlemi yaparak bu fonksiyon tetiklenmeldir.
    public ResponseEntity auth(JWTLogin jwtLogin) {
        Map<ERest, Object> hm = new LinkedHashMap<>();
        try {
            authenticationManager.authenticate( new UsernamePasswordAuthenticationToken(
                    jwtLogin.getUsername(), jwtLogin.getPassword()
            ) );
            UserDetails userDetails = loadUserByUsername(jwtLogin.getUsername());
            String jwt = jwtUtil.generateToken(userDetails);
            hm.put(ERest.status, true);
            hm.put( ERest.jwt, jwt );
            hm.put(ERest.result, userDetails);
            return new ResponseEntity(hm, HttpStatus.OK);
        }catch (Exception ex) {
            hm.put(ERest.status, false);
            hm.put( ERest.error, ex.getMessage() );
            return new ResponseEntity(hm, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
