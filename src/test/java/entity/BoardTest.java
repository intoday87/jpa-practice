package entity;

import org.junit.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BoardTest {
    static EntityManagerFactory entityManagerFactory;
    EntityManager entityManager;
    EntityTransaction transaction;

    @BeforeClass
    public static void beforeClass() throws Exception {
        entityManagerFactory = Persistence.createEntityManagerFactory("jpa-practice");
    }

    @Before
    public void setUp() throws Exception {
        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();
    }

    @Test
    public void tableSequenceIsSumOfidCommited() throws Exception {
        try {
            transaction.begin();
            Board firstBoard = new Board();
            entityManager.persist(firstBoard);
            Board secondBoard = new Board();
            entityManager.persist(secondBoard);
            transaction.commit();

            MySequences mySequences = entityManager.find(MySequences.class, "BOARD_SEQ");

            assertThat(mySequences.getNextVal(), equalTo(firstBoard.getId() + secondBoard.getId()));
        } catch (Exception e) {
           transaction.rollback();
           fail(e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
        entityManager.close();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        entityManagerFactory.close();
    }
}
