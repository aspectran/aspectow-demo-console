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

import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.Transform;
import com.aspectran.core.context.rule.type.FormatType;

/**
 * SampleCommandsActivity provides views and REST API endpoints for managing
 * cluster nodes and executing remote file commands.
 *
 * <p>Created: 2026-04-16</p>
 */
@Component("demo/commands")
public class SampleCommandsActivity {

    @Request("hello")
    @Transform(FormatType.TEXT)
    public String hello() {
        return "Hello, World!";
    }

}
