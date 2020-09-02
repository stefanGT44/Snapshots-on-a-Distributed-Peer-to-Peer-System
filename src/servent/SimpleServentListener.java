package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.ServentInfo;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.LYTellHandler;
import servent.handler.snapshot.RejectCollectorHandler;
import servent.handler.snapshot.RejectParentHandler;
import servent.handler.snapshot.RejectResponseHandler;
import servent.handler.snapshot.ResetHandler;
import servent.handler.snapshot.SumHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
		for (ServentInfo initiator: AppConfig.initiators) {
			queue.put(initiator.getId(), new HashMap<>());
		}
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	//u trenutku kad se bude radio konkurentni snapshot ovde ce morati mapa<initiatorID, List<Message>> red messages
	//private List<Message> redMessages = new ArrayList<>();
	
	//map<inicijatorID, map<snapshotID, List<Message>>>
	private Map<Integer, Map<Integer, List<Message>>> queue = new HashMap<>();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage = null;
				
				/*
				 * Lai-Yang stuff. Process any red messages we got before we got the marker.
				 * The marker contains the collector id, so we need to process that as our first
				 * red message. 
				 */
				
				/*Entry<Integer, List<Message>> test = null;
				int id = 0;
				for (Entry<Integer, Map<Integer, List<Message>>> entry: queue.entrySet()) {
					for (Entry<Integer, List<Message>> redovi: entry.getValue().entrySet()) {
						if (AppConfig.snapshotIDs.get(entry.getKey()) >= redovi.getKey()) {
							clientMessage = redovi.getValue().remove(0);
							if (redovi.getValue().size() == 0) {
								test = redovi;
								id = entry.getKey();
							}
							break;
						}
					}
				}
				
				if (test != null) {
					queue.get(id).remove(test.getKey());
					test = null;
					id = 0;
				}*/
				
				int snapshotToRemove = -1;
				int initiatorID = -1;
				for (Integer initiator: queue.keySet()) {
					for (Integer snapshot: queue.get(initiator).keySet()) {
						if (AppConfig.snapshotIDs.get(initiator) >= snapshot) {
							clientMessage = queue.get(initiator).get(snapshot).remove(0);
							AppConfig.timestampedStandardPrint("Poruka izvadjena iz queue za inicijatora " + initiator + " snapshot id " + snapshot + ": " + clientMessage);
							if (queue.get(initiator).get(snapshot).size() == 0) {
								snapshotToRemove = snapshot;
								initiatorID = initiator;
								break;
							}
						}
					}
					if (snapshotToRemove != -1) break;
				}
				
				if (snapshotToRemove != -1) {
					queue.get(initiatorID).remove(snapshotToRemove);
				}
				
				
				if (clientMessage == null) {
					/*
					 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
					 */
					Socket clientSocket = listenerSocket.accept();
					
					//GOT A MESSAGE! <3
					clientMessage = MessageUtil.readMessage(clientSocket);
					//System.out.println("STIGLA MI PORUKA TIPA " + clientMessage.getMessageType());
				}
				synchronized (AppConfig.colorLock) {
					//if (clientMessage.isWhite() == false && AppConfig.isWhite.get()) {
					Entry<Integer, Integer> entry = clientMessage.compareMSGhigher();
					if (entry != null) {
						/*
						 * If the message is red, we are white, and the message isn't a marker,
						 * then store it. We will get the marker soon, and then we will process
						 * this message. The point is, we need the marker to know who to send
						 * our info to, so this is the simplest way to work around that.
						 */
						
						
						/*if (clientMessage.getMessageType() != MessageType.LY_MARKER) {
							Map<Integer, List<Message>> map = null;
							if (queue.get(entry.getKey()) == null) {
								map = new HashMap<>();
								List<Message> list = new ArrayList<>();
								list.add(clientMessage);
								map.put(entry.getValue(), list);
								queue.put(entry.getKey(), map);
							} else {
								map = queue.get(entry.getKey());
								if (map.get(entry.getValue()) == null) {
									List<Message> list = new ArrayList<>();
									list.add(clientMessage);
									map.put(entry.getValue(), list);
								} else {
									map.get(entry.getValue()).add(clientMessage);
								}
							}
							//redMessages.add(clientMessage);
							continue;
						}*/
						// MOZDA OVDE TREBA NAPRAVITI IZUZETAK ZA DRUGI MARKER DOK SMO JEDAN VEC OBRADILI?
						if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
							System.out.println("Adding message to queue: " + clientMessage);
							//entry KEY je initiatorID
							//entry VALUE je snapshotID u novoj - (crvenoj poruci)
							//poruka se smesta u queue<iniciatorID<snapshotID, LISTA.add(clientMessage)>
							Map<Integer, List<Message>> initiatorMaps = queue.get(entry.getKey());
							List<Message> snapshotLists = initiatorMaps.get(entry.getValue());
							if (snapshotLists == null) {
								snapshotLists = new ArrayList<>();
								snapshotLists.add(clientMessage);
								initiatorMaps.put(entry.getValue(), snapshotLists);
							} else {
								snapshotLists.add(clientMessage);
							}
							continue;
						} else if (clientMessage.getMessageType() == MessageType.LY_MARKER){
							System.out.println("ELSE OD ADDING TO QUEUE msg: " + clientMessage);
							LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();
							lyFinancialManager.markerEvent(Integer.parseInt(clientMessage.getMessageText()), snapshotCollector,
									clientMessage.getSnapshotIDs().get(Integer.parseInt(clientMessage.getMessageText())), clientMessage.getOriginalSenderInfo());
							continue;
						}
					}
				}
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case TRANSACTION:
					messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
					break;
				case LY_MARKER:
					LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();
					lyFinancialManager.markerEvent(Integer.parseInt(clientMessage.getMessageText()), snapshotCollector,
							clientMessage.getSnapshotIDs().get(Integer.parseInt(clientMessage.getMessageText())), clientMessage.getOriginalSenderInfo());
					break;
				case LY_TELL:
					messageHandler = new LYTellHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
					break;
				case REJECT_PARENT_MESSAGE:
					messageHandler = new RejectParentHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
					break;
				case REJECT_COLLECTOR_MESSAGE:
					messageHandler = new RejectCollectorHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
					break;
				case REJECT_RESPONSE_MESSAGE:
					messageHandler = new RejectResponseHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
					break;
				case SUM_MESSAGE:
					messageHandler = new SumHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
					break;
				case RESET_MESSAGE:
					messageHandler = new ResetHandler(clientMessage, (SnapshotCollectorWorker)snapshotCollector);
					break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
