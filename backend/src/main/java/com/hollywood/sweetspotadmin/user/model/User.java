package com.hollywood.sweetspotadmin.user.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "user_email_provider_unique", columnNames = { "email", "provider" })
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column
    private String password;

    @Column(nullable = false)
    private String name;

    @Column
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    // 🔻 추가: User가 삭제될 때 연관된 roles 데이터도 함께 삭제되도록 설정합니다.
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Role> roles;

    @Builder
    public User(String email, String password, String name, String pictureUrl, Provider provider, List<Role> roles) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.provider = provider;
        this.roles = roles;
    }

    public User update(String name, String pictureUrl) {
        this.name = name;
        this.pictureUrl = pictureUrl;
        return this;
    }

    // ✅ [추가] 권한 수정을 위한 setter
    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}