package com.example.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yunpicturebackend.exception.BusinessException;
import com.example.yunpicturebackend.exception.ErrorCode;
import com.example.yunpicturebackend.exception.ThrowUtils;
import com.example.yunpicturebackend.manager.FileManager;
import com.example.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.example.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.example.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.example.yunpicturebackend.model.entity.Picture;
import com.example.yunpicturebackend.model.entity.User;
import com.example.yunpicturebackend.model.vo.PictureVO;
import com.example.yunpicturebackend.model.vo.UserVO;
import com.example.yunpicturebackend.service.PictureService;
import com.example.yunpicturebackend.mapper.PictureMapper;
import com.example.yunpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新，则需要判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            if (!exists) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
        }
        //上传图片
        //按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        //构造入库图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //操作数据库
        //如果图片id不为空表示更新，否则是新增
        if (pictureId != null) {
            //如果是更新需要补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
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
}




