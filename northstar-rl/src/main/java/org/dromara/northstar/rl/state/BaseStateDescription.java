package org.dromara.northstar.rl.state;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class BaseStateDescription {
    @Builder.Default
    private int stateDim = 4;

    @Builder.Default
    private List<String> stateNames = Arrays.asList("openPrice", "highPrice", "lowPrice", "closePrice");
}
