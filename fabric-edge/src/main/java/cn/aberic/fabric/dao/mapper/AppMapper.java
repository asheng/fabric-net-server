/*
 * Copyright (c) 2018. Aberic - All Rights Reserved.
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

package cn.aberic.fabric.dao.mapper;

import cn.aberic.fabric.dao.entity.App;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 作者：Aberic on 2018/6/9 13:53
 * 邮箱：abericyang@gmail.com
 */
@Mapper
public interface AppMapper {

    @Insert("insert into fns_app (name, app_key, chaincode_id, create_date, modify_date, active)" +
            " values (#{a.name},#{a.appKey},#{a.chaincodeId},#{a.createDate},#{a.modifyDate},#{a.active})")
    int add(@Param("a") App app);

    @Update("update fns_app set name=#{a.name}, app_key=#{a.appKey}, modify_date=#{a.modifyDate}, active=#{a.active} where id=#{a.id}")
    int update(@Param("a") App app);

    @Update("update fns_app set app_key=#{a.appKey} where id=#{a.id}")
    int updateKey(@Param("a") App app);

    @Select("select count(name) from fns_app where chaincode_id=#{id}")
    int countById(@Param("id") int id);

    @Select("select count(name) from fns_app")
    int count();

    @Select("select app_key from fns_app where name=#{a.name} and chaincode_id=#{a.chaincodeId}")
    @Results({
            @Result(property = "name", column = "name"),
            @Result(property = "appKey", column = "appKey"),
            @Result(property = "chaincode_id", column = "chaincodeId")
    })
    App check(@Param("a") App app);

    @Select("select id, name, app_key, chaincode_id, create_date, modify_date, active from fns_app where chaincode_id=#{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "appKey", column = "app_key"),
            @Result(property = "chaincodeId", column = "chaincode_id"),
            @Result(property = "createDate", column = "create_date"),
            @Result(property = "modifyDate", column = "modify_date"),
            @Result(property = "active", column = "active")
    })
    List<App> list(@Param("id") int id);

    @Select("select id, name, app_key, chaincode_id, create_date, modify_date, active from fns_app where id=#{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "appKey", column = "app_key"),
            @Result(property = "chaincodeId", column = "chaincode_id"),
            @Result(property = "createDate", column = "create_date"),
            @Result(property = "modifyDate", column = "modify_date"),
            @Result(property = "active", column = "active")
    })
    App get(@Param("id") int id);

    @Select("select id, name, app_key, chaincode_id, create_date, modify_date, active from fns_app where app_key=#{appKey}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "appKey", column = "app_key"),
            @Result(property = "chaincodeId", column = "chaincode_id"),
            @Result(property = "createDate", column = "create_date"),
            @Result(property = "modifyDate", column = "modify_date"),
            @Result(property = "active", column = "active")
    })
    App getByKey(@Param("appKey") String appKey);

    @Delete("delete from fns_app where id=#{id}")
    int delete(@Param("id") int id);

    @Delete("delete from fns_app where chaincode_id=#{id}")
    int deleteAll(@Param("id") int id);

}
