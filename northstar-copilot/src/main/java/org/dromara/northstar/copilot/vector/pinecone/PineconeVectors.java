package org.dromara.northstar.copilot.vector.pinecone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/14 20:07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PineconeVectors {
    /**
     *  每条向量的id
     */
    private String id;

    /**
     *  分段后每一段的向量
     */
    private List<Float> values;

    /**
     * 向量稀疏数据。表示为索引列表和对应值列表，它们必须具有相同的长度。
     */
    //private Map<String, String> sparseValues;

    /**
     *  元数据，可以用来存储向量对应的文本 { key: "content", value: "对应文本" }
     */
    private Map<String, String> metadata;

}
