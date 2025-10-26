package dev.zeann3th.stresspilot.dto.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowConditionDTO {
    private String expression;
    private String next;
}
