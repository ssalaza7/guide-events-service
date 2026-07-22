package com.tcc.guideevents.application;

import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.model.ProcessResult;
import reactor.core.publisher.Mono;

public interface ProcessGuideEventUseCase {

    Mono<ProcessResult> process(GuideEvent event);
}
