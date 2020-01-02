package com.gaoshantech.cracker.keystoreweb.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public TaskController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/send")
    @ApiOperation("发送密码计算请求")
    public String send(String key, String data) {
        kafkaTemplate.sendDefault(key, data);
        return "success";
    }
}
