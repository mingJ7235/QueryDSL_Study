package me.joshua.querydsl_study;

import com.querydsl.jpa.impl.JPAQueryFactory;
import me.joshua.querydsl_study.entity.Hello;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static me.joshua.querydsl_study.entity.QHello.hello;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Commit
class ApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello2 = new Hello();
        em.persist(hello2);

        JPAQueryFactory query = new JPAQueryFactory(em);

        Hello result = query
                .selectFrom(hello)
                .fetchOne();

        assertThat(result).isEqualTo(hello2);
        assertThat(result.getId()).isEqualTo(hello2.getId());
    }

}
