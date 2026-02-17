package com.example.demo.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ================= RETAILER =================
    List<Order> findByRetailerId(Long retailerId);

    // ================= FARMER =================
    List<Order> findByProductFarmerId(Long farmerId);

    // ================= DELIVERY AGENT =================
    // ================= DELIVERY AGENT =================
    @Query("SELECT o FROM Order o WHERE o.deliveryAgent.id = :deliveryAgentId")
    List<Order> findByDeliveryAgentId(@Param("deliveryAgentId") Long deliveryAgentId);

    // ================= FARMER COUNTS =================
    @Query("""
                SELECT COUNT(o)
                FROM Order o
                JOIN o.product p
                WHERE p.farmer.id = :farmerId
            """)
    long countByFarmerId(@Param("farmerId") Long farmerId);

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                JOIN o.product p
                WHERE p.farmer.id = :farmerId
                AND o.status = :status
            """)
    long countByFarmerIdAndStatus(
            @Param("farmerId") Long farmerId,
            @Param("status") OrderStatus status);

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                JOIN o.product p
                WHERE p.farmer.id = :farmerId
                AND o.status IN :statuses
            """)
    long countByFarmerIdAndStatusIn(
            @Param("farmerId") Long farmerId,
            @Param("statuses") List<OrderStatus> statuses);

    // ✅ FIXED Farmer revenue:
    // use totalAmount if present else calculate from qty * price
    @Query("""
                SELECT COALESCE(SUM(
                    CASE
                        WHEN o.totalAmount IS NOT NULL AND o.totalAmount > 0 THEN o.totalAmount
                        ELSE o.quantity * p.price
                    END
                ), 0)
                FROM Order o
                JOIN o.product p
                WHERE p.farmer.id = :farmerId
                AND o.status = :status
            """)
    Double sumRevenueByFarmerId(
            @Param("farmerId") Long farmerId,
            @Param("status") OrderStatus status);

    // ================= ADMIN =================
    @Query("SELECT COUNT(o) FROM Order o")
    Long getTotalOrders();

    // ✅ FIXED Admin revenue:
    // if totalAmount null -> qty * price
    @Query("""
                SELECT COALESCE(SUM(
                    CASE
                        WHEN o.totalAmount IS NOT NULL AND o.totalAmount > 0 THEN o.totalAmount
                        ELSE o.quantity * p.price
                    END
                ), 0)
                FROM Order o
                JOIN o.product p
                WHERE o.status = :status
            """)
    Double getTotalRevenue(@Param("status") OrderStatus status);

    @Query("""
                SELECT COALESCE(SUM(
                    CASE
                        WHEN o.totalAmount IS NOT NULL AND o.totalAmount > 0 THEN o.totalAmount
                        ELSE o.quantity * p.price
                    END
                ) * 0.05, 0)
                FROM Order o
                JOIN o.product p
                WHERE o.status != 'CANCELLED'
            """)
    Double getTotalAdminRevenue();

    // ================= FARMER ANALYTICS =================
    @Query("""
                SELECT o
                FROM Order o
                JOIN o.product p
                WHERE p.farmer.id = :farmerId
                AND o.status = :status
            """)
    List<Order> findByProductFarmerIdAndStatus(
            @Param("farmerId") Long farmerId,
            @Param("status") OrderStatus status);

    // ================= MONTHLY TRANSACTION COUNTS (FARMER) =================
    @Query("""
                SELECT MONTH(o.orderDate) as month, COUNT(o) as count
                FROM Order o
                JOIN o.product p
                WHERE p.farmer.id = :farmerId
                AND o.status = :deliveredStatus
                AND YEAR(o.orderDate) = YEAR(CURRENT_DATE)
                GROUP BY MONTH(o.orderDate)
                ORDER BY MONTH(o.orderDate)
            """)
    List<Object[]> getMonthlyTransactionCounts(
            @Param("farmerId") Long farmerId,
            @Param("deliveredStatus") OrderStatus deliveredStatus);

    // ================= TOP SOLD PRODUCTS (FARMER) =================
    @Query("""
                SELECT p.name, SUM(o.quantity) as totalQuantity
                FROM Order o
                JOIN o.product p
                WHERE p.farmer.id = :farmerId
                AND o.status = :deliveredStatus
                GROUP BY p.name
                ORDER BY SUM(o.quantity) DESC
            """)
    List<Object[]> getTopSoldProducts(
            @Param("farmerId") Long farmerId,
            @Param("deliveredStatus") OrderStatus deliveredStatus,
            Pageable pageable);

    default List<Object[]> getTopSoldProductsTop5(Long farmerId, OrderStatus deliveredStatus) {
        return getTopSoldProducts(farmerId, deliveredStatus, PageRequest.of(0, 5));
    }

    // ================= RETAILER REVENUE =================
    // ✅ FIXED Retailer revenue:
    // if totalAmount null -> qty * price
    @Query("""
                SELECT COALESCE(SUM(
                    CASE
                        WHEN o.totalAmount IS NOT NULL AND o.totalAmount > 0 THEN o.totalAmount
                        ELSE o.quantity * p.price
                    END
                ), 0)
                FROM Order o
                JOIN o.product p
                WHERE o.retailer.id = :retailerId
                AND o.status = :status
            """)
    Double sumRevenueByRetailerId(
            @Param("retailerId") Long retailerId,
            @Param("status") OrderStatus status);

    // ================= ✅ RETAILER ANALYTICS SUPPORT =================

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                WHERE o.retailer.id = :retailerId
            """)
    long countByRetailerId(@Param("retailerId") Long retailerId);

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                WHERE o.retailer.id = :retailerId
                AND o.status = :status
            """)
    long countByRetailerIdAndStatus(
            @Param("retailerId") Long retailerId,
            @Param("status") OrderStatus status);

    @Query("""
                SELECT MONTH(o.orderDate) as month, COUNT(o) as count
                FROM Order o
                WHERE o.retailer.id = :retailerId
                AND o.status = :status
                AND YEAR(o.orderDate) = YEAR(CURRENT_DATE)
                GROUP BY MONTH(o.orderDate)
                ORDER BY MONTH(o.orderDate)
            """)
    List<Object[]> getMonthlyRetailerTransactions(
            @Param("retailerId") Long retailerId,
            @Param("status") OrderStatus status);

    // ================= TOP PURCHASED PRODUCTS =================
    @Query("""
                SELECT o.product.name, SUM(o.quantity)
                FROM Order o
                WHERE o.retailer.id = :retailerId
                AND o.status = :status
                GROUP BY o.product.name
                ORDER BY SUM(o.quantity) DESC
            """)
    List<Object[]> getTopPurchasedProducts(
            @Param("retailerId") Long retailerId,
            @Param("status") OrderStatus status);
}
