package com.tcc.guideevents.domain.eventos;

import com.tcc.guideevents.domain.model.GuideEvent;
import reactor.core.publisher.Mono;

public interface GuideValidador {

    Mono<Boolean> isValid(GuideEvent event);
}
