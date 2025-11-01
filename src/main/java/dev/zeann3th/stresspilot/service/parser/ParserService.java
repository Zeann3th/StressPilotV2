package dev.zeann3th.stresspilot.service.parser;

import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;

import java.util.List;

public interface ParserService {
    String getType();
    List<EndpointDTO> parse(String spec);
}
