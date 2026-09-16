package com.thehartford.identityservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity mapping to the `users` table.
 *
 * <p>userId is a BIGINT AUTO_INCREMENT in MySQL. It must be left null
 * before calling save() so Spring Data R2DBC issues an INSERT instead of an UPDATE.
 * The database assigns the ID and Spring populates it on the returned entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    @Column("user_id")
    private Long userId;   // null before insert → DB assigns AUTO_INCREMENT value

    @Column("name")
    private String name;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("role")
    private UserRole role;

    @Column("status")
    private UserStatus status;
}
