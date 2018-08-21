package entity;

import org.junit.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class OrderTest {
    static EntityManagerFactory emf;
    EntityManager entityManager;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        emf = Persistence.createEntityManagerFactory("jpa-practice");
    }

    @AfterClass
    public static void tearDownAfterClass() {
        emf.close();
    }

    @Before
    public void setUp() throws Exception {
        entityManager = emf.createEntityManager();
    }

    @Test
    public void autoIncrementIdAndCascadePersist() {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        Order order = new Order();
        OrderProduct orderProduct = new OrderProduct();
        order.setOrderProduct(orderProduct);
        entityManager.persist(order);
        transaction.commit();

        assertNotNull(order.getId());

        Order found = entityManager.find(Order.class, order.getId());
        assertThat(found.getId(), is(order.getId()));
        assertThat(found.getOrderProduct().getId(), is(order.getOrderProduct().getId()));
    }

    @After
    public void tearDown() throws Exception {
        entityManager.close();
    }
}
