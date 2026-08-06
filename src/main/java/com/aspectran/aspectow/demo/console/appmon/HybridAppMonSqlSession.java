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
import com.aspectran.aspectow.appmon.engine.persist.db.tx.AppMonTxAspect;

/**
 * A {@link HybridAppMonSqlSession} for handling simple, auto-committing database sessions.
 * This agent is advised by {@link AppMonTxAspect} to manage transactions.
 *
 * <p>Created: 2025. 2. 15.</p>
 */
public class HybridAppMonSqlSession extends AppMonSqlSession {

    /**
     * Instantiates a new DefaultSqlSessionAgent.
     * @param txAspectId the ID of the aspect that provides the SqlSessionAdvice
     */
    public HybridAppMonSqlSession(String txAspectId) {
        super(txAspectId);
    }

}
