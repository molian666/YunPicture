package com.example.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yunpicturebackend.exception.BusinessException;
import com.example.yunpicturebackend.exception.ErrorCode;
import com.example.yunpicturebackend.exception.ThrowUtils;
import com.example.yunpicturebackend.manager.CosManager;
import com.example.yunpicturebackend.manager.FileManager;
import com.example.yunpicturebackend.manager.upload.FilePictureUpload;
import com.example.yunpicturebackend.manager.upload.PictureUploadTemplate;
import com.example.yunpicturebackend.manager.upload.UrlPictureUpload;
import com.example.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.example.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.example.yunpicturebackend.model.dto.picture.PictureReviewRequest;
import com.example.yunpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.example.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.example.yunpicturebackend.model.entity.Picture;
import com.example.yunpicturebackend.model.entity.User;
import com.example.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.example.yunpicturebackend.model.vo.PictureVO;
import com.example.yunpicturebackend.model.vo.UserVO;
import com.example.yunpicturebackend.service.PictureService;
import com.example.yunpicturebackend.mapper.PictureMapper;
import com.example.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.jndi.JndiAccessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author wyh
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-08-05 09:27:16
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新，则需要判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
            //仅管理员和本人可以编辑
            if (!loginUser.getId().equals(oldPicture.getUserId()) && !userService.isAdmin(loginUser)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        //上传图片
        //按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        //根据inputSource的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String){
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造入库图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getName();
        //支持外层传递图片名称
        if (pictureUploadRequest != null && pictureUploadRequest.getPicName() != null){
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //补充审核参数
        this.fillReviewParams(picture, loginUser);
        //操作数据库
        //如果图片id不为空表示更新，否则是新增
        if (pictureId != null) {
            //如果是更新需要补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        //清理old图片
        Picture oldPicture = this.getById(pictureId);
        this.clearPictureFile(oldPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        String searchText = pictureQueryRequest.getSearchText();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Double picScale = pictureQueryRequest.getPicScale();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        //从多字段搜索
        if (StringUtils.isNotBlank(searchText)){
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StringUtils.isNotBlank(category), "category", category);
        queryWrapper.eq(picSize != null, "picSize", picSize);
        queryWrapper.eq(picWidth != null, "picWidth", picWidth);
        queryWrapper.eq(picHeight != null, "picHeight", picHeight);
        queryWrapper.eq(picScale != null, "picScale", picScale);
        queryWrapper.like(picFormat != null, "picFormat", picFormat);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(reviewMessage != null, "reviewMessage", reviewMessage);
        queryWrapper.eq(reviewerId != null, "reviewerId", reviewerId);
        queryWrapper.eq(reviewStatus!= null, "reviewStatus", reviewStatus);
        //json 字段查询
        if (CollUtil.isNotEmpty(tags)){
            for (String tag : tags){
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        //排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取图片封装类
     * @param picture
     * @param httpServletRequest
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest httpServletRequest){
        //对象转为封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> page, HttpServletRequest request) {
        List<Picture> pictureList = page.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(pictureList)){
            return pictureVOPage;
        }
        //对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        //查询关联用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    /**
     * 校验图片
     * @param picture
     */
    @Override
    public void validPicture(Picture picture){
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //修改数据时id不能为空
        ThrowUtils.throwIf(id == null && !StringUtils.isBlank(url), ErrorCode.PARAMS_ERROR, "图片id不能为空");
        if (StringUtils.isNotBlank(url)){
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "图片url过长");
        }
        if (StringUtils.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }
    }

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getReviewerId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || pictureReviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿重复审核");
        }
        //数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)){
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员审核通过");
        }else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer UploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        //名称前缀默认等于搜索文本
        if (StringUtils.isBlank(searchText)){
            namePrefix = searchText;
        }
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多上传30张图片");
        //抓取图片
        Document document = null;
        String fetchUrl = String.format("https://cn.bing.com/images/search?q=%s&form=HDRSC2&first=1", searchText);
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("页面获取失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "页面获取失败");
        }
        //解析图片
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        //遍历元素依次上传图片
        int uploadCount = 0;
        Elements imgElementList = div.select("img.mimg");
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StringUtils.isBlank(fileUrl)){
                log.info("图片地址为空,已跳过，{}", fileUrl);
                continue;
            }
            //处理图片地址防止转义和对象存储冲突问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1){
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            }catch (Exception e) {
                log.error("图片上传失败，fileUrl = {}", fileUrl, e);
                continue;
            }
            if (uploadCount >= count){
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        //判断改图片是否被多条记录使用
        String url = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, url)
                .count();
        if (count > 1){
            return;
        }
        //删除图片
        cosManager.deleteObject(url);
        //删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StringUtils.isNotBlank(thumbnailUrl)){
            cosManager.deleteObject(thumbnailUrl);
        }
    }
}




