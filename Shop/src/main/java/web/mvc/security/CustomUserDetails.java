package web.mvc.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import web.mvc.domain.User;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@Slf4j
public class CustomUserDetails implements UserDetails {
    private final User user;
    public CustomUserDetails(User user) {
        this.user = user;
        log.info("CustomUserDetails : {}", user);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        log.info("getAuthorities.....");
        Collection<GrantedAuthority> collection = new ArrayList<GrantedAuthority>();
        collection.add(()->user.getRole());
        return collection;
    }

    @Override
    public String getPassword() {
        log.info("getPassword....");
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        log.info("getUsername....");
        return user.getUserId();
    }

    @Override
    public boolean isAccountNonExpired() {
        log.info("isAccountNonExpired....");
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        log.info("isAccountNonLocked....");
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        log.info("isCredentialsNonExpired....");
        return true;
    }

    @Override
    public boolean isEnabled() {
        log.info("isEnabled....");
        return true;
    }
}
