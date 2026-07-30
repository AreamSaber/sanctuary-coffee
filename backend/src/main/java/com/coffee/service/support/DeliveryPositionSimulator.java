package com.coffee.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.entity.*;
import com.coffee.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryPositionSimulator {

    private final DeliveryOrderMapper deliveryOrderMapper;
    private final DeliveryStaffMapper staffMapper;
    private final DeliveryRegionMapper regionMapper;
    private final OrderMapper orderMapper;

    public Map<String, Object> getCurrentPosition(Long orderId) {
        DeliveryOrder deliveryOrder = deliveryOrderMapper.selectOne(
            new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getOrderId, orderId)
                .last("LIMIT 1")
        );
        if (deliveryOrder == null) {
            return Map.of("status", "NO_DELIVERY");
        }

        BigDecimal[] pickup = resolvePickupCoord(deliveryOrder);
        BigDecimal[] dest = resolveDestCoord(deliveryOrder);

        Map<String, Object> result = new HashMap<>();
        result.put("deliveryNo", deliveryOrder.getDeliveryNo());
        result.put("deliveryStatus", deliveryOrder.getDeliveryStatus());

        Integer status = deliveryOrder.getDeliveryStatus();
        if (status == null) {
            result.put("longitude", pickup[0]);
            result.put("latitude", pickup[1]);
            return result;
        }

        if (status == 1 || status == 2) {
            result.put("longitude", pickup[0]);
            result.put("latitude", pickup[1]);
            result.put("label", "等待取货");
        } else if (status == 3 && deliveryOrder.getPickupTime() != null) {
            double progress = calcProgress(deliveryOrder.getPickupTime(), 30);
            result.put("longitude", interpolate(pickup[0], dest[0], progress));
            result.put("latitude", interpolate(pickup[1], dest[1], progress));
            result.put("label", "配送中 " + (int)(progress * 100) + "%");
        } else if (status == 4) {
            result.put("longitude", dest[0]);
            result.put("latitude", dest[1]);
            result.put("label", "已送达");
        } else {
            result.put("longitude", pickup[0]);
            result.put("latitude", pickup[1]);
        }

        result.put("pickupLongitude", pickup[0]);
        result.put("pickupLatitude", pickup[1]);
        result.put("destLongitude", dest[0]);
        result.put("destLatitude", dest[1]);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    private BigDecimal[] resolvePickupCoord(DeliveryOrder deliveryOrder) {
        if (deliveryOrder.getDeliverymanId() != null) {
            DeliveryStaff staff = staffMapper.selectById(deliveryOrder.getDeliverymanId());
            if (staff != null && staff.getRegionId() != null) {
                DeliveryRegion region = regionMapper.selectById(staff.getRegionId());
                if (region != null && region.getLongitude() != null && region.getLatitude() != null) {
                    return new BigDecimal[]{region.getLongitude(), region.getLatitude()};
                }
            }
        }
        return new BigDecimal[]{new BigDecimal("116.397128"), new BigDecimal("39.916527")};
    }

    private BigDecimal[] resolveDestCoord(DeliveryOrder deliveryOrder) {
        Order order = orderMapper.selectById(deliveryOrder.getOrderId());
        if (order != null && order.getReceiverAddress() != null) {
            return jitterDest(pickupCoord(order.getReceiverAddress()));
        }
        return new BigDecimal[]{new BigDecimal("116.407526"), new BigDecimal("39.904989")};
    }

    private BigDecimal[] pickupCoord(String address) {
        if (address.contains("海淀")) return new BigDecimal[]{new BigDecimal("116.298056"), new BigDecimal("39.959988")};
        if (address.contains("西城")) return new BigDecimal[]{new BigDecimal("116.365868"), new BigDecimal("39.912289")};
        if (address.contains("浦东")) return new BigDecimal[]{new BigDecimal("121.543099"), new BigDecimal("31.231416")};
        if (address.contains("上海")) return new BigDecimal[]{new BigDecimal("121.473701"), new BigDecimal("31.230416")};
        return new BigDecimal[]{new BigDecimal("116.397128"), new BigDecimal("39.916527")};
    }

    private BigDecimal[] jitterDest(BigDecimal[] base) {
        double lngJitter = (Math.random() - 0.5) * 0.02;
        double latJitter = (Math.random() - 0.5) * 0.02;
        return new BigDecimal[]{
            base[0].add(BigDecimal.valueOf(lngJitter)).setScale(6, RoundingMode.HALF_UP),
            base[1].add(BigDecimal.valueOf(latJitter)).setScale(6, RoundingMode.HALF_UP)
        };
    }

    private double calcProgress(LocalDateTime startTime, int estimatedMinutes) {
        long elapsed = Duration.between(startTime, LocalDateTime.now()).getSeconds();
        long total = estimatedMinutes * 60L;
        return Math.min(1.0, Math.max(0.0, (double) elapsed / total));
    }

    private BigDecimal interpolate(BigDecimal from, BigDecimal to, double progress) {
        double f = from.doubleValue();
        double t = to.doubleValue();
        return BigDecimal.valueOf(f + (t - f) * progress).setScale(6, RoundingMode.HALF_UP);
    }
}
