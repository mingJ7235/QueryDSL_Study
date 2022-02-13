package me.joshua.querydsl_study;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.joshua.querydsl_study.entity.Member;
import me.joshua.querydsl_study.entity.QMember;
import me.joshua.querydsl_study.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static me.joshua.querydsl_study.entity.QMember.*;
import static me.joshua.querydsl_study.entity.QMember.member;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPQLQueryFactory queryFactory;

    @BeforeEach
    public void before () {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamA);
        Member member3 = new Member("member3", 10, teamB);
        Member member4 = new Member("member4", 10, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL () {
        //member1 찾기
        Member member1 = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl () {
        Member member1 = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search () {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.in(10,20)))
                .fetchOne();

        List<Member> memberStartwithMem = queryFactory
                .selectFrom(member)
                .where(member.username.startsWith("mem"))
                .fetch();

        assertThat(findMember.getUsername()).isEqualTo("member1");

        for (Member member1 : memberStartwithMem) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void searchAndParam () {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void resultFetch () {

        //fetch : list조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //fetchone : 단건 조회 // 둘 이상이면 NonUniqueResultException이 터진다.
        Member member = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        //fetchFirst : limit 를 1 걸고 fetchOne 하는 것
        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();
                //limit(1).fetchOne()

        //fetchResult : paging 정보를 담고 있다. total count query가 추가로 발생한다.
        QueryResults<Member> results = queryFactory
                .selectFrom(QMember.member)
                .fetchResults();

        results.getTotal(); //paging 하기 위한 total count를 같이 가져옴
        List<Member> content = results.getResults();

        //fetchCount : count query를 날리는 것
        long count = queryFactory
                .selectFrom(QMember.member)
                .fetchCount();

    }
}
