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

import com.aspectran.aspectow.appmon.engine.persist.db.tx.AppMonSqlSession;
import com.aspectran.aspectow.console.common.db.tx.AppMonSqlMapperProvider;
import org.apache.ibatis.session.SqlSession;

/**
 * A provider class for managing an instance of {@link HybridAppMonSqlSession}.
 * This class extends {@link AppMonSqlMapperProvider} and is designed to supply
 * a {@link HybridAppMonSqlSession} for database operations.
 * <p>
 * The {@link HybridAppMonSqlMapperProvider} is primarily used in scenarios where
 * a hybrid session is required to manage database interactions that combine
 * features of both standard and specific implementations of {@link SqlSession}.</p>
 * <p>
 * This implementation allows seamless integration with the existing infrastructure
 * of {@link AppMonSqlMapperProvider}, leveraging the functionality of {@link HybridAppMonSqlSession}.</p>
 * <p>
 * @see AppMonSqlMapperProvider
 * @see HybridAppMonSqlSession
 */
public class HybridAppMonSqlMapperProvider extends AppMonSqlMapperProvider {

    public HybridAppMonSqlMapperProvider(AppMonSqlSession simpleSqlSession) {
        super(simpleSqlSession);
    }

}
