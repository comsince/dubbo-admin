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

package org.apache.dubbo.admin.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.util.internal.StringUtil;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.common.util.Tool;
import org.apache.dubbo.admin.model.domain.Consumer;
import org.apache.dubbo.admin.model.domain.Provider;
import org.apache.dubbo.admin.model.dto.MetricDTO;
import org.apache.dubbo.admin.service.ConsumerService;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.impl.MetrcisCollectServiceImpl;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;



@RestController
@RequestMapping("/api/{env}/metrics")
public class MetricsCollectController {

    Logger logger = LoggerFactory.getLogger(MetricsCollectController.class);

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    @RequestMapping(method = RequestMethod.POST)
    public String metricsCollect(@RequestParam String group, @PathVariable String env) {
        MetrcisCollectServiceImpl service = new MetrcisCollectServiceImpl();
        service.setUrl("dubbo://127.0.0.1:20880?scope=remote&cache=true");

        return service.invoke(group).toString();
    }

    private String getOnePortMessage(String group, String ip, String port, String protocol) {
        MetrcisCollectServiceImpl metrcisCollectService = new MetrcisCollectServiceImpl();
        metrcisCollectService.setUrl(protocol + "://" + ip + ":" + port +"?scope=remote&cache=true");
        String res = metrcisCollectService.invoke(group).toString();
        return res;
    }

    @RequestMapping( value = "/ipAddr", method = RequestMethod.GET)
    public List<MetricDTO> searchService(@RequestParam String ip, @RequestParam String group, @PathVariable String env) {

        Map<String, Map<String,String>> configMap = new HashMap<>();
        addMetricsConfigToMap(configMap, ip);

//         default value
        if (configMap.size() <= 0) {
            Map<String,String> portProtocolMap = new HashMap<>();
            portProtocolMap.put("20880", "dubbo");
            configMap.put(ip,portProtocolMap);
        }
        List<MetricDTO> metricDTOS = new ArrayList<>();
        Map<String,List<String>> ipMetricMap = new HashMap<>();
        for(String metricIp : configMap.keySet()){
            Map<String,String> portMap = configMap.get(metricIp);
            for (String port : portMap.keySet()) {
                String protocol = portMap.get(port);
                try {
                    //不同ip同一端口的数据要合并，这时同一集群的数据
                    String res = getOnePortMessage(group, metricIp, port, protocol);
                    logger.info("ip:"+metricIp+" metric port:"+port+" protocol "+protocol);
                    if(!ipMetricMap.containsKey(port)){
                        List<String> metricResList = new ArrayList<>();
                        metricResList.add(res);
                        ipMetricMap.put(port,metricResList);
                    } else {
                        ipMetricMap.get(port).add(res);
                    }


                } catch (Exception e){
                    logger.error("ip:"+metricIp+" metric port: "+port+" protocol "+protocol,e);
                }

            }
        }

        for(String metricPort : ipMetricMap.keySet()){
            List<String> metricRes = ipMetricMap.get(metricPort);
            if(metricRes.size() == 1){
                metricDTOS.addAll(new Gson().fromJson(metricRes.get(0), new TypeToken<List<MetricDTO>>(){}.getType()));
            } else {
                List<MetricDTO> firstDtos = new Gson().fromJson(metricRes.get(0), new TypeToken<List<MetricDTO>>(){}.getType());
                for(int i = 1 ; i< metricRes.size();i++){
                    List<MetricDTO> metricDTOList = new Gson().fromJson(metricRes.get(i), new TypeToken<List<MetricDTO>>(){}.getType());
                    for(MetricDTO m0 : metricDTOList){
                        if("dubbo.provider.method.qps".equals(m0.getMetric()) || "dubbo.provider.qps".equals(m0.getMetric())
                            || "dubbo.consumer.qps".equals(m0.getMetric())){
                            for(MetricDTO firstDto : firstDtos){
                                if(m0.equals(firstDto)){
                                    double finalValue = (Double.valueOf(firstDto.getValue().toString()) + Double.valueOf(m0.getValue().toString()));
                                    firstDto.setVaule(finalValue);
                                    break;
                                }
                            }
                        }
                    }

                }
                metricDTOS.addAll(firstDtos);
            }
        }


        return metricDTOS;
    }

    protected void addMetricsConfigToMap(Map<String, Map<String,String>> configMap, String ip) {
        List<Provider> providers;
        if(Constants.ANY_VALUE.equals(ip)){
            providers = providerService.findAll();
        } else {
            providers = providerService.findByAddress(ip);
        }
        if(providers != null){
            for(Provider provider : providers){
                String service = provider.getService();
                MetadataIdentifier providerIdentifier = new MetadataIdentifier(Tool.getInterface(service), Tool.getVersion(service), Tool.getGroup(service),
                        Constants.PROVIDER_SIDE, provider.getApplication());
                String metaData = providerService.getProviderMetaData(providerIdentifier);
                FullServiceDefinition providerServiceDefinition = new Gson().fromJson(metaData, FullServiceDefinition.class);
                Map<String, String> parameters = providerServiceDefinition.getParameters();
                if(!StringUtil.isNullOrEmpty(parameters.get(Constants.METRICS_PORT))){
                    String metricIp = provider.getAddress();
                    metricIp = metricIp.split(":")[0];
                    if(!configMap.containsKey(metricIp)){
                       Map<String,String> portMap = new HashMap<>();
                       portMap.put(parameters.get(Constants.METRICS_PORT), parameters.get(Constants.METRICS_PROTOCOL));
                       configMap.put(metricIp,portMap);
                    } else {
                        configMap.get(metricIp).put(parameters.get(Constants.METRICS_PORT), parameters.get(Constants.METRICS_PROTOCOL));
                    }

                }
            }
        }
        List<Consumer> consumers;
        if(Constants.ANY_VALUE.equals(ip)){
            consumers = consumerService.findAll();
        } else {
            consumers = consumerService.findByAddress(ip);
        }
        if(consumers != null){
            for(Consumer consumer : consumers){
                String service = consumer.getService();
                MetadataIdentifier consumerIdentifier = new MetadataIdentifier(Tool.getInterface(service), Tool.getVersion(service), Tool.getGroup(service),
                        Constants.CONSUMER_SIDE, consumer.getApplication());
                String metaData = consumerService.getConsumerMetadata(consumerIdentifier);
                Map<String, String> consumerParameters = new Gson().fromJson(metaData, Map.class);
                if(!StringUtil.isNullOrEmpty(consumerParameters.get(Constants.METRICS_PORT))){
                    String metricIp = consumer.getAddress();
                    if(!configMap.containsKey(metricIp)){
                        Map<String,String> portMap = new HashMap<>();
                        portMap.put(consumerParameters.get(Constants.METRICS_PORT), consumerParameters.get(Constants.METRICS_PROTOCOL));
                        configMap.put(metricIp,portMap);
                    } else {
                        configMap.get(metricIp).put(consumerParameters.get(Constants.METRICS_PORT), consumerParameters.get(Constants.METRICS_PROTOCOL));
                    }
                }
            }
        }
    }
}
