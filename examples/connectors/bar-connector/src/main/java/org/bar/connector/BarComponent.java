package org.bar.connector;

import io.fabric8.django.component.connector.DjangoComponent;

public class BarComponent extends DjangoComponent {

	public BarComponent() {
		super("bar", "org.bar.connector.BarComponent");
	}
}