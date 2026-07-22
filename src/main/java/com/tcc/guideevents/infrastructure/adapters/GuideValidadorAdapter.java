package com.tcc.guideevents.infrastructure.adapters;

import com.tcc.guideevents.domain.model.GuideEvent;
import com.tcc.guideevents.domain.eventos.GuideValidador;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

// PLACEHOLDER: siempre valida true. En produccion esto debe reemplazarse por una
// integracion real con el servicio de consulta de guias (TMS) que confirme que el
// guideId existe y que el evento no es fraudulento, antes de aceptar y publicar.
@Component
public class GuideValidadorAdapter implements GuideValidador {

    @Override
    public Mono<Boolean> isValid(GuideEvent event) {
        return Mono.just(true);
    }
}
