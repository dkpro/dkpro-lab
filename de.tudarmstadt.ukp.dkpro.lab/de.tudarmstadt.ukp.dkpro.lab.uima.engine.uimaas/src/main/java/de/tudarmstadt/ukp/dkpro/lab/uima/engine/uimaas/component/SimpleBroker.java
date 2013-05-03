package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.component;

import org.apache.activemq.broker.BrokerService;
import org.apache.uima.resource.ResourceInitializationException;

public class SimpleBroker extends JmsComponent
{
	private BrokerService broker;

	public SimpleBroker()
	{
		// Nothing to do
	}

	public void start() throws ResourceInitializationException
	{
		try {
			broker = new BrokerService();
			broker.addConnector(getBrokerUrl());
			broker.setPersistent(false);
			broker.start();
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	public void stop() throws ResourceInitializationException
	{
		try {
			broker.stop();
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}
}
