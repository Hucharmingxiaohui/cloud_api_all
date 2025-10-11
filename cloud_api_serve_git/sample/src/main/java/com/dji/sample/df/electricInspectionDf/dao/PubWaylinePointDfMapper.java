package com.dji.sample.df.electricInspectionDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

//巡检点位
public interface PubWaylinePointDfMapper extends BaseMapper<PubWaylinePointDfEntity> {
    String pointListSql = "<script> \n" +
            "<if test='deviceLevel != null and deviceLevel == 1'>\n" +
            "\tselect group_concat(point_code) from pub_wayline_point_df\n" +
            "\t<where>\n" +
            "\t\t<if test='list != null'>\n" +
            "\t\t\t<foreach collection=\"list\" index=\"index\" item=\"item\">\n" +
            "          or bay_id = #{item}\n" +
            "      </foreach>\n" +
            "\t\t</if>\n" +
            "\t</where>\n" +
            "</if>\n" +
            "<if test='deviceLevel != null and deviceLevel == 2'>\n" +
            "\tselect group_concat(point_code) from pub_wayline_point_df\n" +
            "\t<where>\n" +
            "\t\t<if test='list != null'>\n" +
            "\t\t\t<foreach collection=\"list\" index=\"index\" item=\"item\">\n" +
            "          or device_id = #{item}\n" +
            "      </foreach>\n" +
            "\t\t</if>\n" +
            "\t</where>\n" +
            "</if>\n" +
            "<if test='deviceLevel != null and deviceLevel == 4'>\n" +
            "\tselect group_concat(point_code) from pub_wayline_point_df\n" +
            "\t<where>\n" +
            "\t\t<if test='list != null'>\n" +
            "\t\t\t<foreach collection=\"list\" index=\"index\" item=\"item\">\n" +
            "          or component_id = #{item}\n" +
            "      </foreach>\n" +
            "\t\t</if>\n" +
            "\t</where>\n" +
            "</if> \n";

    String waylineListSql = "<script> \n" +
            "select group_concat(distinct wayline_id) from pub_wayline_point_df\n" +
            "\t<where>\n" +
            "\t\t<if test='list != null'>\n" +
            "\t\t\t<foreach collection=\"list\" index=\"index\" item=\"item\">\n" +
            "          or point_code = #{item}\n" +
            "      </foreach>\n" +
            "\t\t</if>\n" +
            "\t</where> \n";

    List<PubWaylinePointDfEntity> selectBySubCode(String subCode);

    @Select(pointListSql + "  </script>")
    String selectPoints(@Param("deviceLevel") int device_level, @Param("list") List<String> device_list);

    @Select(waylineListSql + "  </script>")
    String selectWaylines(@Param("list") List<String> list);

    List<PubWaylinePointDfEntity> getPointsByParam(Map map);

    int getPointsByParamCount(Map map);
}
