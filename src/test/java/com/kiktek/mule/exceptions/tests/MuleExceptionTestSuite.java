package com.kiktek.mule.exceptions.tests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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
	public static final String ORIGINAL_MESSAGE = "The original message sent";
	public static final String FINAL_MESSAGE = ORIGINAL_MESSAGE+ " AND SOME";
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
	
	/**
	 * Default exception: error thrown is logged and the message is not re-processed.
	 * @throws Exception
	 */
	@Test
	public void testDefaultException() throws Exception{
		MuleClient client = muleContext.getClient(); //get the mule client
		client.dispatch("jms://default.in", ORIGINAL_MESSAGE, null);
		MuleMessage message = client.request("jms://default.out", 500 * getTimeoutSystemProperty());//time in milliseconds
		assertNull(message);//message never gets to the out queue because an error is thrown
		//@see org.mule.tck.junit4.AbstractMuleTestCase.getTestTimeoutSecs()
		assertTrue(defaultExceptionLatch.await(getTestTimeoutSecs(), TimeUnit.SECONDS)); //latch counts down in the listener since runtime exception is thrown
	}
	
	
	/**
	 * Global default exception strategy must use one of catch, rollback or choice exception strategies
	 * This implementation of global default exception uses catch exception strategy. <br>
	 * The message that was being processed in any flow (that does not explicitly have
	 * a an exception strategy in the flow itself) before an exception occurred is passed to the catch block
	 */
	@Test
	public void testCatchException() throws Exception{
		MuleClient client = muleContext.getClient(); //get the mule client
		client.dispatch("jms://catch.in", ORIGINAL_MESSAGE, null);
		MuleMessage message = client.request("jms://catch.out", 500 * getTimeoutSystemProperty());//time in milliseconds
		assertNull(message);//message never gets to the out queue because an error is thrown
		assertTrue(defaultExceptionLatch.await(getTestTimeoutSecs(), TimeUnit.SECONDS)); //latch counts down in the listener since runtime exception is thrown
		
		message = client.request("jms://default.error", 500 * getTimeoutSystemProperty());//time in milliseconds
		assertNotNull(message); //a message is received on the error queue in the catch block
		assertThat(ORIGINAL_MESSAGE, not(equalTo(message.getPayloadAsString())));
		System.out.println("message.getExceptionPayload().getMessage()" + message.getPayloadAsString());
		assertEquals(FINAL_MESSAGE, message.getPayloadAsString());
		

	}
}
