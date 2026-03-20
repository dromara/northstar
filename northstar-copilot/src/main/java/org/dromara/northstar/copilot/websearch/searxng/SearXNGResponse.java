package org.dromara.northstar.copilot.websearch.searxng;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/12/11 21:39
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearXNGResponse {
    private String query;

    @JsonProperty("number_of_results")
    private String numberOfResults;

    private List<Result> results;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result {
        private String url;
        private String title;
        private String content;
        //@JsonProperty("parsed_url")
        //private List<String> parsedUrl;
        //private Date publishedDate;
        //private List<String> engines;
        //private String category;
        //private float score;
    }
}
