package org.dromara.northstar.copilot.vector.pinecone;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/15 0:00
 */

@Builder
@Data
public class PineconeDelete {
    private List<String> ids;

    private boolean deleteAll;

    private String namespace;

    private Map<String, String> filter;
}
