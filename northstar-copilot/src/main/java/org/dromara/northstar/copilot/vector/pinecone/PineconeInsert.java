package org.dromara.northstar.copilot.vector.pinecone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/14 20:08
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PineconeInsert {
    /**
     *  需要插入的文本的向量库
     */
    private List<PineconeVectors> vectors;

    /**
     *  命名空间，用于区分每个文本
     */
    private String namespace;
}
