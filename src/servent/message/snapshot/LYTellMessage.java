package servent.message.snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

	private static final long serialVersionUID = 3116394054726162318L;

	private List<LYSnapshotResult> results = new ArrayList<>();
	private List<LYSnapshotResult> border = new ArrayList<>();
	
	public LYTellMessage(ServentInfo sender, ServentInfo receiver, List<LYSnapshotResult> lySnapshotResults, List<LYSnapshotResult> border) {
		super(MessageType.LY_TELL, sender, receiver);
		
		this.results.addAll(lySnapshotResults);
		this.border.addAll(border);
	}
	
	private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver, 
			Map<Integer, Integer> snapshotIDs, List<ServentInfo> routeList, String messageText, int messageId,
			List<LYSnapshotResult> results, List<LYSnapshotResult> border) {
		super(messageType, sender, receiver, snapshotIDs, routeList, messageText, messageId);
		this.results = results;
		this.border = border;
	}
	
	public List<LYSnapshotResult> getResults() {
		return results;
	}
	
	public List<LYSnapshotResult> getBorder() {
		return border;
	}
	
	@Override
	public Message setRedColor() {
		Map<Integer, Integer> map = new HashMap<>();
		map.putAll(AppConfig.snapshotIDs);
		Message toReturn = new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
				map, getRoute(), getMessageText(), getMessageId(), results, border);
		return toReturn;
	}
}
