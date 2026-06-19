package healthmonitor.listener;

import healthmonitor.model.event.PatientThresholdEvent;
import healthmonitor.service.VitalThresholdEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DomainListener {
    private final VitalThresholdEventPublisher vitalThresholdEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVitalThreshold(PatientThresholdEvent event) {
        vitalThresholdEventPublisher.publishThresholdCreate(event);
    }
}
