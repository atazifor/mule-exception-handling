package com.kiktek.listeners;

import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;

public class UncheckedExceptionNotificationListener implements
		ExceptionNotificationListener<ExceptionNotification> {

	@Override
	public void onNotification(ExceptionNotification notification) {
		//@see org.mule.context.notification.ExceptionNotification.ExceptionNotification(Throwable) - uses private method getExceptionCause
		if ("java.lang.RuntimeException".equals(notification.getResourceIdentifier())) {
			//System.out.println("RuntimeException exception occurred");
		}
	}

}
