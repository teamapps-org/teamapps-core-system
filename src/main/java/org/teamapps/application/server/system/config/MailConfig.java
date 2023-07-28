package org.teamapps.application.server.system.config;

import java.util.Arrays;
import java.util.List;

public class MailConfig {

	private String callBackDomain = "example.com";
	private String mailFooter = "footer";
	private String fromName = "the name";

	private List<MailProviderConfig> mailProviders = Arrays.asList(new MailProviderConfig());


	public String getCallBackDomain() {
		return callBackDomain;
	}

	public void setCallBackDomain(String callBackDomain) {
		this.callBackDomain = callBackDomain;
	}

	public String getMailFooter() {
		return mailFooter;
	}

	public void setMailFooter(String mailFooter) {
		this.mailFooter = mailFooter;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public List<MailProviderConfig> getMailProviders() {
		return mailProviders;
	}

	public void setMailProviders(List<MailProviderConfig> mailProviders) {
		this.mailProviders = mailProviders;
	}
}
