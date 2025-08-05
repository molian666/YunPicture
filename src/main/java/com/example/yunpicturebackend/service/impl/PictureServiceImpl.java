package com.example.yunpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yunpicturebackend.model.entity.Picture;
import com.example.yunpicturebackend.service.PictureService;
import com.example.yunpicturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
* @author wyh
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-08-05 09:27:16
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

}




