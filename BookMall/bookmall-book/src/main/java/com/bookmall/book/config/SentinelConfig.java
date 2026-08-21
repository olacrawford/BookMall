package com.bookmall.book.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel限流规则配置类
 * 代码方式定义限流规则，针对listBooks资源做QPS限流；
 * 也可以不在代码写，直接在Sentinel控制台网页配置规则
 */
@Configuration // 标识为Spring配置类，项目启动会加载该类
public class SentinelConfig {

    /**
     * @PostConstruct：对象创建完成、依赖注入完毕之后，自动执行这个initFlowRules初始化方法
     * 作用：项目启动成功后立刻加载Sentinel限流规则
     */
    @PostConstruct
    public void initFlowRules() {
        // 存放多条限流规则的集合，可以配置多个资源的限流
        List<FlowRule> rules = new ArrayList<>();

        // 创建一条流控规则对象
        FlowRule rule = new FlowRule();
        // 指定规则针对的资源名，和service层@SentinelResource(value = "listBooks")保持一致
        rule.setResource("listBooks");
        // 限流模式：FLOW_GRADE_QPS 按照每秒请求数限流（QPS）
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1); // QPS阈值：每秒最多允许1次请求；超过就触发限流，执行blockHandler兜底方法
        rules.add(rule);

        // 将规则加载到Sentinel管理器，规则正式生效
        FlowRuleManager.loadRules(rules);
    }
}
