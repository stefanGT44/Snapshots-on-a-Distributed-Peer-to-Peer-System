package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollectorWorker;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.SumMessage;

public class SumHandler implements MessageHandler {

	private Message clientMessage;
	private SnapshotCollectorWorker snapshotCollector;
	
	public SumHandler(Message clientMessage, SnapshotCollectorWorker snapshotCollector) {
		this.clientMessage = clientMessage;
		this.snapshotCollector = snapshotCollector;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SUM_MESSAGE) {
			SumMessage sum = (SumMessage)clientMessage;
			synchronized(snapshotCollector.sumLock) {
				if (!snapshotCollector.summingResults) return;
				snapshotCollector.handleSum(sum.getResults(), sum.getOriginalSenderInfo().getId(), sum.getMessageText());
			}
		} else {
			AppConfig.timestampedErrorPrint("SumHandler got: " + clientMessage);
		}
	}
	
}
