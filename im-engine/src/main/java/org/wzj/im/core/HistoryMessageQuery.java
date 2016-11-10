package org.wzj.im.core;

/**
 * Created by wens on 16/11/10.
 */
public class HistoryMessageQuery {

    private String groupId ;

    private Long since ;

    private Integer limit ;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Long getSince() {
        return since;
    }

    public void setSince(Long since) {
        this.since = since;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
