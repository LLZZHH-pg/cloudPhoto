package com.lab.study.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.study.userservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 增加使用容量，附带并发校验限制
     * @return 返回 1 成功，0 失败（空间不足）
     */
    @Update("UPDATE user_info SET usedstorage = usedstorage + #{sizeDelta} " +
            "WHERE userid = #{userId} AND (usedstorage + #{sizeDelta}) <= totalstorage")
    int deductStorage(@Param("userId") Integer userId, @Param("sizeDelta") Long sizeDelta);

    /**
     * 释放使用容量，防止扣减为负数
     */
    @Update("UPDATE user_info SET usedstorage = CASE WHEN usedstorage - #{sizeDelta} < 0 THEN 0 ELSE usedstorage - #{sizeDelta} END " +
            "WHERE userid = #{userId}")
    int releaseStorage(@Param("userId") Integer userId, @Param("sizeDelta") Long sizeDelta);
}