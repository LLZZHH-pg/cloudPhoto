package com.lab.study.albummanageservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.study.albummanageservice.entity.Album;
import org.apache.ibatis.annotations.Mapper;

/**
 * 影集 Mapper
 * MyBatis Plus 已提供基础 CRUD，复杂查询在 XML 中编写
 */
@Mapper
public interface AlbumMapper extends BaseMapper<Album> {
}
