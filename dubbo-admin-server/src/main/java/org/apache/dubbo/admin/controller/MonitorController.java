package org.apache.dubbo.admin.controller;


import org.apache.dubbo.admin.common.CommonResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author comsicne
 *         Copyright (c) [2019] [Meizu.inc]
 * @Time 19-2-20 上午10:44
 **/
@RestController
@RequestMapping("monitor")
public class MonitorController {

    @RequestMapping("alive")
    public CommonResponse active(){
        return CommonResponse.createCommonResponse().putData("code",200);
    }
}
