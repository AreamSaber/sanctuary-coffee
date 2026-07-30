package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 原子扣减优惠券可领取库存，避免并发领取时超发。
     *
     * @param couponId 优惠券 ID
     * @return 影响行数，1 表示领取名额占用成功，0 表示已领完或优惠券不存在
     */
    @Update("""
        UPDATE coupon
        SET received_quantity = COALESCE(received_quantity, 0) + 1
        WHERE id = #{couponId}
          AND deleted = 0
          AND status = 1
          AND COALESCE(received_quantity, 0) < COALESCE(total_quantity, 0)
        """)
    int increaseReceivedQuantityIfAvailable(@Param("couponId") Long couponId);
}
