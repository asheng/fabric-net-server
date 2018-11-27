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

import cn.aberic.fabric.base.BaseService;
import cn.aberic.fabric.bean.Api;
import cn.aberic.fabric.bean.State;
import cn.aberic.fabric.bean.Trace;
import cn.aberic.fabric.dao.entity.*;
import cn.aberic.fabric.dao.mapper.*;
import cn.aberic.fabric.sdk.FabricManager;
import cn.aberic.fabric.service.ChaincodeService;
import cn.aberic.fabric.utils.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cn.aberic.fabric.bean.Api.Intent.INVOKE;

@Service("chaincodeService")
public class ChaincodeServiceImpl implements ChaincodeService, BaseService {

    @Resource
    private LeagueMapper leagueMapper;
    @Resource
    private OrgMapper orgMapper;
    @Resource
    private OrdererMapper ordererMapper;
    @Resource
    private PeerMapper peerMapper;
    @Resource
    private CAMapper caMapper;
    @Resource
    private ChannelMapper channelMapper;
    @Resource
    private ChaincodeMapper chaincodeMapper;
    @Resource
    private AppMapper appMapper;
    @Resource
    private Environment env;

    @Override
    public int add(Chaincode chaincode) {
        if (StringUtils.isEmpty(chaincode.getName()) ||
                StringUtils.isEmpty(chaincode.getPath()) ||
                StringUtils.isEmpty(chaincode.getVersion()) ||
                chaincode.getProposalWaitTime() == 0 ||
                null != chaincodeMapper.check(chaincode)) {
            return 0;
        }
        chaincode.setCc(createCC(chaincode));
        chaincode.setDate(DateUtil.getCurrent("yyyy-MM-dd"));
        CacheUtil.removeHome();
        return chaincodeMapper.add(chaincode);
    }

    @Override
    public JSONObject install(Chaincode chaincode, MultipartFile file, Api api, boolean init) {
        if (verify(chaincode) || null == file || null != chaincodeMapper.check(chaincode)) {
            return responseFailJson("install error, param has none value and source mush be uploaded or had the same chaincode");
        }
        if (!upload(chaincode, file)) {
            return responseFailJson("source unzip fail");
        }
        CacheUtil.removeHome();
        chaincode.setCc(createCC(chaincode));
        if (chaincodeMapper.add(chaincode) <= 0) {
            return responseFailJson("chaincode add fail");
        }
        chaincode.setId(chaincodeMapper.check(chaincode).getId());
        JSONObject jsonResult = chainCode(chaincode.getId(), caMapper.getByFlag(chaincode.getFlag()), ChainCodeIntent.INSTALL, new String[]{});
        if (jsonResult.getInteger("code") == BaseService.FAIL) {
            delete(chaincode.getId());
            return jsonResult;
        }
        return instantiate(chaincode, Arrays.asList(api.getExec().split(",")));
    }

    @Override
    public JSONObject upgrade(Chaincode chaincode, MultipartFile file, Api api) {
        if (verify(chaincode) || null == file || null == chaincodeMapper.get(chaincode.getId())) {
            return responseFailJson("install error, param has none value and source mush be uploaded or had no chaincode to upgrade");
        }
        if (!upload(chaincode, file)) {
            return responseFailJson("source unzip fail");
        }
        FabricHelper.obtain().removeChaincodeManager(chaincode.getCc());
        CacheUtil.removeHome();
        if (chaincodeMapper.updateForUpgrade(chaincode) <= 0) {
            return responseFailJson("chaincode updateForUpgrade fail");
        }
        CA ca = caMapper.getByFlag(chaincode.getFlag());
        JSONObject jsonResult = chainCode(chaincode.getId(), ca, ChainCodeIntent.INSTALL, new String[]{});
        if (jsonResult.getInteger("code") == BaseService.FAIL) {
            delete(chaincode.getId());
            return jsonResult;
        }
        List<String> strArray = Arrays.asList(api.getExec().split(","));
        int size = strArray.size();
        String[] args = new String[size];
        for (int i = 0; i < size; i++) {
            args[i] = strArray.get(i);
        }
        return chainCode(chaincode.getId(), ca, ChainCodeIntent.UPGRADE, args);
    }

    @Override
    public JSONObject instantiate(Chaincode chaincode, List<String> strArray) {
        int size = strArray.size();
        String[] args = new String[size];
        for (int i = 0; i < size; i++) {
            args[i] = strArray.get(i);
        }
        // TODO
        return chainCode(chaincode.getId(), caMapper.getByFlag(chaincode.getFlag()), ChainCodeIntent.INSTANTIATE, args);
    }

    @Override
    public int update(Chaincode chaincode) {
        chaincode.setCc(createCC(chaincode));
        FabricHelper.obtain().removeChaincodeManager(chaincode.getCc());
        CacheUtil.removeHome();
        return chaincodeMapper.update(chaincode);
    }

    @Override
    public List<Chaincode> listAll() {
        return chaincodeMapper.listAll();
    }

    @Override
    public List<Chaincode> listById(int id) {
        return chaincodeMapper.list(id);
    }

    @Override
    public Chaincode get(int id) {
        return chaincodeMapper.get(id);
    }

    @Override
    public int countById(int id) {
        return chaincodeMapper.count(id);
    }

    @Override
    public int count() {
        return chaincodeMapper.countAll();
    }

    @Override
    public int delete(int id) {
        return DeleteUtil.obtain().deleteChaincode(id, chaincodeMapper, appMapper);
    }

    @Override
    public int deleteAll(int channelId) {
        List<Chaincode> chaincodes = chaincodeMapper.list(channelId);
        for (Chaincode chaincode : chaincodes) {
            FabricHelper.obtain().removeChaincodeManager(chaincode.getCc());
            chaincodeMapper.delete(chaincode.getId());
        }
        return 0;
    }

    @Override
    public Chaincode getInstantiateChaincode(Api api, int chaincodeId) {
        Chaincode chaincode = chaincodeMapper.get(chaincodeId);
        Channel channel = channelMapper.get(chaincode.getChannelId());
        Peer peer = peerMapper.get(channel.getPeerId());
        Org org = orgMapper.get(peer.getOrgId());
        League league = leagueMapper.get(org.getLeagueId());
        chaincode.setLeagueName(league.getName());
        chaincode.setOrgName(org.getMspId());
        chaincode.setPeerName(peer.getName());
        chaincode.setChannelName(channel.getName());
        chaincode.setFlag(api.getFlag());
        return chaincode;
    }

    @Override
    public Chaincode getEditChaincode(int chaincodeId) {
        Chaincode chaincode = chaincodeMapper.get(chaincodeId);
        Peer peer = peerMapper.get(channelMapper.get(chaincode.getChannelId()).getPeerId());
        Org org = orgMapper.get(peer.getOrgId());
        League league = leagueMapper.get(org.getLeagueId());
        chaincode.setPeerName(peer.getName());
        chaincode.setOrgName(org.getMspId());
        chaincode.setLeagueName(league.getName());
        return chaincode;
    }

    @Override
    public List<Channel> getEditChannels(Chaincode chaincode) {
        List<Channel> channels = channelMapper.list(channelMapper.get(chaincode.getChannelId()).getPeerId());
        for (Channel channel : channels) {
            channel.setPeerName(chaincode.getPeerName());
            channel.setOrgName(chaincode.getOrgName());
            channel.setLeagueName(chaincode.getLeagueName());
        }
        return channels;
    }

    @Override
    public CA getCAByFlag(String flag) {
        return caMapper.getByFlag(flag);
    }

    @Override
    public List<Api> getApis() {
        List<Api> apis = new ArrayList<>();
        Api apiInvoke = new Api(SpringUtil.get("chaincode_invoke"), INVOKE.getIndex());
        Api apiQuery = new Api(SpringUtil.get("chaincode_query"), Api.Intent.QUERY.getIndex());
        Api api = new Api(SpringUtil.get("chaincode_block_info"), Api.Intent.INFO.getIndex());
        Api apiHash = new Api(SpringUtil.get("chaincode_block_get_by_hash"), Api.Intent.HASH.getIndex());
        Api apiTxid = new Api(SpringUtil.get("chaincode_block_get_by_txid"), Api.Intent.TXID.getIndex());
        Api apiNumber = new Api(SpringUtil.get("chaincode_block_get_by_height"), Api.Intent.NUMBER.getIndex());
        apis.add(apiInvoke);
        apis.add(apiQuery);
        apis.add(api);
        apis.add(apiHash);
        apis.add(apiTxid);
        apis.add(apiNumber);
        return apis;
    }

    @Override
    public List<CA> getCAs(int chaincodeId) {
        return caMapper.list(channelMapper.get(chaincodeMapper.get(chaincodeId).getChannelId()).getPeerId());
    }

    @Override
    public List<CA> getAllCAs() {
        List<CA> cas = caMapper.listAll();
        for (CA ca : cas) {
            Peer peer = peerMapper.get(ca.getPeerId());
            Org org = orgMapper.get(peer.getOrgId());
            ca.setPeerName(peer.getName());
            ca.setOrgName(org.getMspId());
            ca.setLeagueName(leagueMapper.get(org.getLeagueId()).getName());
        }
        return cas;
    }

    @Override
    public List<Chaincode> getChaincodes() {
        List<Chaincode> chaincodes = chaincodeMapper.listAll();
        for (Chaincode chaincode : chaincodes) {
            Channel channel = channelMapper.get(chaincode.getChannelId());
            chaincode.setChannelName(channel.getName());
            chaincode.setPeerName(peerMapper.get(channel.getPeerId()).getName());
        }
        return chaincodes;
    }

    @Override
    public List<Channel> getChannelFullList() {
        List<Channel> channels = channelMapper.listAll();
        for (Channel channel : channels) {
            Peer peer = peerMapper.get(channel.getPeerId());
            channel.setPeerName(peer.getName());
            Org org = orgMapper.get(peer.getOrgId());
            channel.setOrgName(org.getMspId());
            League league = leagueMapper.get(org.getLeagueId());
            channel.setLeagueName(league.getName());
        }
        return channels;
    }

    @Override
    public State getState(int id, Api api) {
        Chaincode chaincode = chaincodeMapper.get(id);
        State state = new State();
        state.setKey(api.getKey());
        state.setChannelId(chaincode.getChannelId());
        state.setFlag(api.getFlag());
        state.setVersion(api.getVersion());
        state.setStrArray(Arrays.asList(api.getExec().trim().split(",")));
        return state;
    }

    @Override
    public String formatState(State state) {
        JSONObject jsonObject = new JSONObject();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(state.getKey())) {
            jsonObject.put("key", state.getKey());
            jsonObject.put("flag", state.getFlag());
        }
        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(state.getStrArray()));
        jsonObject.put("strArray", jsonArray);
        return jsonObject.toJSONString();
    }

    @Override
    public Trace getTrace(Api api) {
        Trace trace = new Trace();
        trace.setFlag(api.getFlag());
        trace.setKey(api.getKey());
        trace.setVersion(api.getVersion());
        trace.setTrace(api.getExec().trim());
        return trace;
    }

    @Override
    public String formatTrace(Trace trace) {
        JSONObject jsonObject = new JSONObject();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(trace.getKey())) {
            jsonObject.put("key", trace.getKey());
            jsonObject.put("flag", trace.getFlag());
        }
        jsonObject.put("trace", trace.getTrace());
        return jsonObject.toJSONString();
    }

    @Override
    public Chaincode resetChaincode(Chaincode chaincode) {
        Channel channel = channelMapper.get(chaincode.getChannelId());
        Peer peer = peerMapper.get(channel.getPeerId());
        Org org = orgMapper.get(peer.getOrgId());
        League league = leagueMapper.get(org.getLeagueId());
        chaincode.setLeagueName(league.getName());
        chaincode.setOrgName(org.getMspId());
        chaincode.setPeerName(peer.getName());
        chaincode.setChannelName(channel.getName());
        return chaincode;
    }

    enum ChainCodeIntent {
        INSTALL, INSTANTIATE, UPGRADE
    }

    private JSONObject chainCode(int chaincodeId, CA ca, ChainCodeIntent intent, String[] args) {
        JSONObject jsonObject = null;
        try {
            FabricManager manager = FabricHelper.obtain().get(leagueMapper, orgMapper, channelMapper, chaincodeMapper, ordererMapper, peerMapper,
                    ca, chaincodeMapper.get(chaincodeId).getCc());
            switch (intent) {
                case INSTALL:
                    jsonObject = manager.install();
                    break;
                case INSTANTIATE:
                    jsonObject = manager.instantiate(args);
                    break;
                case UPGRADE:
                    jsonObject = manager.upgrade(args);
                    break;
            }
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            return responseFailJson(String.format("Request failed： %s", e.getMessage()));
        }
    }

    private boolean verify(Chaincode chaincode) {
        return StringUtils.isEmpty(chaincode.getName()) ||
                StringUtils.isEmpty(chaincode.getVersion()) ||
                chaincode.getProposalWaitTime() == 0;
    }

    private boolean upload(Chaincode chaincode, MultipartFile file) {
        String chaincodeSource = String.format("%s%s%s%s%s%s%s%s%s%schaincode",
                env.getProperty("config.dir"),
                File.separator,
                chaincode.getLeagueName(),
                File.separator,
                chaincode.getOrgName(),
                File.separator,
                chaincode.getPeerName(),
                File.separator,
                chaincode.getChannelName(),
                File.separator);
        String chaincodePath = Objects.requireNonNull(file.getOriginalFilename()).split("\\.")[0];
        String childrenPath = String.format("%s%ssrc%s%s", chaincodeSource, File.separator, File.separator, Objects.requireNonNull(file.getOriginalFilename()).split("\\.")[0]);
        chaincode.setSource(chaincodeSource);
        chaincode.setPath(chaincodePath);
        chaincode.setPolicy(String.format("%s%spolicy.yaml", childrenPath, File.separator));
        chaincode.setDate(DateUtil.getCurrent("yyyy-MM-dd"));
        try {
            FileUtil.unZipAndSave(file, String.format("%s%ssrc", chaincodeSource, File.separator), childrenPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String createCC(Chaincode chaincode) {
        Channel channel = channelMapper.get(chaincode.getChannelId());
        Peer peer = peerMapper.get(channel.getPeerId());
        Org org = orgMapper.get(peer.getOrgId());
        League league = leagueMapper.get(org.getLeagueId());
        return MD5Util.md5(league.getName() + org.getMspId() + peer.getName() + channel.getName() + chaincode.getName());
    }
}
