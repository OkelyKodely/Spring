package org.o7planning.springmvcshoppingcart.dao.impl;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
 
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.o7planning.springmvcshoppingcart.dao.OrderDAO;
import org.o7planning.springmvcshoppingcart.dao.ProductDAO;
import org.o7planning.springmvcshoppingcart.entity.Order;
import org.o7planning.springmvcshoppingcart.entity.OrderDetail;
import org.o7planning.springmvcshoppingcart.entity.Product;
import org.o7planning.springmvcshoppingcart.model.CartInfo;
import org.o7planning.springmvcshoppingcart.model.CartLineInfo;
import org.o7planning.springmvcshoppingcart.model.CustomerInfo;
import org.o7planning.springmvcshoppingcart.model.OrderDetailInfo;
import org.o7planning.springmvcshoppingcart.model.OrderInfo;
import org.o7planning.springmvcshoppingcart.model.PaginationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
 
//Transactional for Hibernate
@Transactional
public class OrderDAOImpl implements OrderDAO {
 
    @Autowired
    private SessionFactory sessionFactory;
 
    @Autowired
    private ProductDAO productDAO;
 
    private int getMaxOrderNum() {
        String sql = "Select max(o.orderNum) from " + Order.class.getName() + " o ";
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery(sql);
        Integer value = (Integer) query.uniqueResult();
        if (value == null) {
            return 0;
        }
        return value;
    }
 
    public void saveOrder(CartInfo cartInfo) {
        Session session = sessionFactory.getCurrentSession();
 
        int orderNum = this.getMaxOrderNum() + 1;
        Order order = new Order();
 
        order.setId(UUID.randomUUID().toString());
        order.setOrderNum(orderNum);
        order.setOrderDate(new Date());
        order.setAmount(cartInfo.getAmountTotal());
 
        CustomerInfo customerInfo = cartInfo.getCustomerInfo();
        order.setCustomerName(customerInfo.getName());
        order.setCustomerEmail(customerInfo.getEmail());
        order.setCustomerPhone(customerInfo.getPhone());
        order.setCustomerAddress(customerInfo.getAddress());
        order.setCardnum(customerInfo.getCardnum());
        order.setCardname(customerInfo.getCardname());
        order.setExpire(customerInfo.getExpire());
        order.setCode(customerInfo.getCode());
        
///        System.out.println("=>"+order.getCode());
		String hostName = "localhost:3320";
		String dbName = "mydb";
		String userName = "root";
		String passWord = "";

		Connection conn = null;

		PreparedStatement st1=null ;

		try {

    		try {
    		    Class.forName("com.mysql.jdbc.Driver");
    		    String url = "jdbc:mysql://" + hostName + "/" + dbName;
    		    conn = DriverManager.getConnection(url, userName, passWord);
    		} catch(Exception e) {
    			e.printStackTrace();
    		}

    		st1 = conn.prepareStatement(
"insert into orders (id, order_num, order_date, amount, customer_name, customer_email, customer_phone, "
+ "customer_address, cardnum, cardname, expire, code) values (?,?,now(),?,?,?,?,?,?,?,?,?)");

    		st1.setString(1, order.getId()+"");
    		st1.setString(2, order.getOrderNum()+"");
    		st1.setString(3, order.getAmount()+"");
    		st1.setString(4, order.getCustomerName()+"");
    		st1.setString(5, order.getCustomerEmail()+"");
    		st1.setString(6, order.getCustomerPhone()+"");
    		st1.setString(7, order.getCustomerAddress()+"");
    		st1.setString(8, order.getCardnum()+"");
    		st1.setString(9, order.getCardname()+"");
    		st1.setString(10, order.getExpire()+"");
    		st1.setString(11, order.getCode()+"");
    		
    		st1.execute();
    		
    		st1.close();
    		conn.close();

		} catch(SQLException sqle) {
			sqle.printStackTrace();
		}
        
        // session.persist(order);
 
        List<CartLineInfo> lines = cartInfo.getCartLines();
 
        for (CartLineInfo line : lines) {
            OrderDetail detail = new OrderDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setOrder(order);
            detail.setAmount(line.getAmount());
            detail.setPrice(line.getProductInfo().getPrice());
            detail.setQuanity(line.getQuantity());
 
            String code = line.getProductInfo().getCode();
            Product product = this.productDAO.findProduct(code);
            detail.setProduct(product);
 
            session.persist(detail);
        }
 
        // Set OrderNum for report.
        // Set OrderNum d? thông báo cho ngu?i dùng.
        cartInfo.setOrderNum(orderNum);
    }
 
    // @page = 1, 2, ...
    public PaginationResult<OrderInfo> listOrderInfo(int page, int maxResult, int maxNavigationPage) {
        String sql = "Select new " + OrderInfo.class.getName()//
                + "(ord.id, ord.orderDate, ord.orderNum, ord.amount, "
                + " ord.customerName, ord.customerAddress, ord.customerEmail, ord.customerPhone, ord.cardnum, ord.cardname, ord.expire, ord.code) " + " from "
                + Order.class.getName() + " ord "//
                + " order by ord.orderNum desc";
        Session session = this.sessionFactory.getCurrentSession();
 
        Query query = session.createQuery(sql);
 
        return new PaginationResult<OrderInfo>(query, page, maxResult, maxNavigationPage);
    }
 
    public Order findOrder(String orderId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria crit = session.createCriteria(Order.class);
        crit.add(Restrictions.eq("id", orderId));
        return (Order) crit.uniqueResult();
    }
 
    public OrderInfo getOrderInfo(String orderId) {
        Order order = this.findOrder(orderId);
        if (order == null) {
            return null;
        }
        return new OrderInfo(order.getId(), order.getOrderDate(), //
                order.getOrderNum(), order.getAmount(), order.getCustomerName(), //
                order.getCustomerAddress(), order.getCustomerEmail(), order.getCustomerPhone(), //
                order.getCardnum(), order.getCardname(), order.getExpire(), order.getCode());
    }
 
    public List<OrderDetailInfo> listOrderDetailInfos(String orderId) {
        String sql = "Select new " + OrderDetailInfo.class.getName() //
                + "(d.id, d.product.code, d.product.name , d.quanity,d.price,d.amount) "//
                + " from " + OrderDetail.class.getName() + " d "//
                + " where d.order.id = :orderId ";
 
        Session session = this.sessionFactory.getCurrentSession();
 
        Query query = session.createQuery(sql);
        query.setParameter("orderId", orderId);
 
        return query.list();
    }
 
}