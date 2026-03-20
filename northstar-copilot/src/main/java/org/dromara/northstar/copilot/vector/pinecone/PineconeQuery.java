package org.dromara.northstar.copilot.vector.pinecone;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/14 23:59
 */

@Data
@Builder
public class PineconeQuery {

    /**
     *  命名空间
     */
    @NonNull
    private String namespace;

    /**
     *  需要最相似的前K条向量
     */
    @Builder.Default
    private Integer topK = 10;

    /**
     *  可用于对metadata进行过滤
     */
    private Map<String, String> filter;

    /**
     *  指示响应中是否包含向量值

     */
    @Builder.Default
    private Boolean includeValues = true;

    /**
     *  指示响应中是否包含元数据以及id
     */
    @Builder.Default
    private Boolean includeMetadata = true;

    /**
     *  查询向量
     */
    @NonNull
    private List<Float> vector;

    /**
     *  向量稀疏数据。表示为索引列表和对应值列表，它们必须具有相同的长度
     */
    private Map<String, String> sparseVector;

    /**
     *  每条向量独一无二的id
     */
    private String id;
}
