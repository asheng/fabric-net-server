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

import cn.aberic.fabric.dao.entity.League;
import cn.aberic.fabric.dao.entity.Org;
import cn.aberic.fabric.dao.mapper.*;
import cn.aberic.fabric.service.OrgService;
import cn.aberic.fabric.utils.CacheUtil;
import cn.aberic.fabric.utils.DateUtil;
import cn.aberic.fabric.utils.DeleteUtil;
import cn.aberic.fabric.utils.FabricHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service("orgService")
public class OrgServiceImpl implements OrgService {

    @Resource
    private LeagueMapper leagueMapper;
    @Resource
    private OrgMapper orgMapper;
    @Resource
    private PeerMapper peerMapper;
    @Resource
    private CAMapper caMapper;
    @Resource
    private OrdererMapper ordererMapper;
    @Resource
    private ChannelMapper channelMapper;
    @Resource
    private ChaincodeMapper chaincodeMapper;
    @Resource
    private AppMapper appMapper;
    @Resource
    private BlockMapper blockMapper;


    @Override
    public int add(Org org) {
        if (StringUtils.isEmpty(org.getMspId())) {
            return 0;
        }
        org.setDate(DateUtil.getCurrent("yyyy-MM-dd"));
        CacheUtil.removeHome();
        return orgMapper.add(org);
    }

    @Override
    public int update(Org org) {
        FabricHelper.obtain().removeChaincodeManager(peerMapper.list(org.getId()), channelMapper, chaincodeMapper);
        CacheUtil.removeHome();
        return orgMapper.update(org);
    }

    @Override
    public List<Org> listAll() {
        List<Org> orgs = new ArrayList<>(orgMapper.listAll());
        for (Org org : orgs) {
            org.setOrdererCount(ordererMapper.count(org.getId()));
            org.setPeerCount(peerMapper.count(org.getId()));
            org.setLeagueName(leagueMapper.get(org.getLeagueId()).getName());
        }
        return orgs;
    }

    @Override
    public List<Org> listById(int id) {
        return orgMapper.list(id);
    }

    @Override
    public Org get(int id) {
        return orgMapper.get(id);
    }

    @Override
    public int countById(int id) {
        return orgMapper.count(id);
    }

    @Override
    public int count() {
        return orgMapper.countAll();
    }

    @Override
    public int delete(int id) {
        return DeleteUtil.obtain().deleteOrg(id, orgMapper, ordererMapper, peerMapper, caMapper, channelMapper, chaincodeMapper, appMapper, blockMapper);
    }

    @Override
    public List<League> listAllLeague() {
        return leagueMapper.listAll();
    }

}
