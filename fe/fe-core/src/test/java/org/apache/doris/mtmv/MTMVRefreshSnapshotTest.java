// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.mtmv;

import org.apache.doris.common.AnalysisException;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class MTMVRefreshSnapshotTest {
    private String mvExistPartitionName = "mvp1";
    private String relatedExistPartitionName = "p1";
    private long baseExistTableId = 1L;
    private long correctVersion = 1L;
    private MTMVRefreshSnapshot refreshSnapshot = new MTMVRefreshSnapshot();
    private MTMVVersionSnapshot p1Snapshot = new MTMVVersionSnapshot(correctVersion, 0);
    private MTMVVersionSnapshot t1Snapshot = new MTMVVersionSnapshot(correctVersion, 0);
    @Mocked
    private BaseTableInfo existTable;
    @Mocked
    private BaseTableInfo nonExistTable;

    @Before
    public void setUp() throws NoSuchMethodException, SecurityException, AnalysisException {
        new Expectations() {
            {
                existTable.getCtlName();
                minTimes = 0;
                result = "ctl1";

                existTable.getDbName();
                minTimes = 0;
                result = "db1";

                existTable.getTableName();
                minTimes = 0;
                result = "t1";

                existTable.getTableId();
                minTimes = 0;
                result = 1L;

                nonExistTable.getCtlName();
                minTimes = 0;
                result = "ctl1";

                nonExistTable.getDbName();
                minTimes = 0;
                result = "db1";

                nonExistTable.getTableName();
                minTimes = 0;
                result = "t2";

                nonExistTable.getTableId();
                minTimes = 0;
                result = 2L;
            }
        };

        Map<String, MTMVRefreshPartitionSnapshot> partitionSnapshots = Maps.newHashMap();
        MTMVRefreshPartitionSnapshot mvp1PartitionSnapshot = new MTMVRefreshPartitionSnapshot();
        partitionSnapshots.put(mvExistPartitionName, mvp1PartitionSnapshot);
        mvp1PartitionSnapshot.getPartitions().put(relatedExistPartitionName, p1Snapshot);
        mvp1PartitionSnapshot.addTableSnapshot(existTable, t1Snapshot);
        refreshSnapshot.updateSnapshots(partitionSnapshots, Sets.newHashSet(mvExistPartitionName));
    }

    @Test
    public void testPartitionSync() {
        // normal
        boolean sync = refreshSnapshot.equalsWithRelatedPartition(mvExistPartitionName, relatedExistPartitionName,
                new MTMVVersionSnapshot(correctVersion, 0));
        Assert.assertTrue(sync);
        // non exist mv partition
        sync = refreshSnapshot.equalsWithRelatedPartition("mvp2", relatedExistPartitionName,
                new MTMVVersionSnapshot(correctVersion, 0));
        Assert.assertFalse(sync);
        // non exist related partition
        sync = refreshSnapshot
                .equalsWithRelatedPartition(mvExistPartitionName, "p2", new MTMVVersionSnapshot(correctVersion, 0));
        Assert.assertFalse(sync);
        // snapshot value not equal
        sync = refreshSnapshot.equalsWithRelatedPartition(mvExistPartitionName, relatedExistPartitionName,
                new MTMVVersionSnapshot(2L, 0));
        Assert.assertFalse(sync);
        // snapshot type not equal
        sync = refreshSnapshot.equalsWithRelatedPartition(mvExistPartitionName, relatedExistPartitionName,
                new MTMVTimestampSnapshot(correctVersion));
        Assert.assertFalse(sync);
    }

    @Test
    public void testTableSync() {
        // normal
        boolean sync = refreshSnapshot.equalsWithBaseTable(mvExistPartitionName, existTable,
                new MTMVVersionSnapshot(correctVersion, 0));
        Assert.assertTrue(sync);
        // non exist mv partition
        sync = refreshSnapshot
                .equalsWithBaseTable("mvp2", existTable, new MTMVVersionSnapshot(correctVersion, 0));
        Assert.assertFalse(sync);
        // non exist related partition
        sync = refreshSnapshot
                .equalsWithBaseTable(mvExistPartitionName, nonExistTable, new MTMVVersionSnapshot(correctVersion, 0));
        Assert.assertFalse(sync);
        // snapshot value not equal
        sync = refreshSnapshot
                .equalsWithBaseTable(mvExistPartitionName, existTable, new MTMVVersionSnapshot(2L, 0));
        Assert.assertFalse(sync);
        // snapshot type not equal
        sync = refreshSnapshot.equalsWithBaseTable(mvExistPartitionName, existTable,
                new MTMVTimestampSnapshot(correctVersion));
        Assert.assertFalse(sync);
    }
}
