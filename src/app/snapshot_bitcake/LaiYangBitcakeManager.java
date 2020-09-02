package app.snapshot_bitcake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.LYTellMessage;
import servent.message.snapshot.RejectCollectorMessage;
import servent.message.snapshot.RejectParentMessage;
import servent.message.snapshot.ResetMessage;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}
	
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}
	
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}
	
	//cuva se istorija za sve inicijatore
	private Map<Integer, Map<Integer, Integer>> giveHistory = new ConcurrentHashMap<>();
	private Map<Integer, Map<Integer, Integer>> getHistory = new ConcurrentHashMap<>();
	
	private int master = -1;
	private ServentInfo parent = null;
	private List<LYSnapshotResult> border = new ArrayList<>();
	private Map<Integer, Boolean> waitingBeforeReply = new ConcurrentHashMap<>();
	private List<LYSnapshotResult> resultsToReturn = new ArrayList<>();
	private boolean sentResults = false;
	//this sounds WRONG :D, za reset koristim decu
	private List<Integer> acceptedChildren = new ArrayList<>();
	
	private Map<Integer, LYSnapshotResult> resultsForOtherRegions = new HashMap<>();
	
	
	public LaiYangBitcakeManager() {
		initHistory();
		for (Integer neighbor: AppConfig.myServentInfo.getNeighbors()) {
			waitingBeforeReply.put(neighbor, false);
		}
	}
	
	/*
	 * This value is protected by AppConfig.colorLock.
	 * Access it only if you have the blessing.
	 */
	public int recordedAmount = 0;
	
	
	// TREBA DA RESETUJE HISTORY ZA COLLECTORID NAKON OVOGA
	public void markerEvent(int collectorId, SnapshotCollector snapshotCollector, int snapshotId, ServentInfo sender) {
		synchronized (AppConfig.colorLock) {
			if (sender != null)
				AppConfig.timestampedStandardPrint("MARKER EVENT ZA SENDERA " + sender.getId());
			else {
				AppConfig.timestampedStandardPrint("USO U MARKER ALO");
			}
			//mozda mora da se provere snapshotIDs, nisam siguran
			if (parent != null && master == collectorId) {
				// odgovori mu da je on vec zauzet da bi ga dodao u svoj waitingBeforeReply !!!!!!!!________________________________________________________________________
				if (sender != null)
					AppConfig.timestampedStandardPrint("Odbio sam suseda [" + sender.getId() + "], vec imam roditelja u zajednickom regionu.");
				Message rejectParentMessage = new RejectParentMessage(AppConfig.myServentInfo, sender);
				MessageUtil.sendMessage(rejectParentMessage);
				return;
			}
			
			//AppConfig.isWhite.set(false);
			if (collectorId == AppConfig.myServentInfo.getId()) {
				master = AppConfig.myServentInfo.getId();
				//povecamo sopstveni snapshotID i dodamo ga u listu snapshotIDs
				AppConfig.snapshotIDs.put(collectorId, AppConfig.snapshotID.incrementAndGet());
				System.out.println("App config " + AppConfig.snapshotIDs.get(collectorId));
			}
			else {
				//u svojim snapshotIDs povecamo snapshotCounter od collectorId za 1
				//PRE SK
				//AppConfig.snapshotIDs.put(collectorId, AppConfig.snapshotIDs.get(collectorId) + 1);
				
				if (AppConfig.snapshotIDs.get(collectorId) < snapshotId)
					AppConfig.snapshotIDs.put(collectorId, AppConfig.snapshotIDs.get(collectorId) + 1);
				
				
				if (master != -1) {
					//granica ________________________________________________________________________________________________________________________________
					//ne belezimo da smo primili odgovor dok ne dobijem njegov rezultat
					
					//border.add(collectorId);
					
					//vec imamo mastera, sto znaci da smo svoj rezultat vec uubacili u return listu, pa ih samo getujemo odande
					RejectCollectorMessage rejectMessage = new RejectCollectorMessage(AppConfig.myServentInfo, sender, resultsForOtherRegions.get(collectorId));
					//RejectCollectorMessage rejectMessage = new RejectCollectorMessage(AppConfig.myServentInfo, sender, getResultById(AppConfig.myServentInfo.getId()));
					MessageUtil.sendMessage(rejectMessage);
					
					//reset istorije za cvor
					giveHistory.get(collectorId).put(sender.getId(), 0);
					getHistory.get(collectorId).put(sender.getId(), 0);
					
					//if (sender != null)
					AppConfig.timestampedStandardPrint("Odbio sam suseda [" + sender.getId() + "], pripadam [" + master + "] regionu.");
					
					//waitingBeforeReply.put(sender.getId(), true);
					
					//AKO SMO SAMO OVOG KOMSIJU CEKALI
					//if (readyToSendResult()) {
						
						//Message tellMessage = new LYTellMessage(AppConfig.myServentInfo, parent, resultsToReturn, border);
						//MessageUtil.sendMessage(tellMessage);
						
						//resetSKparameters();
						//AppConfig.timestampedStandardPrint("Svi susedi odgovorili [vec imamo mastera], saljem rezultate roditelju");
					//}
					
					return;
					
				} else {
					master = collectorId;
					parent = sender;
					if (parent != null)
						AppConfig.timestampedStandardPrint("Prvi kontakt, novi master = " + master + ", roditelj = " + parent.getId());
				}
				
				waitingBeforeReply.put(sender.getId(), true);
				
			}
			
			recordedAmount = getCurrentBitcakeAmount();
			
			for (ServentInfo initiator: AppConfig.initiators) {
				LYSnapshotResult snapshotResult = new LYSnapshotResult(
						AppConfig.myServentInfo.getId(), recordedAmount, giveHistory.get(initiator.getId()), getHistory.get(initiator.getId()), master);
				resultsForOtherRegions.put(initiator.getId(), snapshotResult);
			}

			LYSnapshotResult snapshotResult = new LYSnapshotResult(
					AppConfig.myServentInfo.getId(), recordedAmount, giveHistory.get(collectorId), getHistory.get(collectorId), master);
			
			if (collectorId == AppConfig.myServentInfo.getId()) {
				snapshotCollector.addLYSnapshotInfo(AppConfig.myServentInfo.getId(), snapshotResult);
				resultsToReturn.add(snapshotResult);
			} else if (readyToSendResult()){
				//AKO JE LIST
				resultsToReturn.add(snapshotResult);
				Message tellMessage = new LYTellMessage(AppConfig.myServentInfo, sender, resultsToReturn, border);
				MessageUtil.sendMessage(tellMessage);
				sentResults = true;
				
				//resetSKparameters();
				
				//OOOOOO
				giveHistory.get(collectorId).clear();
				getHistory.get(collectorId).clear();
				initHistorySpecific(collectorId);
				//resetujIstorijuZaSvojeKomsije();
				AppConfig.timestampedStandardPrint("Mi smo list, saljemo roditelju rezultat");
				return;
				
			} else {
				resultsToReturn.add(snapshotResult);
			}
			
			//OOOOOOOOO
			//RESET ISTORIJE NAKON SNAPSHOTA
			giveHistory.get(collectorId).clear();
			getHistory.get(collectorId).clear();
			initHistorySpecific(collectorId);
			//resetujIstorijuZaSvojeKomsije();
			
			for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				if (neighbor.equals(collectorId)) continue;
				if (sender != null && neighbor.equals(sender.getId())) continue;
				Message clMarker = new LYMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
				MessageUtil.sendMessage(clMarker);
				try {
					/*
					 * This sleep is here to artificially produce some white node -> red node messages.
					 * Not actually recommended, as we are sleeping while we have colorLock.
					 */
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void addChild(int childId) {
		acceptedChildren.add(childId);
	}
	
	public void sendResetToChildren() {
		for (Integer childId: acceptedChildren) {
			ResetMessage msg = new ResetMessage(AppConfig.myServentInfo, AppConfig.getInfoById(childId));
			MessageUtil.sendMessage(msg);
		}
	}
	
	public void addBorder(LYSnapshotResult result) {
		border.add(result);
	}
	
	public LYSnapshotResult getOtherRegionResult(int master, int sender) {
		giveHistory.get(master).put(sender, 0);
		getHistory.get(master).put(sender, 0);
		return resultsForOtherRegions.get(master);
	}
	
	public LYSnapshotResult getResultById(int id) {
		for (LYSnapshotResult result: resultsToReturn)
			if (result.getServentId() == id) return result;
		return null;
	}
	
	public void sendResultsToParent() {
		if (sentResults) return;
		sentResults = true;
		AppConfig.timestampedStandardPrint("Saljemo rezultate roditelju");
		Message tellMessage = new LYTellMessage(AppConfig.myServentInfo, parent, resultsToReturn, border);
		MessageUtil.sendMessage(tellMessage);
		giveHistory.get(master).clear();
		getHistory.get(master).clear();
		initHistorySpecific(master);
	}
	
	public void resetSKparameters() {
		AppConfig.timestampedStandardPrint("RESETOVANO SK STANJE +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		master = -1;
		parent = null;
		border.clear();
		resultsToReturn.clear();
		sentResults = false;
		acceptedChildren.clear();
		resultsForOtherRegions.clear();
		for (Integer neighbor: AppConfig.myServentInfo.getNeighbors()) {
			waitingBeforeReply.put(neighbor, false);
		}
	}
	
	public ServentInfo getParent() {
		return parent;
	}
	
	private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {
		
		private int valueToAdd;
		
		public MapValueUpdater(int valueToAdd) {
			this.valueToAdd = valueToAdd;
		}
		
		@Override
		public Integer apply(Integer key, Integer oldValue) {
			return oldValue + valueToAdd;
		}
	}
	
	public void addResults(List<LYSnapshotResult> results, List<LYSnapshotResult> borderResults) {
		this.resultsToReturn.addAll(results);
		this.border.addAll(borderResults);
	}
	
	public void markRecievedMessage(Integer sender) {
		this.waitingBeforeReply.put(sender, true);
	}
	
	public void recordGiveTransaction(int neighbor, int amount) {
		for (Entry<Integer, Map<Integer, Integer>> entry: giveHistory.entrySet()) {
			entry.getValue().compute(neighbor, new MapValueUpdater(amount));
		}
	}
	
	public void recordGetTransaction(int neighbor, int amount) {
		for (Entry<Integer, Map<Integer, Integer>> entry: getHistory.entrySet()) {
			entry.getValue().compute(neighbor, new MapValueUpdater(amount));
		}
	}
	
	public boolean readyToSendResult() {
		for (Entry<Integer, Boolean> entry: waitingBeforeReply.entrySet()) {
			if (!entry.getValue())
				return false;
		}
		return true;
	}
	
	private void initHistorySpecific(int initiatorID) {
		Map<Integer, Integer> give = giveHistory.get(initiatorID);
		Map<Integer, Integer> get = getHistory.get(initiatorID);
		for (Integer neighbor: AppConfig.myServentInfo.getNeighbors()) {
			give.put(neighbor, 0);
			get.put(neighbor, 0);
		}
	}
	
	private void initHistory() {
		for (ServentInfo initiator: AppConfig.initiators) {
			giveHistory.put(initiator.getId(), new ConcurrentHashMap<Integer, Integer>());
			getHistory.put(initiator.getId(), new ConcurrentHashMap<Integer, Integer>());
			for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
				giveHistory.get(initiator.getId()).put(neighbor, 0);
				getHistory.get(initiator.getId()).put(neighbor, 0);
			}
		}
	}
}
