package org.dromara.northstar.copilot.websearch.searxng;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/12/11 21:41
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearXNGRequest {
    @Builder.Default
    private final String format = "json";

    private String q;

    @Builder.Default
    private String engines = "duckduckgo,google,bing,brave,mojeek,presearch,qwant,startpage,yahoo,arxiv,crossref,google_scholar,internetarchivescholar,semantic_scholar";
}
