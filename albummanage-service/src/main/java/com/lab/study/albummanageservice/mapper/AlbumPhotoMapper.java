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
     * 查询影集内所有照片ID
     */
    @Select("SELECT photo_id FROM album_photo WHERE album_id = #{albumId}")
    List<Long> selectPhotoIdsByAlbumId(Long albumId);

    /**
     * 查询照片所属的影集ID列表
     */
    @Select("SELECT album_id FROM album_photo WHERE photo_id = #{photoId}")
    List<Long> selectAlbumIdsByPhotoId(Long photoId);
}
