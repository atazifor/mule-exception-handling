package com.kiktek.mule.exceptions.tests;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.tck.junit4.FunctionalTestCase;

public class MuleExceptionTestSuite extends FunctionalTestCase {

	//test-connectors has in-memory connectors used for testing
	//mule-exceptions contain actual application flows
	protected String getConfigResources() {
		return "test-connectors.xml, mule-exceptions.xml";
	}
	
	CountDownLatch defaultExceptionLatch;
	@Override
	protected void doSetUp() throws Exception {
		super.doSetUp();
		defaultExceptionLatch = new CountDownLatch(1);
		muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>(){
			public void onNotification(ExceptionNotification notification) {
				if ("java.lang.RuntimeException".equals(notification.getResourceIdentifier())) {
					defaultExceptionLatch.countDown();//if runtime exception is thrown, count down
				}
			}
		});
	}
	@Test
	public void testDefaultException() throws Exception{
		MuleClient client = muleContext.getClient(); //get the mule client
		client.dispatch("jms://default.in", "default message", null);
		MuleMessage message = client.request("jms://default.out", 500 * getTimeoutSystemProperty());//time in milliseconds
		assertNull(message);
		//@see org.mule.tck.junit4.AbstractMuleTestCase.getTestTimeoutSecs()
		assertTrue(defaultExceptionLatch.await(getTestTimeoutSecs(), TimeUnit.SECONDS));
	}
}
