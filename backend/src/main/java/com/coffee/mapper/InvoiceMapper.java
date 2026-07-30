package com.coffee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coffee.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票Mapper
 * 
 * @author Coffee Shop Team
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
