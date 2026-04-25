package com.lab.study.photomanageservice.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface PictureService {

    /**
     * 分页获取时间线图片（按拍摄日期分组）
     * @param page 当前页码
     * @param size 每页分组数量（如每天一组）
     * @return 分组时间线数据，结构示例：
     * {
     *   "records": [
     *     {
     *       "date": "2026-04-20",
     *       "pictures": [
     *         {
     *           "pictureId": 1,
     *           "thumbnailUrl": "https://qiniu.xxx/thumbnail/abc.jpg",
     *           "shotTime": "2026-04-20T10:30:00"
     *         }
     *       ]
     *     }
     *   ],
     *   "total": 100,
     *   "current": 1,
     *   "size": 20
     * }
     */
    Object getTimeline(int page, int size);

    /**
     * 获取单张照片完整信息（EXIF + 高清预览图）
     * @param pictureId 图片ID
     * @return 图片详情，结构示例：
     * {
     *   "pictureId": 1,
     *   "userId": 101,
     *   "fileHash": "e99a18c...",
     *   "fileName": "DSC001.jpg",
     *   "fileSize": 2048000,
     *   "shotTime": "2026-04-20T10:30:00",
     *   "deleteTime": null,
     *   "fileExif": { "Make": "Canon", "Model": "EOS 5D", ... },
     *   "previewUrl": "https://qiniu.xxx/highres/abc.jpg"
     * }
     */
    Object getDetail(Integer pictureId);

    /**
     * 批量上传图片
     * 1. 校验文件 hash 是否已存在
     * 2. 解析 EXIF 信息
     * 3. 上传至七牛云并获取 URL
     * 4. 数据库写入记录
     * 5. 删除服务器本地临时文件
     *
     * @param files  上传的文件列表
     * @param userId 用户ID（可从当前登录上下文获取）
     * @return 上传结果列表，每个元素示例：
     * {
     *   "fileName": "DSC001.jpg",
     *   "success": true,
     *   "pictureId": 123,
     *   "message": "上传成功"
     * }
     */
    Object upload(List<MultipartFile> files, Integer userId);

    /**
     * 批量删除（移入回收站）
     * 将指定图片的 delete_time 设置为当前时间 + 30 天
     * @param pictureIds 要删除的图片ID列表
     */
    void deletePictures(List<Integer> pictureIds);

    /**
     * 获取回收站列表（delete_time 不为 null 的图片）
     * @param page 当前页码
     * @param size 每页条数
     * @return 回收站图片分页数据，结构与时间线类似（不带分组）
     */
    Object getTrashList(int page, int size);

    /**
     * 批量恢复回收站图片
     * 将指定图片的 delete_time 重新置为 null
     * @param pictureIds 要恢复的图片ID列表
     */
    void restorePictures(List<Integer> pictureIds);

    /**
     * 彻底删除
     * 1. 删除数据库记录
     * 2. 同步删除七牛云上的文件
     * @param pictureIds 要彻底删除的图片ID列表
     */
    void cleanTrash(List<Integer> pictureIds);
}
