package me.joshua.querydsl_study;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.joshua.querydsl_study.entity.Member;
import me.joshua.querydsl_study.entity.QMember;
import me.joshua.querydsl_study.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static me.joshua.querydsl_study.entity.QMember.*;
import static me.joshua.querydsl_study.entity.QMember.member;
import static me.joshua.querydsl_study.entity.QTeam.team;
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
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

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

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 올림차수 (asc)
     * 단 2 에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort () {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(10, 100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        Member member1 = result.get(3);


        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void paging1 () {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("전체 조회수가 필요한 경우")
    public void paging2 () {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    @DisplayName("집합 : count, sum, avg, max, min / Tuple")
    public void aggregation () {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .where(member.username.startsWith("memb"))
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     * groupBy, having
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    // Join

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join () {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /**
     * 세타조인
     * 회원이 이름이 팀 이름과 같은 회원 조회
     * (약간 억지성 -> 연관 관계가 없는 조인을 보여주기 위함임)
     *
     * from 절에 여러 엔티티를 선택해서 세타조인
     * 외부 조인이 불가 했으나 join on을 사용하면 외부 조인 가능
     */
    @Test
    public void theta_join () {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> theta_result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(theta_result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

}
