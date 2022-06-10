/*-
 * ========================LICENSE_START=================================
 * TeamApps Application Server
 * ---
 * Copyright (C) 2020 - 2022 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.application.server.system.sms;


import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.teamapps.application.server.system.config.TwilioConfig;

public class SmsMessage {

	public static String sendSMS(String phoneNumber, String message, TwilioConfig config) {
		Twilio.init(config.getAccountSid(), config.getAuthToken());
		Message msg = Message.creator(new PhoneNumber(phoneNumber), new PhoneNumber(config.getSenderPhoneNumber()), message).create();
		//todo log to db
		return msg.getStatus().toString();
	}

}
