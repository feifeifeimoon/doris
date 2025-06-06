
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

suite("test_primary_key_partial_update", "p0") {

    String db = context.config.getDbNameByFile(context.file)
    sql "select 1;" // to create database

    for (def use_row_store : [false, true]) {
        logger.info("current params: use_row_store: ${use_row_store}")

        connect( context.config.jdbcUser, context.config.jdbcPassword, context.config.jdbcUrl) {
            sql "use ${db};"
            def tableName = "test_primary_key_partial_update"
            // create table
            sql """ DROP TABLE IF EXISTS ${tableName} """
            sql """ CREATE TABLE ${tableName} (
                        `id` int(11) NOT NULL COMMENT "用户 ID",
                        `name` varchar(65533) NOT NULL COMMENT "用户姓名",
                        `score` int(11) NOT NULL COMMENT "用户得分",
                        `test` int(11) NULL COMMENT "null test",
                        `dft` int(11) DEFAULT "4321")
                        UNIQUE KEY(`id`) DISTRIBUTED BY HASH(`id`) BUCKETS 1
                        PROPERTIES("replication_num" = "1", "enable_unique_key_merge_on_write" = "true",
                        "store_row_column" = "${use_row_store}"); """

            // insert 2 lines
            sql """
                insert into ${tableName} values(2, "doris2", 2000, 223, 1)
            """

            sql """
                insert into ${tableName} values(1, "doris", 1000, 123, 1)
            """

            // skip 3 lines and file have 4 lines
            streamLoad {
                table "${tableName}"

                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'true'
                set 'columns', 'id,score'

                file 'basic.csv'
                time 10000 // limit inflight 10s
            }

            sql "sync"

            qt_select_default """
                select * from ${tableName} order by id;
            """

            // partial update a row multiple times in one stream load
            streamLoad {
                table "${tableName}"

                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'true'
                set 'columns', 'id,score'

                file 'basic_with_duplicate.csv'
                time 10000 // limit inflight 10s
            }

            sql "sync"

            qt_partial_update_in_one_stream_load """
                select * from ${tableName} order by id;
            """

            streamLoad {
                table "${tableName}"

                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'true'
                set 'columns', 'id,score'

                file 'basic_with_duplicate2.csv'
                time 10000 // limit inflight 10s
            }

            sql "sync"

            qt_partial_update_in_one_stream_load """
                select * from ${tableName} order by id;
            """

            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'true'
                set 'columns', 'id,name,score'

                file 'basic_with_new_keys.csv'
                time 10000 // limit inflight 10s
            }

            sql "sync"

            qt_partial_update_in_one_stream_load """
                select * from ${tableName} order by id;
            """

            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'false'
                set 'columns', 'id,name,score'

                file 'basic_with_new_keys.csv'
                time 10000 // limit inflight 10s
            }

            sql "sync"

            qt_partial_update_in_one_stream_load """
                select * from ${tableName} order by id;
            """

            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'true'
                set 'columns', 'id,name,score'

                file 'basic_with_new_keys_and_invalid.csv'
                time 10000// limit inflight 10s

                check {result, exception, startTime, endTime ->
                    assertTrue(exception == null)
                    def json = parseJson(result)
                    assertEquals("Fail", json.Status)
                }
            }

            qt_partial_update_in_one_stream_load """
                select * from ${tableName} order by id;
            """

            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'true'
                set 'columns', 'id,score'

                file 'basic_invalid.csv'
                time 10000// limit inflight 10s

                check {result, exception, startTime, endTime ->
                    assertTrue(exception == null)
                    def json = parseJson(result)
                    assertEquals("Fail", json.Status)
                    assertTrue(json.Message.contains("[DATA_QUALITY_ERROR]too many filtered rows"))
                    assertEquals(3, json.NumberTotalRows)
                    assertEquals(0, json.NumberLoadedRows)
                    assertEquals(2, json.NumberFilteredRows)
                }
            }
            sql "sync"
            qt_partial_update_in_one_stream_load """
                select * from ${tableName} order by id;
            """

            // drop table
            sql """ DROP TABLE IF EXISTS ${tableName} """

            sql """ CREATE TABLE ${tableName} (
                        `name` VARCHAR(600) NULL,
                        `userid` INT NOT NULL,
                        `seq` BIGINT NOT NULL AUTO_INCREMENT(1),
                        `ctime` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
                        `rtime` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                        `corp_name` VARCHAR(600) NOT NULL
                        ) ENGINE = OLAP UNIQUE KEY(`name`, `userid`) COMMENT 'OLAP' DISTRIBUTED BY HASH(`name`) BUCKETS 10 
                        PROPERTIES ("replication_num" = "1",
                                    "enable_unique_key_merge_on_write" = "true",
                                    "store_row_column" = "${use_row_store}"); """

            sql "set enable_unique_key_partial_update=true;" 
            sql "set enable_insert_strict=false;"

            sql "INSERT INTO ${tableName}(`name`, `userid`, `corp_name`) VALUES ('test1', 1234567, 'A');"

            qt_select_timestamp "select count(*) from ${tableName} where `ctime` > \"1970-01-01\""

            sql "set time_zone = 'Asia/Tokyo'"

            Thread.sleep(5000)

            sql "INSERT INTO ${tableName}(`name`, `userid`, `corp_name`) VALUES ('test2', 1234567, 'A');"

            qt_select_timestamp2 "SELECT ABS(TIMESTAMPDIFF(HOUR, MIN(ctime), MAX(ctime))) AS time_difference_hours FROM ${tableName};"

            // drop table
            sql """ DROP TABLE IF EXISTS ${tableName} """

            sql """ SET enable_nereids_planner=true; """
            sql """ CREATE TABLE ${tableName} (
                        `name` VARCHAR(600) NULL,
                        `userid` INT NOT NULL,
                        `seq` BIGINT NOT NULL AUTO_INCREMENT(1),
                        `ctime` DATE DEFAULT CURRENT_DATE,
                        `corp_name` VARCHAR(600) NOT NULL
                        ) ENGINE = OLAP UNIQUE KEY(`name`, `userid`) COMMENT 'OLAP' DISTRIBUTED BY HASH(`name`) BUCKETS 10 
                        PROPERTIES ("replication_num" = "1",
                                    "enable_unique_key_merge_on_write" = "true",
                                    "store_row_column" = "${use_row_store}"); """

            sql "set enable_unique_key_partial_update=true;" 
            sql "set enable_insert_strict=false;"

            sql "INSERT INTO ${tableName}(`name`, `userid`, `corp_name`) VALUES ('test1', 1234567, 'A');"

            qt_select_date "select count(*) from ${tableName} where `ctime` > \"1970-01-01\""

            sql "set time_zone = 'America/New_York'"

            sql "INSERT INTO ${tableName}(`name`, `userid`, `corp_name`) VALUES ('test2', 1234567, 'B');"

            qt_select_date2 "select count(*) from ${tableName} where `ctime` > \"1970-01-01\""

            // test partial update with update statement
            // drop table
            sql """ DROP TABLE IF EXISTS ${tableName} """

            sql """ SET enable_nereids_planner=true; """
            sql """ CREATE TABLE ${tableName} (
                        `name` VARCHAR(600) NULL,
                        `userid` INT NOT NULL,
                        `seq` BIGINT NOT NULL AUTO_INCREMENT(1),
                        `ctime` DATE DEFAULT CURRENT_DATE,
                        `corp_name` VARCHAR(600) NOT NULL
                        ) ENGINE = OLAP UNIQUE KEY(`name`, `userid`) COMMENT 'OLAP' DISTRIBUTED BY HASH(`name`) BUCKETS 10 
                        PROPERTIES ("replication_num" = "1",
                                    "enable_unique_key_merge_on_write" = "true",
                                    "store_row_column" = "${use_row_store}"); """

            sql "set enable_unique_key_partial_update=true;" 
            sql "set enable_insert_strict=false;"

            sql "INSERT INTO ${tableName}(`name`, `userid`, `corp_name`) VALUES ('test1', 1234567, 'A');"

            sql "UPDATE ${tableName} set corp_name = 'B';"
            qt_select_update "select corp_name from ${tableName};"


            tableName = "test_primary_key_partial_update_1"
            sql """ DROP TABLE IF EXISTS ${tableName} FORCE"""
            sql """ CREATE TABLE IF NOT EXISTS ${tableName} (
                    `k1` int NOT NULL,
                    `c1` int,
                    `c2` int,
                    `c3` int,
                    `c4` int
                    )UNIQUE KEY(k1)
                DISTRIBUTED BY HASH(k1) BUCKETS 1
                PROPERTIES (
                    "disable_auto_compaction" = "true",
                    "replication_num" = "1"); """

            sql "insert into ${tableName} values(1,1,1,1,1);"
            sql "insert into ${tableName} values(2,2,2,2,2);"
            sql "insert into ${tableName} values(3,3,3,3,3);"
            sql "sync;"
            qt_sql "select * from ${tableName} order by k1;"

            String content1 = 
"""
1,99,99,99,99,0
2,88,88,88,88,0
4,77,77,77,77,0
3,23,23,23,23,1
""".trim()
            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'format', 'csv'
                set 'partial_columns', 'true'
                set 'hidden_columns', '__DORIS_DELETE_SIGN__'
                inputStream new ByteArrayInputStream(content1.getBytes()) 
                time 10000// limit inflight 10s
            }
            qt_sql "select * from ${tableName} order by k1;"

            // MERGE_TYPE=MERGE, test delete on illegal column
            String content2 = "1,99,1"
            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'format', 'csv'
                set 'columns', 'k1,c2'
                set 'partial_columns', 'true'
                set 'merge_type', 'MERGE'
                set 'delete', 'c3=1'
                inputStream new ByteArrayInputStream(content2.getBytes()) 
                time 10000
                check {result, exception, startTime, endTime ->
                    assertTrue(exception == null)
                    def json = parseJson(result)
                    assertEquals("Fail", json.Status)
                    assertTrue(json.Message.contains("Unknown column 'c3'"))
                }
            }

            String content3 = 
"""
1,99
2,88,
""".trim()
            streamLoad {
                table "${tableName}"
                set 'column_separator', ','
                set 'format', 'csv'
                set 'columns', 'k1,c4'
                set 'partial_columns', 'true'
                set 'where', 'c5=1'
                inputStream new ByteArrayInputStream(content3.getBytes()) 
                time 10000
                check {result, exception, startTime, endTime ->
                    assertTrue(exception == null)
                    def json = parseJson(result)
                    assertEquals("Fail", json.Status)
                    assertTrue(json.Message.contains("Unknown column 'c5'"))
                }
            }
        }
    }
}
