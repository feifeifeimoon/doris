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

package org.apache.doris.resource.computegroup;

import org.apache.doris.cloud.system.CloudSystemInfoService;
import org.apache.doris.system.Backend;

import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

public class CloudComputeGroup extends ComputeGroup {

    public CloudComputeGroup(String id, String name, CloudSystemInfoService systemInfoService) {
        super(id, name, systemInfoService);
    }

    @Override
    public List<Backend> getBackendList() {
        return ((CloudSystemInfoService) (super.systemInfoService)).getBackendsByClusterName(name);
    }

    @Override
    public Set<String> getNames() {
        return Sets.newHashSet(id);
    }

}
