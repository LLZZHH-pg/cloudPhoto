package com.lab.study.photomanageservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.study.photomanageservice.entity.Picture;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PictureMapper extends BaseMapper<Picture> {

    @Select("SELECT * FROM ( " +
            "  SELECT *, ROW_NUMBER() OVER(PARTITION BY category ORDER BY shot_time DESC) as rn " +
            "  FROM picture_info " +
            "  WHERE userid = #{userId} AND delete_time IS NULL " +
            ") t WHERE rn = 1")
    List<Picture> selectFirstPicturePerCategory(@Param("userId") Integer userId);
}
