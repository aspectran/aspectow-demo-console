/*
 * Copyright (c) 2020-present The Aspectran Project
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
package com.aspectran.aspectow.demo.console.demo;

import com.aspectran.aspectow.node.manager.NodeManager;
import com.aspectran.core.component.bean.annotation.Bean;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Job;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Schedule;
import com.aspectran.core.component.bean.annotation.SimpleTrigger;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.ActivityContext;
import com.aspectran.core.context.rule.type.FormatType;
import com.aspectran.core.context.rule.type.MisfirePolicy;
import com.aspectran.core.service.CoreServiceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Created: 2026. 6. 6.</p>
 */
@Component
@Bean(lazyInit = true)
@Schedule(
        id = "countSchedule",
        scheduler = "demoScheduler",
        simpleTrigger = @SimpleTrigger(
                startDelaySeconds = 3,      // Start 3 seconds after scheduler starts
                intervalInSeconds = 3,      // Repeat every 3 seconds
                repeatForever = true,       // Repeat indefinitely
                misfirePolicy = MisfirePolicy.SMART_POLICY
        ),
        jobs = {
                @Job(translet = "demo/schedule/count.job")
        },
        disabled = true
)
public class CountSchedule {

    private final Logger logger = LoggerFactory.getLogger(CountSchedule.class);

    private final AtomicInteger counter = new AtomicInteger(0);

    private String nodeId;

    public CountSchedule() {
        resolveNodeId();
    }

    @Request("test/schedule/count.job")
    @Transform(FormatType.TEXT)
    public String count() {
        int count = counter.incrementAndGet();
        String result = "NodeId: " + nodeId + ", Count: " + count;
        logger.info(result);
        return result;
    }

    private void resolveNodeId() {
        ActivityContext context = CoreServiceHolder.findActivityContext();
        if (context != null) {
            NodeManager nodeManager = context.getBeanRegistry().getBean(NodeManager.class);
            nodeId = nodeManager.getNodeId();
        } else {
            nodeId = "Unknown";
        }
    }

}
