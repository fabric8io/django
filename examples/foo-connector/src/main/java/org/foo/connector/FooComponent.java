package org.foo.connector;

import io.fabric8.django.component.connector.DjangoComponent;

public class FooComponent extends DjangoComponent {

	public FooComponent() {
	    super("foo", "org.foo.connector.FooComponent");
	}
}