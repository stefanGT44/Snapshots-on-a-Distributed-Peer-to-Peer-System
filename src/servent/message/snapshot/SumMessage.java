package servent.message.snapshot;

import java.util.HashMap;
import java.util.Map;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class SumMessage extends BasicMessage {

	private Map<Integer, Integer> results;
	
	public SumMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, Map<Integer, Integer> results) {
		super(MessageType.SUM_MESSAGE, originalSenderInfo, receiverInfo);
		if (results != null)
			this.results = new HashMap<>(results);
		else
			this.results = results;
	}
	
	@Override
	public Message setRedColor() {
		return new SumMessage(getOriginalSenderInfo(), getReceiverInfo(), results);
	}
	
	public Map<Integer, Integer> getResults() {
		return results;
	}
	
	private static final long serialVersionUID = 1L;

}
