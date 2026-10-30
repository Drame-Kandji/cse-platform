package sn.cse.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

/**
 * Fabrique de ProblemDetail conforme RFC 7807.
 */
public final class ProblemDetailFactory {
    private ProblemDetailFactory() {}

    public static ProblemDetail create(HttpStatus status, String title, String detail, String code, String traceId) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://cse.sn/errors/" + code.toLowerCase()));
        pd.setProperty("code", code);
        pd.setProperty("traceId", traceId);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
