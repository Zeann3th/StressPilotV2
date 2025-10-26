package dev.zeann3th.stresspilot.service.parser;

import dev.zeann3th.stresspilot.dto.endpoint.ParsedEndpointDTO;

import java.util.List;

public interface ParserService {
    String getType();
    List<ParsedEndpointDTO> parse(String spec);
}
