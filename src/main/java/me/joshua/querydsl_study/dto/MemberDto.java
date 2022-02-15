package me.joshua.querydsl_study.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;

    private int age;

    public MemberDto(final String username, final int age) {
        this.username = username;
        this.age = age;
    }

}
