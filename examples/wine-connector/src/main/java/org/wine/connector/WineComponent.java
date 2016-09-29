package org.wine.connector;

import io.fabric8.django.component.connector.DjangoComponent;

public class WineComponent extends DjangoComponent {

    public WineComponent() {
        super("wine", "org.wine.connector.WineComponent");
    }
}