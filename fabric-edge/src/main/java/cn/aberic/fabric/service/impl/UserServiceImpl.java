/*
 * Copyright (c) 2018. Aberic - aberic@qq.com - All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aberic.fabric.service.impl;

import cn.aberic.fabric.dao.entity.Role;
import cn.aberic.fabric.dao.entity.User;
import cn.aberic.fabric.dao.mapper.RoleMapper;
import cn.aberic.fabric.dao.mapper.UserMapper;
import cn.aberic.fabric.service.UserService;
import cn.aberic.fabric.utils.CacheUtil;
import cn.aberic.fabric.utils.DateUtil;
import cn.aberic.fabric.utils.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public int init(User user) {
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return 0;
        }
        if (null != userMapper.get(user.getUsername())) {
            return update(user);
        }
        return add(user);
    }

    @Override
    public int add(User user) {
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return 0;
        }
        user.setDate(DateUtil.getCurrent("yyyyMMddHHmmss"));
        return userMapper.add(user);
    }

    @Override
    public int create(User user) {
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return 0;
        }
        if (null != userMapper.get(user.getUsername())) {
            return 0;
        }
        user.setPassword(MD5Util.md5(user.getPassword()));
        user.setDate(DateUtil.getCurrent("yyyyMMddHHmmss"));
        return add(user);
    }

    @Override
    public int delete(int id) {
        return userMapper.delete(id);
    }

    @Override
    public int update(User user) {
        return userMapper.update(user);
    }

    @Override
    public int upgrade(User user) {
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return 0;
        }
        user.setPassword(MD5Util.md5(user.getPassword()));
        return userMapper.upgrade(user);
    }

    @Override
    public int updatePassword(User user) {
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
            return 0;
        }
        user.setPassword(MD5Util.md5(user.getPassword()));
        return userMapper.updatePassword(user);
    }

    @Override
    public int updateRole(User user) {
        return userMapper.updateRole(user);
    }

    @Override
    public int setRole(User user) {
        return userMapper.setRole(user);
    }

    @Override
    public List<User> listAll() {
        List<User> users = userMapper.listAll();
        for (User user: users) {
            try {
                user.setDate(DateUtil.strDateFormat(user.getDate(), "yyyyMMddHHmmss", "yyyy/MM/dd HH:mm:ss"));
                user.setRoleName(roleMapper.getRoleById(user.getRoleId()).getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return users;
    }

    @Override
    public List<Role> listRole() {
        return roleMapper.listRole();
    }

    @Override
    public User get(String username) {
        return userMapper.get(username);
    }

    @Override
    public User get(int id) {
        return userMapper.getById(id);
    }

    @Override
    public String login(User user) {
        User userCache = userMapper.get(user.getUsername());
        try {
            if (MD5Util.verify(user.getPassword(), userCache.getPassword())) {
                String token = UUID.randomUUID().toString();
                // CacheUtil.putString(user.getUsername(), token);
                CacheUtil.putUser(token, userCache);
                return token;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int addRole(Role role) {
        return roleMapper.add(role);
    }

    @Override
    public int addRoleList(List<Role> roles) {
        if (roles.size() > 0) {
            return roleMapper.addList(roles);
        }
        return 0;
    }

    @Override
    public Role getRoleById(int id) {
        return roleMapper.getRoleById(id);
    }

}
