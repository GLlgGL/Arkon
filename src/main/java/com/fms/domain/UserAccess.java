package com.fms.domain;

import jakarta.persistence.*;

/** An access override for one user and one area. */
@Entity
@Table(name = "USER_ACCESS")
public class UserAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String username;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Area area;
    @Enumerated(EnumType.STRING)
    @Column(name = "ACCESS_LEVEL", nullable = false)
    private Area.Level level;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public Area getArea() { return area; }
    public void setArea(Area a) { this.area = a; }
    public Area.Level getLevel() { return level; }
    public void setLevel(Area.Level l) { this.level = l; }
}
