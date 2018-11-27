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

package cn.aberic.fabric.service;

import cn.aberic.fabric.bean.*;
import cn.aberic.fabric.dao.entity.Block;
import cn.aberic.fabric.dao.entity.Channel;

import java.util.List;

/**
 * 描述：
 *
 * @author : Aberic 【2018-08-10 16:23】
 */
public interface BlockService {

    int add(Block block);

    int addBlockList(List<Block> blocks);

    List<ChannelPercent> getChannelPercents(List<Channel> channels);

    List<ChannelBlockList> getChannelBlockLists(List<Channel> channels);

    DayStatistics getDayStatistics();

    Platform getPlatform();

    Block getByChannelId(int channelId);

    List<Block> getLimit(int limit);

    Curve get20CountList();

    Curve get20TxCountList();

    Curve get20RWCountList();

}
