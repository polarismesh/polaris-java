package com.tencent.polaris.plugins.connector.consul.service.authority.entity;

import java.io.Serializable;
import java.util.List;

/**
 * TSF微服务鉴权规则
 *
 * @author hongweizhu
 */
public class AuthRule implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -4319194985653367847L;
    /**
     * 规则ID
     */
    private String ruleId;
    /**
     * 规则名称
     */
    private String ruleName;
    /**
     * 是否启用：0：不启用；1：启用
     */
    private String isEnabled;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 更新时间
     */
    private String updateTime;
    /**
     * 微服务于ID
     */
    private String microserviceId;
    /**
     * 命名空间ID
     */
    private String namespaceId;
    /**
     * 标签(Tag)列表
     */
    private List<AuthTag> tags;

    /**
     * 标签(Tag)计算规则
     */
    private String tagProgram;

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(String isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getMicroserviceId() {
        return microserviceId;
    }

    public void setMicroserviceId(String microserviceId) {
        this.microserviceId = microserviceId;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public List<AuthTag> getTags() {
        return tags;
    }

    public void setTags(List<AuthTag> tags) {
        this.tags = tags;
    }

    public String getTagProgram() {
        return tagProgram;
    }

    public void setTagProgram(String tagProgram) {
        this.tagProgram = tagProgram;
    }

    /**
     * 推送Consul前清理rule
     */
    public void clearAuthRule() {
        this.microserviceId = null;
        this.namespaceId = null;
        this.createTime = null;
        this.updateTime = null;
        this.isEnabled = null;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AuthRule{");
        sb.append("ruleId='").append(ruleId).append('\'');
        sb.append(", ruleName='").append(ruleName).append('\'');
        sb.append(", isEnabled='").append(isEnabled).append('\'');
        sb.append(", createTime='").append(createTime).append('\'');
        sb.append(", updateTime='").append(updateTime).append('\'');
        sb.append(", microserviceId='").append(microserviceId).append('\'');
        sb.append(", namespaceId='").append(namespaceId).append('\'');
        sb.append(", tags=").append(tags);
        sb.append(", tagProgram='").append(tagProgram).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
