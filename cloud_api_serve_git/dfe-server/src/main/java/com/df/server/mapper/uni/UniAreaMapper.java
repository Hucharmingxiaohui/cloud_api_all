package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniAreaEntity;
import com.df.server.vo.UniArea.UniAreaVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 区域接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@Mapper
public interface UniAreaMapper extends BaseMapper<UniAreaEntity> {

    List<UniAreaVO> listBySubCode(String subCode);

}