package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.PurchaseRequestDao;
import com.minhhai.wms.dao.mapper.PurchaseRequestRowMapper;
import com.minhhai.wms.entity.PurchaseRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PurchaseRequestDaoImpl implements PurchaseRequestDao {

    private final JdbcTemplate jdbcTemplate;
    private final PurchaseRequestRowMapper rowMapper = new PurchaseRequestRowMapper();

    public PurchaseRequestDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PurchaseRequest> findByPurchaseOrderId(Integer poId) {
        return jdbcTemplate.query(
                "SELECT PRID, PRNumber, WarehouseID, Status, RelatedSOID, POID FROM PurchaseRequests WHERE POID = ?",
                rowMapper,
                poId
        );
    }

    @Override
    public List<PurchaseRequest> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(
                "SELECT PRID, PRNumber, WarehouseID, Status, RelatedSOID, POID FROM PurchaseRequests WHERE WarehouseID = ?",
                rowMapper,
                warehouseId
        );
    }

    @Override
    public List<PurchaseRequest> findByWarehouseIdAndStatus(Integer warehouseId, String status) {
        return jdbcTemplate.query(
                "SELECT PRID, PRNumber, WarehouseID, Status, RelatedSOID, POID FROM PurchaseRequests WHERE WarehouseID = ? AND Status = ?",
                rowMapper,
                warehouseId,
                status
        );
    }

    @Override
    public Optional<PurchaseRequest> findById(Integer prId) {
        List<PurchaseRequest> list = jdbcTemplate.query(
                "SELECT PRID, PRNumber, WarehouseID, Status, RelatedSOID, POID FROM PurchaseRequests WHERE PRID = ?",
                rowMapper,
                prId
        );
        return list.stream().findFirst();
    }

    @Override
    public Optional<PurchaseRequest> findByRelatedSalesOrderId(Integer soId) {
        List<PurchaseRequest> list = jdbcTemplate.query(
                "SELECT PRID, PRNumber, WarehouseID, Status, RelatedSOID, POID FROM PurchaseRequests WHERE RelatedSOID = ?",
                rowMapper,
                soId
        );
        return list.stream().findFirst();
    }

    @Override
    public List<PurchaseRequest> findByRelatedSalesOrderIdAndStatusIn(Integer soId, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return new ArrayList<>();
        }
        String placeholders = String.join(",", statuses.stream().map(s -> "?").toList());
        String sql = "SELECT PRID, PRNumber, WarehouseID, Status, RelatedSOID, POID FROM PurchaseRequests " +
                "WHERE RelatedSOID = ? AND Status IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>();
        params.add(soId);
        params.addAll(statuses);
        return jdbcTemplate.query(sql, rowMapper, params.toArray());
    }

    @Override
    public PurchaseRequest save(PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getPrId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO PurchaseRequests (PRNumber, WarehouseID, Status, RelatedSOID, POID) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, purchaseRequest.getPrNumber());
                ps.setInt(2, purchaseRequest.getWarehouse().getWarehouseId());
                ps.setString(3, purchaseRequest.getStatus());
                if (purchaseRequest.getRelatedSalesOrder() != null) {
                    ps.setInt(4, purchaseRequest.getRelatedSalesOrder().getSoId());
                } else {
                    ps.setObject(4, null);
                }
                if (purchaseRequest.getPurchaseOrder() != null) {
                    ps.setInt(5, purchaseRequest.getPurchaseOrder().getPoId());
                } else {
                    ps.setObject(5, null);
                }
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                purchaseRequest.setPrId(keyHolder.getKey().intValue());
            }
            return purchaseRequest;
        }
        jdbcTemplate.update(
                "UPDATE PurchaseRequests SET WarehouseID = ?, Status = ?, RelatedSOID = ?, POID = ? WHERE PRID = ?",
                purchaseRequest.getWarehouse().getWarehouseId(),
                purchaseRequest.getStatus(),
                purchaseRequest.getRelatedSalesOrder() != null ? purchaseRequest.getRelatedSalesOrder().getSoId() : null,
                purchaseRequest.getPurchaseOrder() != null ? purchaseRequest.getPurchaseOrder().getPoId() : null,
                purchaseRequest.getPrId()
        );
        return purchaseRequest;
    }

    @Override
    public void deleteById(Integer prId) {
        jdbcTemplate.update("DELETE FROM PurchaseRequests WHERE PRID = ?", prId);
    }

    @Override
    public String findMaxPrNumber(String prefix) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(PRNumber) FROM PurchaseRequests WHERE PRNumber LIKE ?",
                String.class,
                prefix + "%"
        );
    }
}
