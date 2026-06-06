package com.lab.study.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.study.userservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**     * 增加使用容量，涉及跨表校验总容量     * 逻辑：如果 plan 状态为 'enable'，则使用 p.storage；否则使用默认值 1073741824     */
    @Update("UPDATE user_info u " +
            "LEFT JOIN user_plan up ON u.userid = up.userid " +
            "LEFT JOIN plan_info p ON up.planid = p.planid " +
            "SET u.usedstorage = u.usedstorage + #{sizeDelta} " +
            "WHERE u.userid = #{userId} " +
            "AND (u.usedstorage + #{sizeDelta}) <= IF(p.statues = 'enable', p.storage, 1073741824)")
    int deductStorage(@Param("userId") Integer userId, @Param("sizeDelta") Long sizeDelta);

    /**     * 释放容量（保持不变，仅需确保不减为负数）     */
    @Update("UPDATE user_info SET usedstorage = CASE WHEN usedstorage - #{sizeDelta} < 0 THEN 0 ELSE usedstorage - #{sizeDelta} END " +
            "WHERE userid = #{userId}")
    int releaseStorage(@Param("userId") Integer userId, @Param("sizeDelta") Long sizeDelta);

    /**     * 查询用户当前的剩余容量     * 逻辑：剩余容量 = (生效计划容量 OR 默认容量) - 已采集容量     */
    @Select("SELECT IF(p.statues = 'enable', p.storage, 1073741824) as total " +
            "FROM user_info u " +
            "LEFT JOIN user_plan up ON u.userid = up.userid " +
            "LEFT JOIN plan_info p ON up.planid = p.planid " +
            "WHERE u.userid = #{userId}")
    Long getTotalStorage(@Param("userId") Integer userId);

    /**     * 查询回收站有效期（天）     * 逻辑：如果 plan 状态为 'enable'，使用 p.recycle；否则使用默认值 30     */
    @Select("SELECT IF(p.statues = 'enable', p.recycle, 30) " +
            "FROM user_info u " +
            "LEFT JOIN user_plan up ON u.userid = up.userid " +
            "LEFT JOIN plan_info p ON up.planid = p.planid " +
            "WHERE u.userid = #{userId}")
    Integer getRecycleDays(@Param("userId") Integer userId);
}