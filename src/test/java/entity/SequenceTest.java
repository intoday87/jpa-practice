package entity;

import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SequenceTest {

    EntityManagerFactory entityManagerFactory;
    EntityManager entityManager;
    EntityTransaction transaction;

    @Before
    public void setUp() throws Exception {
        entityManagerFactory = Persistence.createEntityManagerFactory("jpa-practice");
        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();
    }

    @Test
    public void generateSequence() throws Exception {
        try {
            transaction.begin();
            entityManager.persist(new Sequence());
            entityManager.persist(new Sequence());
            entityManager.persist(new Sequence());
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            fail();
        }

        try {
            assertNotNull(entityManager.find(Sequence.class, 1L));
            assertNotNull(entityManager.find(Sequence.class, 2L));
            assertNotNull(entityManager.find(Sequence.class, 3L));
        } catch (Exception e) {
            transaction.rollback();
            fail();
        }
    }
}
