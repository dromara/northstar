package org.dromara.northstar.copilot.vector.pinecone;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/15 0:05
 */

@Data
public class PineconeQueryResponse {
    private List<String> results;

    /**
     *  匹配的结果
     */
    private List<Match> matches;

    /**
     *  命名空间
     */
    private String namespace;

    @Data
    public static class Match {
        /**
         * 向量id
         */
        private String id;

        /**
         *  相似度分数
         */
        private Float score;

        /**
         *  向量
         */
        private List<Float> values;

        /**
         *  向量的元数据，存放对应文本
         */
        private Map<String, String> metadata;
    }
}
