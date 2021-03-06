/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */

package herddb.model;

import herddb.model.planner.PlannerOp;
import herddb.utils.ObjectSizeUtils;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data access execution plan.
 *
 * @author enrico.olivelli
 */
public final class ExecutionPlan {

    private static final AtomicLong ID = new AtomicLong();
    private final long id = ID.incrementAndGet();
    public final Statement mainStatement;
    // this is actually only for tests and debug
    public final PlannerOp originalRoot;

    private ExecutionPlan(
            Statement mainStatement,
            PlannerOp originalRoot
    ) {
        this.mainStatement = mainStatement;

        this.originalRoot = originalRoot;
    }

    public static ExecutionPlan simple(Statement statement) {
        return new ExecutionPlan(statement, null);
    }

    public static ExecutionPlan simple(Statement statement, PlannerOp root) {
        return new ExecutionPlan(statement, root);
    }

    public void validateContext(StatementEvaluationContext context) throws StatementExecutionException {
        mainStatement.validateContext(context);
    }

    @Override
    public String toString() {
        return "Plan" + id;
    }

    /**
     * Estimate Object size for the PlanCache.
     * see {@link ObjectSizeUtils} for the limitations of this computation.
     */
    public int estimateObjectSizeForCache() {
        // ignore Calcite object
        return mainStatement.estimateObjectSizeForCache();
    }

}
