package com.example.demo.transit.config;

import com.example.demo.transit.service.TransitGraphFactory;
import org.springframework.stereotype.Component;

/** Small indirection so config classes don't depend on the service package directly. */
@Component
public class TransitGraphInvalidator {

    private final TransitGraphFactory graphFactory;

    public TransitGraphInvalidator(TransitGraphFactory graphFactory) {
        this.graphFactory = graphFactory;
    }

    public void invalidate() {
        graphFactory.invalidate();
    }
}
