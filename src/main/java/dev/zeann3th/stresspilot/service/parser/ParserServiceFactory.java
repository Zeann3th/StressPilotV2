package dev.zeann3th.stresspilot.service.parser;

import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ParserServiceFactory {

    private final List<ParserService> parsers;

    public ParserService getParser(String type) {
        return parsers.stream()
                .filter(parser -> parser.getType().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> CommandExceptionBuilder.exception(ErrorCode.ENDPOINT_UNSUPPORTED_FORMAT));
    }
}