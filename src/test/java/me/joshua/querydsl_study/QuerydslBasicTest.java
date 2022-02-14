package me.joshua.querydsl_study;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
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
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

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
     * 외부 조인이 불가 했으나 join on을 사용하면 외부 조인 가능     */
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

    /**
     * join on 절
     *
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * jpql: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        /**
         * 결과 값
         * tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
         * tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
         * tuple = [Member(id=5, username=member3, age=30), null]
         * tuple = [Member(id=6, username=member4, age=40), null]
         */
    }

    @Test
    public void join_on_filtering_inner() {
        /**
         * Join 대상을 필터링하는 경우
         * inner join 에서 on은 where로 대체해서 쓰는것이 좋다.
         * 외부조인 (leftJoin or rightJoin) 일 때만만on을 쓰는 것이 좋다.
         */
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                //.join(member.team, team).on(team.name.eq("teamA"))
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     *
     * 회원의 이름이 팀 이름과 같은 대상을 외부 조인하는 경우
     */
    @Test
    public void join_on_no_relation () {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }

        /**
         * 막 조인 결과 >
         * tuple = [Member(id=3, username=member1, age=10), null]
         * tuple = [Member(id=4, username=member2, age=20), null]
         * tuple = [Member(id=5, username=member3, age=30), null]
         * tuple = [Member(id=6, username=member4, age=40), null]
         * tuple = [Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
         * tuple = [Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
         * tuple = [Member(id=9, username=teamC, age=0), null]
         */

    }

    /**
     * 패치조인
     *
     * 패치조인은 연관된 엔티티를 SQL 한번에 조회하는 기능
     * 주로 성능 최적화에 사용하는 방법.
     */

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("FetchJoin 미적용")
    public void noFetchJoin() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // fetch가 아닌경우 (Lazy로딩으로 연관관계가 엮여잇음) load를 해오는지 검증해준다.
        // fetch가 아니므로 false가 나와야한다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).isFalse();

    }

    @Test
    @DisplayName("FetchJoin 적용")
    public void useFetchJoin() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();

    }

    /**
     * 서브쿼리
     * JPAExpressions 사용
     *
     * from 절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리는 지원하지 않는다.
     *
     * 해결방안>
     * 1. 서브쿼리를 join으로 변경한다. (가능한 경우도, 불가능한 경우도 존재)
     * 2. 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     */
    @Test
    @DisplayName("서브쿼리1 - 나이가 가장 많은 회원 조회")
    public void subQuery () {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @Test
    @DisplayName("서브쿼리2 - 나이가 평균이상인 회원 조회")
    public void subQuery_goe () {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    @DisplayName("서브쿼리3 - in 을 사용하는 예제")
    public void subQuery_in () {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)) //20, 30, 40 이 서브쿼리의 결과
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    @DisplayName("서브쿼리4 - select 절에 서브쿼리")
    public void subQuery_select () {

        QMember memberSub = new QMember("memberSub");


        List<Tuple> fetch = queryFactory
                .select(member.username,
                        JPAExpressions //static import 가능하다.
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
        /**
         * 결과값
         * tuple = [member1, 25.0]
         * tuple = [member2, 25.0]
         * tuple = [member3, 25.0]
         * tuple = [member4, 25.0]
         */
    }

    /**
     * Case 문
     * -> select, where 절에서 사용가능
     *
     * but, 가급적이면 db 단에서는 이런 로직을 넣지말자.
     * 가공시키는것은 비지니스 로직에서 사용하자.
     */

    @Test
    public void BasicCase () {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase () {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0 ~ 20살")
                        .when(member.age.between(21, 30)).then("21살 ~ 30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 상수 문자 더하기
     */

    @Test
    @DisplayName("상수 붙여서 결과값 내기")
    public void constant () {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
        /**
         * 결과
         * tuple = [member1, A]
         * tuple = [member2, A]
         * tuple = [member3, A]
         * tuple = [member4, A]
         */
    }

    @Test
    public void concat() {

        //{username}_{age} 형태로 내보내기
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }

        /**
         * 결과
         * s = member1_10
         */
        /**
         * stringValue()는 활용도가 크다.
         * 문자 아닌 다른 타입들은 이것으로 문자로 변환될 수 있는데, 이 방법은 ENUM을 처리할 때 자주 사용된다.
         */
    }

    /**
     * Projection
     *
     * 정의 : 프로젝션이란 select 대상을 지정하는 것을 의미한다.
     * 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
     */

    @Test
    @DisplayName("프로젝션 대상이 하나인 경우")
    public void simpleProjection () {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("프로젝션 대상이 여러개인 경우")
    public void tupleProjection () {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

}























