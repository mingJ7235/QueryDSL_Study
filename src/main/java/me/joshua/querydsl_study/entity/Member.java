package me.joshua.querydsl_study.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@ToString (of = {"id", "username", "age"})
public class Member {
    @Id
    @GeneratedValue
    @Column (name = "member_id")
    private Long id;

    private String username;

    private int age;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "team_id")
    private Team team;

    public Member(final String username) {
        this(username, 0);
    }

    public Member(final String username, final int age) {
        this(username, age, null);
    }

    public Member(final String username, final int age, final Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(final Team team) {
        this.team = team;
        team.getMembers().add(this);
    }

}
