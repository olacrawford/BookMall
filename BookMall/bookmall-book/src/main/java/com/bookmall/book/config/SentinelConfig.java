package com.bookmall.book.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SentinelRuleProperties.class)
public class SentinelConfig {

    public SentinelConfig(SentinelRuleProperties properties) {
        FlowRuleManager.loadRules(buildRules(properties));
    }

    private List<FlowRule> buildRules(SentinelRuleProperties properties) {
        List<FlowRule> rules = new ArrayList<>();
        rules.add(rule("/books", properties.getListQps()));
        rules.add(rule("/books/{id}", properties.getDetailQps()));
        rules.add(rule("/books/search", properties.getSearchQps()));
        rules.add(rule("/books/page", properties.getSearchQps()));
        return rules;
    }

    private FlowRule rule(String resource, double qps) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        return rule;
    }
}
