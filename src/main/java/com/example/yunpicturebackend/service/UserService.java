package com.example.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.example.yunpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yunpicturebackend.model.vo.LoginUserVO;
import com.example.yunpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author wyh
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-08-02 09:01:29
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密密码
     * @param userPassword 原密码
     * @return 加密后密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 登录
     * @param userAccount
     * @param userPassword
     * @return 用户脱敏后信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest  request);

    /**
     * 获取脱敏用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏用户信息列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 注销登录
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest  request);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
