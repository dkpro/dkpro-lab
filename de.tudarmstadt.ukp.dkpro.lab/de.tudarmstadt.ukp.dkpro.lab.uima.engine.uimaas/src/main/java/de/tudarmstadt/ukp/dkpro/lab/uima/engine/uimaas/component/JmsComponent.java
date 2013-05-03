package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.component;

public class JmsComponent
{
	private String brokerUrl = "tcp://localhost:61616";

	public void setBrokerUrl(String aBrokerUrl)
	{
		brokerUrl = aBrokerUrl;
	}

	public String getBrokerUrl()
	{
		return brokerUrl;
	}
}
