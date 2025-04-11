package io.quarkus.ts.opentelemetry;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;

@Unremovable
@ApplicationScoped
public class InMemoryLogRecordExporter implements LogRecordExporter {

    @Override
    public CompletableResultCode export(Collection<LogRecordData> collection) {
        collection.forEach(logRecordData -> {
            if (logRecordData.getAttributes() != null) {
                logRecordData.getAttributes().forEach((k, v) -> {
                    if (v != null && v.toString().contains("LoggingResource")) {
                        Log.infof("Exporting log record - body value %s, severity text %s, severity %s",
                                logRecordData.getBodyValue(), logRecordData.getSeverityText(), logRecordData.getSeverity());
                    }
                });
            }
        });
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
