package servent.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.ServentInfo;

/**
 * A default message implementation. This should cover most situations.
 * If you want to add stuff, remember to think about the modificator methods.
 * If you don't override the modificators, you might drop stuff.
 * @author stefanGT44
 *
 */
public class BasicMessage implements Message {

	private static final long serialVersionUID = -9075856313609777945L;
	private final MessageType type;
	private final ServentInfo originalSenderInfo;
	private final ServentInfo receiverInfo;
	private final List<ServentInfo> routeList;
	private final String messageText;
	//private final boolean white;
	private final Map<Integer, Integer> snapshotIDs;
	private final boolean changedState;
	
	//This gives us a unique id - incremented in every natural constructor.
	private static AtomicInteger messageCounter = new AtomicInteger(0);
	private final int messageId;
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.routeList = new ArrayList<>();
		this.messageText = "";
		this.snapshotIDs = new HashMap<>();
		changedState = false;
		
		//treba proveriti da ne dodje do deadlock
		//OVAJ KONSTRUKTOR SE POZIVA SAMO UNUTAR LOCKA NAD OBJECT COLOR PA NEMA POTREBE ZA SYNCHRONIZED
		//synchronized (AppConfig.snapshotIDs) {
			this.snapshotIDs.putAll(AppConfig.snapshotIDs);
		//}
		
		this.messageId = messageCounter.getAndIncrement();
	}
	
	public BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
			String messageText) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		//this.white = AppConfig.isWhite.get();
		this.routeList = new ArrayList<>();
		
		this.snapshotIDs = new HashMap<>();
		changedState = false;
		
		//synchronized (AppConfig.snapshotIDs) {
		//OVAJ KONSTRUKTOR SE POZIVA SAMO UNUTAR LOCKA NAD OBJECT COLOR PA NEMA POTREBE ZA SYNCHRONIZED
			this.snapshotIDs.putAll(AppConfig.snapshotIDs);
		//}
		
		this.messageText = messageText;
		
		this.messageId = messageCounter.getAndIncrement();
	}
	
	@Override
	public MessageType getMessageType() {
		return type;
	}

	@Override
	public ServentInfo getOriginalSenderInfo() {
		return originalSenderInfo;
	}

	@Override
	public ServentInfo getReceiverInfo() {
		return receiverInfo;
	}
	
	@Override
	public boolean changedState() {
		return changedState;
	}
	
	/*@Override
	public boolean isWhite() {
		return white;
	}*/
	
	public Map<Integer, Integer> getSnapshotIDs() {
		return snapshotIDs;
	}
	
	@Override
	public Entry<Integer, Integer> compareMaps() {
		for (Entry<Integer, Integer> entry: snapshotIDs.entrySet()) {
			if (entry.getValue() != AppConfig.snapshotIDs.get(entry.getKey()))
				return entry;
		}
		return null;
	}
	
	@Override
	public Entry<Integer, Integer> compareMSGhigher() {
		for (Entry<Integer, Integer> entry: snapshotIDs.entrySet()) {
			if (entry.getValue() > AppConfig.snapshotIDs.get(entry.getKey()))
				return entry;
		}
		return null;
	}
	
	//if is white
	public Entry<Integer, Integer> compareMSGlower() {
		for (Entry<Integer, Integer> entry: snapshotIDs.entrySet()) {
			if (entry.getValue() < AppConfig.snapshotIDs.get(entry.getKey()))
				return entry;
		}
		return null;
	}
	
	@Override
	public List<ServentInfo> getRoute() {
		return routeList;
	}
	
	@Override
	public String getMessageText() {
		return messageText;
	}
	
	@Override
	public int getMessageId() {
		return messageId;
	}
	
	protected BasicMessage(MessageType type, ServentInfo originalSenderInfo, ServentInfo receiverInfo,
			Map<Integer, Integer> snapshotIDs, List<ServentInfo> routeList, String messageText, int messageId) {
		this.type = type;
		this.originalSenderInfo = originalSenderInfo;
		this.receiverInfo = receiverInfo;
		this.routeList = routeList;
		this.messageText = messageText;
		this.snapshotIDs = snapshotIDs;
		this.messageId = messageId;
		this.changedState = true;
	}
	
	/**
	 * Used when resending a message. It will not change the original owner
	 * (so equality is not affected), but will add us to the route list, so
	 * message path can be retraced later.
	 */
	@Override
	public Message makeMeASender() {
		ServentInfo newRouteItem = AppConfig.myServentInfo;
		
		List<ServentInfo> newRouteList = new ArrayList<>(routeList);
		newRouteList.add(newRouteItem);
		Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), getSnapshotIDs(), newRouteList, getMessageText(), getMessageId());
		
		return toReturn;
	}
	
	/**
	 * Change the message received based on ID. The receiver has to be our neighbor.
	 * Use this when you want to send a message to multiple neighbors, or when resending.
	 */
	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			
			Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
					newReceiverInfo, getSnapshotIDs(), getRoute(), getMessageText(), getMessageId());
			
			return toReturn;
		} else {
			AppConfig.timestampedErrorPrint("Trying to make a message for " + newReceiverId + " who is not a neighbor.");
			
			return null;
		}
		
	}
	
	// POZIVA SE SAMO IZ SYNCHRONIZED(COLORLOCK) - nema potrebe za sinhronizacijom
	@Override
	public Message setRedColor() {
		//napuniti mapu
		Map<Integer, Integer> map = new HashMap<>();
		map.putAll(AppConfig.snapshotIDs);
		Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), map, getRoute(), getMessageText(), getMessageId());
		
		return toReturn;
	}
	
	// NE KORISTI SE NIGDE
	@Override
	public Message setWhiteColor() {
		/*Message toReturn = new BasicMessage(getMessageType(), getOriginalSenderInfo(),
				getReceiverInfo(), true, getRoute(), getMessageText(), getMessageId());
		*/
		//return toReturn;
		return null;
	}
	
	/**
	 * Comparing messages is based on their unique id and the original sender id.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BasicMessage) {
			BasicMessage other = (BasicMessage)obj;
			
			if (getMessageId() == other.getMessageId() &&
				getOriginalSenderInfo().getId() == other.getOriginalSenderInfo().getId()) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Hash needs to mirror equals, especially if we are gonna keep this object
	 * in a set or a map. So, this is based on message id and original sender id also.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(getMessageId(), getOriginalSenderInfo().getId());
	}
	
	/**
	 * Returns the message in the format: <code>[sender_id|message_id|text|type|receiver_id]</code>
	 */
	@Override
	public String toString() {
		return "[" + getOriginalSenderInfo().getId() + "|" + getMessageId() + "|" +
					getMessageText() + "|" + getMessageType() + "|" +
					getReceiverInfo().getId() + "]";
	}

	/**
	 * Empty implementation, which will be suitable for most messages.
	 */
	@Override
	public void sendEffect() {
		
	}
}
