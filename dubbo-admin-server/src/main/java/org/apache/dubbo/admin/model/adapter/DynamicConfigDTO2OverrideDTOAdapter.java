/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.admin.model.adapter;

import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.model.dto.DynamicConfigDTO;
import org.apache.dubbo.admin.model.store.OverrideDTO;
import org.apache.dubbo.common.utils.StringUtils;

public class DynamicConfigDTO2OverrideDTOAdapter extends OverrideDTO {

  public DynamicConfigDTO2OverrideDTOAdapter() {
  }

  public DynamicConfigDTO2OverrideDTOAdapter(DynamicConfigDTO dynamicConfigDTO) {
    if (StringUtils.isNotEmpty(dynamicConfigDTO.getApplication())) {
      setScope(Constants.APPLICATION);
      setKey(dynamicConfigDTO.getApplication());
    } else {
      setScope(Constants.SERVICE);
      setKey(dynamicConfigDTO.getService());
    }
    setConfigVersion(dynamicConfigDTO.getConfigVersion());
    setConfigs(dynamicConfigDTO.getConfigs());
  }
}
