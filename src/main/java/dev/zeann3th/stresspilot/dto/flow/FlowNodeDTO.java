package dev.zeann3th.stresspilot.dto.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowNodeDTO {
    private String id;
    private String type;
    private Long endpointId;
    private String preProcessor;
    private String postProcessor;
    private String next;
    private List<FlowConditionDTO> conditions;
}
