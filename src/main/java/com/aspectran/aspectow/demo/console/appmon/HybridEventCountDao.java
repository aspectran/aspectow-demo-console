/*
 * Copyright (c) 2018-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.demo.console.appmon;

import com.aspectran.aspectow.appmon.engine.persist.counter.EventCountVO;
import com.aspectran.aspectow.appmon.engine.persist.db.mapper.EventCountDao;
import com.aspectran.aspectow.appmon.engine.persist.db.mapper.EventCountMapper;
import com.aspectran.aspectow.console.common.db.tx.AppMonSqlMapperProvider;
import com.aspectran.mybatis.SqlMapperProvider;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A hybrid DAO implementation that uses both local MariaDB and Supabase.
 * Writes are duplicated, and aggregated reads are served from Supabase.
 */
public class HybridEventCountDao extends EventCountDao {

    private final SqlMapperProvider supabaseProvider;

    public HybridEventCountDao(AppMonSqlMapperProvider localProvider, HybridAppMonSqlMapperProvider supabaseProvider) {
        super(localProvider);
        this.supabaseProvider = supabaseProvider;
    }

    private EventCountMapper supabase() {
        return supabaseProvider.mapper(EventCountMapper.class);
    }

    @Override
    public void insertEventCountHourly(EventCountVO eventCountVO) {
        super.insertEventCountHourly(eventCountVO);
        try {
            supabase().insertEventCountHourly(eventCountVO);
        } catch (Exception e) {
            // Log and ignore Supabase errors
        }
    }

    @Override
    public List<EventCountVO> getChartDataByHour(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return supabase().getChartDataByHour(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getChartDataByDay(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return supabase().getChartDataByDay(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartDataByDay(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return supabase().getGroupChartDataByDay(groupId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getChartDataByMonth(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return supabase().getChartDataByMonth(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartDataByMonth(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return supabase().getGroupChartDataByMonth(groupId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getChartDataByYear(String nodeId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return supabase().getChartDataByYear(nodeId, appId, eventId, zoneOffset, dateOffset);
    }

    @Override
    public List<EventCountVO> getGroupChartDataByYear(String groupId, String appId, String eventId, int zoneOffset, LocalDateTime dateOffset) {
        return supabase().getGroupChartDataByYear(groupId, appId, eventId, zoneOffset, dateOffset);
    }

}
