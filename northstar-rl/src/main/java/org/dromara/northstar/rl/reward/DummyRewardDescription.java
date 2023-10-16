package org.dromara.northstar.rl.reward;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Data
public class DummyRewardDescription {

    @Builder.Default
    private String RewardType = "dummy";
    
}
