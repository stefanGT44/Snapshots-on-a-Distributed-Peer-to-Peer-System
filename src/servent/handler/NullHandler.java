package servent.handler;

import app.AppConfig;
import servent.message.Message;

/**
 * This will be used if no proper handler is found for the message.
 * @author stefanGT44
 *
 */
public class NullHandler implements MessageHandler {

	private final Message clientMessage;
	
	public NullHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		AppConfig.timestampedErrorPrint("Couldn't handle message: " + clientMessage);
	}

}
