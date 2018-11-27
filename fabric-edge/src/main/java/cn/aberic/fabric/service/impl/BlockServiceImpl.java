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

package cn.aberic.fabric.service.impl;

import cn.aberic.fabric.bean.*;
import cn.aberic.fabric.dao.entity.Block;
import cn.aberic.fabric.dao.entity.Channel;
import cn.aberic.fabric.dao.mapper.BlockMapper;
import cn.aberic.fabric.dao.mapper.ChannelMapper;
import cn.aberic.fabric.dao.mapper.PeerMapper;
import cn.aberic.fabric.service.BlockService;
import cn.aberic.fabric.utils.DateUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * 描述：
 *
 * @author : Aberic 【2018-08-10 16:24】
 */
@Service("blockService")
public class BlockServiceImpl implements BlockService {

    @Resource
    private BlockMapper blockMapper;
    @Resource
    private PeerMapper peerMapper;
    @Resource
    private ChannelMapper channelMapper;

    @Override
    public int add(Block block) {
        return blockMapper.add(block);
    }

    @Override
    public int addBlockList(List<Block> blocks) {
        if (blocks.size() > 0) {
            return blockMapper.addList(blocks);
        }
        return 0;
    }

    @Override
    public List<ChannelPercent> getChannelPercents(List<Channel> channels) {
        List<ChannelPercent> channelPercents = new LinkedList<>();
        for (Channel channel : channels) {
            int txCount = 0;
            try {
                txCount = blockMapper.countTxByChannelId(channel.getId());
            } catch (Exception ignored) {

            }
            ChannelPercent channelPercent = new ChannelPercent();
            channelPercent.setName(String.format("%s-%s", channel.getPeerName(), channel.getName()));
            channelPercent.setBlockPercent(blockMapper.countByChannelId(channel.getId()));
            channelPercent.setTxPercent(txCount);
            channelPercents.add(channelPercent);
        }
        return channelPercents;
    }

    @Override
    public List<ChannelBlockList> getChannelBlockLists(List<Channel> channels) {
        List<ChannelBlockList> channelBlockLists = new LinkedList<>();
        for (Channel channel : channels) {
            List<ChannelBlock> channelBlocks = new LinkedList<>();
            int zeroCount = 0;
            for (int i = 14; i >= 0; i--) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, 0 - i);
                int date = Integer.valueOf(DateUtil.date2Str(calendar.getTime(), "yyyyMMdd"));
                int blockCount = blockMapper.countByChannelIdAndDate(channel.getId(), date);
                if (blockCount == 0) {
                    zeroCount++;
                }
                ChannelBlock channelBlock = new ChannelBlock();
                channelBlock.setBlocks(blockCount);
                channelBlock.setDate(String.valueOf(date));
                channelBlocks.add(channelBlock);
            }
            ChannelBlockList channelBlockList = new ChannelBlockList();
            channelBlockList.setName(String.format("%s-%s", channel.getPeerName(), channel.getName()));
            channelBlockList.setChannelBlocks(channelBlocks);
            channelBlockList.setZeroCount(zeroCount);
            channelBlockLists.add(channelBlockList);
        }
        channelBlockLists.sort((t1, t2) -> Math.toIntExact(t2.getZeroCount() - t1.getZeroCount()));
        return channelBlockLists.size() >= 3 ? channelBlockLists.subList(0, 3) : channelBlockLists;
    }

    @Override
    public DayStatistics getDayStatistics() {
        int today = Integer.valueOf(DateUtil.getCurrent("yyyyMMdd"));
        int todayBlocks = 0;
        int todayTxs = 0;
        int allTxs = 0;
        try {
            todayBlocks = blockMapper.countByDate(today);
        } catch (Exception ignored) {

        }
        try {
            todayTxs = blockMapper.countTxByDate(today);
        } catch (Exception ignored) {

        }
        try {
            allTxs = blockMapper.countTx();
        } catch (Exception ignored) {

        }
        int allBlocks = blockMapper.count();
        DayStatistics dayStatistics = new DayStatistics();
        dayStatistics.setBlockCount(todayBlocks);
        dayStatistics.setTxCount(todayTxs);
        dayStatistics.setBlockPercent(todayBlocks == 0 ? 0 : (1 - todayBlocks / allBlocks) * 100);
        dayStatistics.setTxPercent(todayTxs == 0 ? 0 : (1 - todayTxs / allTxs) * 100);
        return dayStatistics;
    }

    @Override
    public Platform getPlatform() {
        int txCount = 0;
        int rwSetCount = 0;
        try {
            txCount = blockMapper.countTx();
        } catch (Exception ignored) {

        }
        try {
            rwSetCount = blockMapper.countRWSet();
        } catch (Exception ignored) {

        }
        Platform platform = new Platform();
        platform.setBlockCount(blockMapper.count());
        platform.setTxCount(txCount);
        platform.setRwSetCount(rwSetCount);
        return platform;
    }

    @Override
    public Block getByChannelId(int channelId) {
        return blockMapper.getByChannelId(channelId);
    }

    @Override
    public List<Block> getLimit(int limit) {
        List<Block> blocks = new LinkedList<>();
        List<Channel> channels = channelMapper.listAll();
        for (Channel channel : channels) {
            List<Block> blockTmps = blockMapper.getLimit(channel.getId(), limit);
            for (Block block : blockTmps) {
                block.setPeerChannelName(String.format("%s-%s", peerMapper.get(channel.getPeerId()).getName(), channel.getName()));
                block.setHeight(block.getHeight() + 1);
            }
            blocks.addAll(blockTmps);
        }
        blocks.sort((o1, o2) -> {
            try {
                return DateUtil.str2Date(o2.getTimestamp(), "yyyy/MM/dd HH:mm:ss").compareTo(DateUtil.str2Date(o1.getTimestamp(), "yyyy/MM/dd HH:mm:ss"));
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
        return blocks.size() != 0 && blocks.size() >= 6 ? blocks.subList(0, 6) : blocks;
    }

    @Override
    public Curve get20CountList() {
        List<Integer> integers = new LinkedList<>();
        Curve curve = new Curve();
        curve.setName("Block Count");
        for (int i = 19; i >= 0; i--) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 0 - i);
            int date = Integer.valueOf(DateUtil.date2Str(calendar.getTime(), "yyyyMMdd"));
            int blockCount = 0;
            try {
                blockCount = blockMapper.countByDate(date);
            } catch (Exception ignored) {

            }
            integers.add(blockCount);
        }
        int ud = integers.get(19) - integers.get(18);
        curve.setUpDown(ud >= 0 ? "+" + ud : String.valueOf(ud));
        curve.setIntegers(integers);
        return curve;
    }

    @Override
    public Curve get20TxCountList() {
        List<Integer> integers = new LinkedList<>();
        Curve curve = new Curve();
        curve.setName("TX Count");
        for (int i = 19; i >= 0; i--) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 0 - i);
            int date = Integer.valueOf(DateUtil.date2Str(calendar.getTime(), "yyyyMMdd"));
            int txCount = 0;
            try {
                txCount = blockMapper.countTxByDate(date);
            } catch (Exception ignored) {

            }
            integers.add(txCount);
        }
        int ud = integers.get(19) - integers.get(18);
        curve.setUpDown(ud >= 0 ? "+" + ud : String.valueOf(ud));
        curve.setIntegers(integers);
        return curve;
    }

    @Override
    public Curve get20RWCountList() {
        List<Integer> integers = new LinkedList<>();
        Curve curve = new Curve();
        curve.setName("RWSet Count");
        for (int i = 19; i >= 0; i--) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 0 - i);
            int date = Integer.valueOf(DateUtil.date2Str(calendar.getTime(), "yyyyMMdd"));
            int rwCount = 0;
            try {
                rwCount = blockMapper.countRWSetByDate(date);
            } catch (Exception ignored) {

            }
            integers.add(rwCount);
        }
        int ud = integers.get(19) - integers.get(18);
        curve.setUpDown(ud >= 0 ? "+" + ud : String.valueOf(ud));
        curve.setIntegers(integers);
        return curve;
    }

}
