package org.dromara.northstar.copilot.websearch.searxng;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description SearXNG网路搜索配置信息
 * @Date 2024/12/11 23:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearXNGConfig {
    private String url;
    private String engines = "duckduckgo,google,bing,brave,mojeek,presearch,qwant,startpage,yahoo,arxiv,crossref,google_scholar,internetarchivescholar,semantic_scholar";
    private int nums = 20;
}
