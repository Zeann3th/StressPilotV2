package dev.zeann3th.stresspilot.dto.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.CookieJar;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowThreadContext {
    private int threadId;
    private int iterationCount;
    private CookieJar cookieJar;
    private Map<String, Object> variables;

    public void incrementIteration() {
        this.iterationCount++;
    }
}
