package com.xh.system.service;

import com.xh.common.core.service.BaseServiceImpl;
import com.xh.common.core.service.CommonService;
import com.xh.common.core.utils.CommonUtil;
import com.xh.common.core.web.MyException;
import com.xh.common.core.web.PageQuery;
import com.xh.common.core.web.PageResult;
import com.xh.system.client.dto.DownloadFileDTO;
import com.xh.system.client.entity.SysFile;
import io.minio.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
//import org.bytedeco.javacv.FFmpegFrameGrabber;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件操作service
 * sunxh 2023/4/22
 */
@Service
@Slf4j
public class SysFileService extends BaseServiceImpl {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    @Value("${minio.bucket}")
    private String bucket;

    @Resource
    private CommonService commonService;
    @Resource
    private PlatformTransactionManager platformTransactionManager;
    @Resource
    private TransactionDefinition transactionDefinition;

    /**
     * 上传单个文件
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SysFile uploadFile(MultipartFile multipartFile) {
        try {
            //获取文件摘要sha1
            String sha1 = CommonUtil.getFileSha1(multipartFile.getInputStream());
            String sql2 = "delete from sys_file where sha1 = ? and status = 4";
            primaryJdbcTemplate.update(sql2, sha1);
            String sql = "select * from sys_file where sha1 = ? and deleted is false and status != 4";
            //利用sha1判断文件是否一致，sha1查询文件是否已上传过，已上传则节省空间直接返回sysFile对象
            SysFile sysFile = baseJdbcDao.findBySql(SysFile.class, sql, sha1);
            if (sysFile != null) return sysFile;
            sysFile = new SysFile();
            MinioClient minioClient = getMinioClient();
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            String filename = multipartFile.getOriginalFilename();
            DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String date = yyyyMMdd.format(LocalDateTime.now());
            //文件后缀名
            String object = "%s/%s".formatted(date, uuid);
            String suffix = CommonUtil.getFileSuffix(filename);
            if (CommonUtil.isNotEmpty(suffix)) object += "." + suffix;
            sysFile.setObject(object);
            sysFile.setSize(multipartFile.getSize());
            sysFile.setSuffix(suffix);
            sysFile.setContentType(multipartFile.getContentType());
            sysFile.setName(filename);
            // 判断文件是否为图片文件，图片文件获取宽高，横纵比
            if (sysFile.getContentType().startsWith("image/")) {
                try (InputStream inputStream = multipartFile.getInputStream()) {

                    BufferedImage image = ImageIO.read(inputStream);
                    sysFile.setImgWidth(image.getWidth());
                    sysFile.setImgHeight(image.getHeight());
                    sysFile.setImgRatio(image.getWidth() / new BigDecimal(image.getHeight()).doubleValue());
                } catch (Exception e) {
                    log.info("非图片文件");
                }
            }

            //如果是视频文件，抽第十帧为视频缩略图，长边压缩至40像素
            if (sysFile.getContentType().startsWith("video/")) {
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    BufferedImage videoFrameImage = getVideoFrameImage(multipartFile.getInputStream(), 10);
                    //压缩
                    videoFrameImage = scaleImage(videoFrameImage, 200);

                    ImageIO.write(videoFrameImage, "jpg", stream);
                    try (InputStream inputStream = new ByteArrayInputStream(stream.toByteArray())) {
                        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                                "视频预览图片",
                                "视频预览图片.jpg",
                                "image/jpeg",
                                inputStream
                        );
                        //上传预览图片
                        SysFile previewFile = uploadFile(mockMultipartFile);
                        sysFile.setPreviewImageFileId(previewFile.getId());
                    }
                } catch (Exception e) {
                    log.error("抽帧失败", e);
                }
            }
            sysFile.setStatus(1);
            sysFile.setSha1(sha1);
            baseJdbcDao.insert(sysFile);

            try (InputStream inputStream = multipartFile.getInputStream()) {
                //上传至文件存储服务器
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(object)
                                .contentType(multipartFile.getContentType())
                                .stream(inputStream, multipartFile.getSize(), -1)
                                .build()
                );
            } catch (Exception e) {
                throw new MyException("文件上传失败：%s".formatted(e.getMessage()));
            }
            return sysFile;
        } catch (MyException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件下载，支持断点续传，304协商缓存，图片压缩缩略图，视频文件抽帧
     */
    @Transactional
    public void downloadFile(DownloadFileDTO downloadFileDTO, String range, HttpServletRequest request, HttpServletResponse response) {
        long startByte = 0L;
        Long endByte = null;
        boolean hasRange = range != null && range.contains("bytes=") && range.contains("-");
        if (hasRange) {
            String rangeFormat = range.substring(range.lastIndexOf("=") + 1).trim();
            String[] ranges = rangeFormat.split("-");
            try {
                if (rangeFormat.startsWith("-")) {
                    endByte = Long.parseLong(ranges[1]);
                } else if (rangeFormat.endsWith("-")) {
                    startByte = Long.parseLong(ranges[0]);
                } else if (ranges.length == 2) {
                    startByte = Long.parseLong(ranges[0]);
                    endByte = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                log.error("download 错误", e);
            }
        }

        MinioClient minioClient = getMinioClient();
        if (CommonUtil.isEmpty(downloadFileDTO.getObject())) {
            if (downloadFileDTO.getId() == null) throw new MyException("参数异常");
            SysFile sysFile = baseJdbcDao.findById(SysFile.class, downloadFileDTO.getId());
            downloadFileDTO.setObject(sysFile.getObject());
            downloadFileDTO.setContentType(sysFile.getContentType());
            if (CommonUtil.isEmpty(downloadFileDTO.getFileName())) {
                downloadFileDTO.setFileName(sysFile.getName());
            }
        }

        // 获取对象的元数据信息
        StatObjectResponse statObjectResponse;
        try {
            statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(downloadFileDTO.getObject())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //协商缓存，命中则返回304状态码，浏览器会从缓存中获取数据节省流量
        if (!downloadFileDTO.getNoCache()) {
            // 文件最后修改时间
            long lastModified = statObjectResponse.lastModified().toInstant().toEpochMilli();

            // 设置缓存相关的HTTP头
            response.setDateHeader("Last-Modified", lastModified);
            response.setHeader("Cache-Control", "public, max-age=3600");
            response.setHeader("ETag", "\"" + lastModified + "\"");

            // 检查缓存
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            String ifNoneMatch = request.getHeader("If-None-Match");
            request.getHeaderNames();
            if (ifModifiedSince != -1 && ifNoneMatch != null && ifNoneMatch.equals("\"" + lastModified + "\"")) {
                // 如果请求的缓存版本与服务器上的相同，则返回304状态码
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        }

        GetObjectArgs.Builder builder = GetObjectArgs.builder()
                .bucket(bucket)
                .object(downloadFileDTO.getObject())
                .offset(startByte);
        if (endByte != null) {
            builder.length(endByte - startByte + 1);
        }
        try (
                GetObjectResponse objectResponse = minioClient.getObject(builder.build());
                OutputStream outputStream = response.getOutputStream()
        ) {
            //如果图片预览缩略图，svg无须缩略
            if (downloadFileDTO.getIsScale() && statObjectResponse.contentType().startsWith("image/") && !downloadFileDTO.getObject().endsWith(".svg")) {
                try {
                    //缩略图长边大小
                    double scaleWidth = downloadFileDTO.getScaleWidth();
                    BufferedImage newBI = scaleImage(ImageIO.read(objectResponse), scaleWidth);
                    ImageIO.write(newBI, "webp", outputStream);
                    return;
                } catch (Exception e) {
                    log.error("图片缩放失败，{}", e.getMessage());
                }
            }
            //视频文件允许抽帧图片输出
            else if (downloadFileDTO.getVideoFrameNum() != null && statObjectResponse.contentType().startsWith("video/")) {
                BufferedImage videoFrameImage = getVideoFrameImage(objectResponse, downloadFileDTO.getVideoFrameNum());
                ImageIO.write(videoFrameImage, "webp", outputStream);
                return;
            } else {
                if (hasRange) {
                    Headers headers = objectResponse.headers();
                    for (String name : headers.names()) {
                        response.setHeader(name, headers.get(name));
                    }
                    response.setHeader("Server", "XH-Admin-File");
                    //设置为206响应码，表示服务器支持断点下载
                    response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                }
                //下载时表示总文件大小
                if (downloadFileDTO.getDisposition().equals("attachment")) {
                    response.setHeader("Content-Length", String.valueOf(statObjectResponse.size()));
                }
                response.setCharacterEncoding("UTF-8");
                if (CommonUtil.isNotEmpty(downloadFileDTO.getFileName())) {
                    String fileName = URLEncoder.encode(downloadFileDTO.getFileName(), StandardCharsets.UTF_8);
                    //设置响应头中文件的下载方式为附件方式，以及设置文件名
                    response.setHeader("Content-Disposition",
                            "%s; filename=%s".formatted(downloadFileDTO.getDisposition(), fileName));
                }
                if (CommonUtil.isNotEmpty(downloadFileDTO.getContentType())) {
                    //设置Content-Type，这样可以使用浏览器原生的文件预览功能
                    response.setContentType(downloadFileDTO.getContentType());
                }
            }
            objectResponse.transferTo(outputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 系统文件查询
     */
    @Transactional(readOnly = true)
    public PageResult<SysFile> query(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> param = pageQuery.getParam();
        String sql = "select * from sys_file where status <> 4 ";
        if (CommonUtil.isNotEmpty(param.get("object"))) {
            sql += " and object like '%' ? '%'";
            pageQuery.addArg(param.get("object"));
        }
        if (CommonUtil.isNotEmpty(param.get("name"))) {
            sql += " and name like '%' ? '%'";
            pageQuery.addArg(param.get("name"));
        }
        if (CommonUtil.isNotEmpty(param.get("contentType"))) {
            sql += " and content_type like  '%' ? '%'";
            pageQuery.addArg(param.get("contentType"));
        }
        if (CommonUtil.isNotEmpty(param.get("suffix"))) {
            sql += " and suffix = ? ";
            pageQuery.addArg(param.get("suffix"));
        }
        if (CommonUtil.isNotEmpty(param.get("sha1"))) {
            sql += " and sha1 = ? ";
            pageQuery.addArg(param.get("sha1"));
        }
        if (CommonUtil.isNotEmpty(param.get("type"))) {
            sql += " and content_type like  ? '%' ";
            pageQuery.addArg(param.get("type"));
        }
        // 数据权限
        String permissionSql = commonService.getPermissionSql("sys_file", "create_by", "sys_role_id", "sys_org_id");
        if (CommonUtil.isNotEmpty(permissionSql)) {
            sql += " and %s".formatted(permissionSql);
        }

        sql += " order by create_time desc";
        pageQuery.setBaseSql(sql);
        return baseJdbcDao.query(SysFile.class, pageQuery);
    }

    @Transactional
    public SysFile save(SysFile sysFile) {
        if (sysFile.getId() == null) baseJdbcDao.insert(sysFile);
        else baseJdbcDao.update(sysFile);
        return sysFile;
    }

    /**
     * id获取文件详情
     */
    @Transactional(readOnly = true)
    public SysFile getById(Serializable id) {
        return baseJdbcDao.findById(SysFile.class, id);
    }

    /**
     * 批量id删除文件
     */
    public void del(List<Integer> ids) {
        log.info("批量id删除文件--");
        String sql = "select * from sys_file where id in (:ids)";
        Map<String, Object> paramMap = new HashMap<>() {{
            put("ids", ids);
        }};
        List<SysFile> list = primaryNPJdbcTemplate.query(sql, paramMap, new BeanPropertyRowMapper<>(SysFile.class));
        MinioClient minioClient = getMinioClient();
        for (SysFile sysFile : list) {
            TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(sysFile.getObject())
                        .build());
                sysFile.setStatus(4);//已删除
                baseJdbcDao.update(sysFile);
                platformTransactionManager.commit(transactionStatus); //提交事务
            } catch (Exception e) {
                log.error("删除文件出错：{}", e.getMessage());
                platformTransactionManager.rollback(transactionStatus);//回滚事务
                throw new MyException(e);
            }
        }
    }

    private MinioClient getMinioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 抽取视频的指定帧图片
     */
    public BufferedImage getVideoFrameImage(InputStream inputStream, int frameNum) {
        //注释此段代码，抽帧会集成opencv依赖，大部分用户用不上，导致打包体积过大，增加部署负担
        return null;
//        try (inputStream; FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
//            grabber.start();
//            int ftp = grabber.getLengthInFrames();
//            int currentFrameNum = 0;
//            while (currentFrameNum <= ftp) {
//                //获取帧
//                try (Frame frame = grabber.grabImage()) {
//                    if ((currentFrameNum > frameNum) && (frame != null)) {
//                        try (Java2DFrameConverter a = new Java2DFrameConverter()) {
//                            BufferedImage image = a.convert(frame);
//                            frame.close();
//                            grabber.stop();
//                            grabber.release();
//                            return image;
//                        }
//                    }
//                }
//                currentFrameNum++;
//            }
//            return null;
//        } catch (Exception e) {
//            log.error("视频截取帧图片失败", e);
//            throw new MyException("视频截取帧图片失败");
//        } finally {
//            System.gc();
//        }
    }


    /**
     * 对图片进行压缩， 长边压缩到指定像素，原图片长边小于压缩的像素大小时，直接返回原图片
     */
    public BufferedImage scaleImage(BufferedImage sourceImage, double scaleWidth) {
        // 判断文件是否为图片文件，图片文件获取宽高，横纵比
        int maxWidth = Math.max(sourceImage.getWidth(), sourceImage.getHeight());
        if (maxWidth > scaleWidth) {
            double scale = scaleWidth / maxWidth;
            int newWidth = (int) (sourceImage.getWidth() * scale);
            int newHeight = (int) (sourceImage.getHeight() * scale);
            BufferedImage newBI = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            newBI.getGraphics().drawImage(sourceImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
            return newBI;
        } else {
            return sourceImage;
        }
    }
}
