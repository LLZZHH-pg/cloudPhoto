package com.lab.study.albummanageservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.study.albummanageservice.entity.AlbumPhoto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 影集-照片关联 Mapper
 */
@Mapper
public interface AlbumPhotoMapper extends BaseMapper<AlbumPhoto> {

    /**
     * 查询影集内所有未删除照片ID
     */
    @Select("SELECT photo_id FROM album_photo WHERE album_id = #{albumId} AND is_deleted = 0")
    List<Long> selectPhotoIdsByAlbumId(Long albumId);

}
