package org.lf10.stimmungsumfrage.Models;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private LocalDateTime lastSubmission = LocalDateTime.of(1970, 1, 1, 0, 0);

    @Column(nullable = false)
    private Boolean enabled = true;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="role_id", nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserChannelFeedbackStatus> channelFeedbackStatuses = new HashSet<>();

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Assuming Role has a 'name' like "USER" or "ADMIN"
        return Collections.singletonList(new SimpleGrantedAuthority(role.getName()));
    }

    @Override
    @NonNull
    public String getUsername() {
        return email; // Spring Security username is usually the email
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // implement your logic if needed
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // implement your logic if needed
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // implement your logic if needed
    }
    @Override
    public boolean isEnabled() {
        return enabled; // implement your logic if needed
    }

    public boolean canSubmitFeedback() {
        return this.getLastSubmission().isAfter(LocalDateTime.now().minusDays(1));
    }
}
