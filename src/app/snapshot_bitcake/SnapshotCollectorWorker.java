package app.snapshot_bitcake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import servent.message.snapshot.SumMessage;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class.
 * 
 * @author stefanGT44
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	public volatile boolean resultsReady = false;
	public volatile boolean summingResults = false;
	public volatile boolean borderResultsReady = false;
	
	public int finalSum = 0;
	public Object sumLock = new Object();
	public Map<Integer, Integer> gatheredResults = new HashMap<>();
	
	//<runda<master, pristigao odgovor>
	
	public boolean testForChangeInRound = false;
	public List<Integer> initiators = new ArrayList<>();
	public List<Integer> repliedInitiators = new ArrayList<>();
	public List<Integer> changes = new ArrayList<>();
	public List<Integer> blank = new ArrayList<>();
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	
	//za trenutno prikupljanje rezultata
	private Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();
	
	private List<LYSnapshotResult> border = new ArrayList<>();
	
	//istorija kanala (TRANZIT)
	private Map<String, Integer> channelHistory = new HashMap<>();
	
	private LaiYangBitcakeManager bitcakeManager;

	public SnapshotCollectorWorker() {
		bitcakeManager = new LaiYangBitcakeManager();
	}
	
	@Override
	public LaiYangBitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			System.out.println("Pocinjem da trazim [" + AppConfig.myServentInfo.getId() + "]");
			
			//1 send asks
			((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this, AppConfig.snapshotID.get(), null);
			
			//2 wait for responses or finish
			boolean waiting = true;
			System.out.println("ceka se");
			while (waiting) {
				System.out.println("ceka se OD KOMSIJA (SVA DECA VRACAJU ODGOVOR - ILI REZULTAT ILI DA ODBACUJU RODITELJA :()");
				// komsije + ja 
				if (resultsReady) {
					waiting = false;
				}
				
				//TEST RADIM
				
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			//print
			int sum;
			sum = 0;
			for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
				sum += nodeResult.getValue().getRecordedAmount();
				AppConfig.timestampedStandardPrint(
						"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
			}
			
			for (int i = 0; i < AppConfig.getServentCount(); i++) {
				if (!collectedLYValues.containsKey(i)) continue;
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					//PROMENA IFA
					if (i == j || (!collectedLYValues.containsKey(j) && !borderContains(j))) continue; //ovim preskacemo u border kanalima, treba ispraviti
					//ako su komsije
					if (AppConfig.getInfoById(i).getNeighbors().contains(j) && AppConfig.getInfoById(j).getNeighbors().contains(i)) {
						//if (!collectedLYValues.containsKey(j) && !border.contains(j))
							//OVDE
						System.out.println();
						//OVDE SAM STAO
						String key = i + "->" + j;
						
						int i_sent_j = collectedLYValues.get(i).getGiveHistory().get(j);
						AppConfig.timestampedStandardPrint(""+ i + " sent to " + j + " = " + i_sent_j);
						int j_gotFrom_i = 0;
						if (collectedLYValues.containsKey(j))
							j_gotFrom_i = collectedLYValues.get(j).getGetHistory().get(i);
						else
							j_gotFrom_i = getEdgeResult(j).getGetHistory().get(i);
						AppConfig.timestampedStandardPrint(""+ j + " got from " + i + " = " + j_gotFrom_i);
						
						int transit = 0;
						if (channelHistory.containsKey(key)) {
							transit = channelHistory.get(key);
						}
						AppConfig.timestampedStandardPrint("Old tranzit for " + i + "->"+j+" = " + transit);
						
						transit = transit + i_sent_j - j_gotFrom_i;
						sum += transit; 
						
						AppConfig.timestampedStandardPrint("New Tranzit for " + i + "->"+j+" = " + transit);
						System.out.println();
						
						channelHistory.put(key, transit);
						
					}
				}
			}
			
			/*for(int i = 0; i < AppConfig.getServentCount(); i++) {
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					if (i != j) {
						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
							int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);
							int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);
							
							
							
							if (ijAmount != jiAmount) {
								String outputString = String.format(
										"Unreceived bitcake amount: %d from servent %d to servent %d",
										ijAmount - jiAmount, i, j);
								AppConfig.timestampedStandardPrint(outputString);
								sum += ijAmount - jiAmount;
							}
						}
					}
				}
			}*/
			
			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
			AppConfig.timestampedStandardPrint("Cvorovi u mom regionu: " + collectedLYValues.entrySet());
			AppConfig.timestampedStandardPrint("Suma za region[" + AppConfig.myServentInfo.getId() + "]: " + sum);
			AppConfig.timestampedStandardPrint("Summing results with other regions...");
			
			if (border.size() == 0) {
				collectedLYValues.clear(); //reset for next invocation
				collecting.set(false);
				resultsReady = false;
				border.clear();
				bitcakeManager.sendResetToChildren();
				bitcakeManager.resetSKparameters();
				continue;
			}
			
			AppConfig.timestampedStandardPrint("Pocinje treci deo");
			synchronized(sumLock) {
			
				finalSum += sum;
				gatheredResults.put(AppConfig.myServentInfo.getId(), sum);
			
				//saljemo svim svojim susednim regionima nas rezultat
				for (LYSnapshotResult result: border) {
					if (!initiators.contains(result.getMaster())) initiators.add(result.getMaster());
				}

				if (repliedInitiators.size() != initiators.size()) {
					AppConfig.timestampedStandardPrint("Nisu mi svi javili dok sam ja pocinjao, saljem svima SVOJ REZULTAT_________________________________________");
					
					//inicijalan nas rezultat saljemo
					Map<Integer, Integer> newMap = new HashMap<>();
					newMap.put(AppConfig.myServentInfo.getId(), sum);
					for (Integer initiator: initiators) {
						SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(initiator), newMap);
							MessageUtil.sendMessage(msg);
						}
				} else {
					//ukoliko smo vec dobili odgovor od komsija onda 
					
					repliedInitiators.clear();
						
					for (Integer change: changes) {
						if (!initiators.contains(change))
							initiators.add(change);
					}
					
					AppConfig.timestampedStandardPrint("SVI komsijski regioni su mi se javili dok sam ja pocinjao, saljem im SVE____________________________________________");
					System.out.println(gatheredResults.keySet() + " saljem ovima");
					System.out.println(initiators + " inicijatori");
					System.out.println(changes + " changes");
						
					for (Entry<Integer, Integer> region: gatheredResults.entrySet()) {
						if (region.getKey() == AppConfig.myServentInfo.getId()) continue;
						SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(region.getKey()), gatheredResults);
						MessageUtil.sendMessage(msg);
					}
						
					
					//gotova runda, resetovanje
					testForChangeInRound = false;
					changes.clear();
					blank.clear();
					
				}
			
				//for (Integer initiator: initiators) {
				//SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(initiator), gatheredResults);
				//MessageUtil.sendMessage(msg);
				//}
			
				//check if ready, onda kraj, else vrti se u pelji
			}
			
			summingResults = true;
			AppConfig.timestampedStandardPrint("KRECEM DA CEKAM ALGORITAM____________________________________________");
			while (summingResults) {
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) return;
				
			}
			
			AppConfig.timestampedStandardPrint("ZAVRSENO SKUPLJANJE REZULTATA OD OSTALIH INICIJATORA");
			AppConfig.timestampedStandardPrint("**DONE-!-!-!-!-!-!**DONE-!-!-!-!-!-!-!-!-**DONE! 	Bitcake count = " + finalSum + "	 !DONE**-!-!-!-!-!-!-!**DONE-!-!-!-!-!-!-!-**DONE");
			
			bitcakeManager.sendResetToChildren();
			bitcakeManager.resetSKparameters();
			
			collectedLYValues.clear(); //reset for next invocation
			collecting.set(false);
			resultsReady = false;
			border.clear();
		}

	}
	
	public void handleSum(Map<Integer, Integer> map, int senderId, String messageText) {
		if (map == null) {
			AppConfig.timestampedStandardPrint("DOBILI SMO SUM NULL____________________________________________" + senderId);
			repliedInitiators.add(senderId);
			blank.add(senderId);
			
			if (repliedInitiators.size() == initiators.size()) {
				AppConfig.timestampedStandardPrint("TO JE BIO ODGOVOR KOJI NAM JE FALIO U RUNDI____________________________________________" + senderId);
				//krece nova runda
				repliedInitiators.clear();
				
				if (testForChangeInRound) {
					AppConfig.timestampedStandardPrint("IMA PROMENA, SALJE SE SVIM KOMSIJSKIM REGIONIMA GORNJI____________________________________________" + senderId);
					
					for (Integer change: changes) {
						if (!initiators.contains(change))
							initiators.add(change);
					}
					
					for (Entry<Integer, Integer> region: gatheredResults.entrySet()) {
						if (region.getKey() == AppConfig.myServentInfo.getId()) continue;
						SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(region.getKey()), gatheredResults);
						MessageUtil.sendMessage(msg);
					}
					
				} else {
					
					if (blank.size() == initiators.size()) {
						AppConfig.timestampedStandardPrint("DOBILI BLANKO OD SVIH, KRAJ ALGORITMA____________________________________________" + senderId);
						
						for (Integer initiator: initiators) {
							SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(initiator), null);
							MessageUtil.sendMessage(msg);
						}
						
						//KRAJ ALGORITMA
						summingResults = false;
						initiators.clear();
						gatheredResults.clear();
						blank.clear();
						return;
					} else {
						AppConfig.timestampedStandardPrint("NEMA PROMENA, NISU SVI BLANKO PA SALJEMO DALJE BLANKO____________________________________________" + senderId);
						for (Integer initiator: initiators) {
							SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(initiator), null);
							MessageUtil.sendMessage(msg);
						}
					}
					
				}
				
				//gotova runda, resetovanje
				testForChangeInRound = false;
				changes.clear();
				blank.clear();
			}
			
			
		} else {
			
			//za sve regione od sendera
			for (Entry<Integer, Integer> region: map.entrySet()) {
				//ako ima neki koji NEMAMO
				if (!gatheredResults.containsKey(region.getKey())) {
					gatheredResults.put(region.getKey(), region.getValue());
					finalSum += region.getValue();
					changes.add(region.getKey());
					testForChangeInRound = true;
				}
			}
			//TREBA DODATI I IZNAD PROVERU, U GLAVNOJ METODI AKO SU NAM ODGOVORILI DA ODMA SALJEMO
			repliedInitiators.add(senderId);
			//svi su odgovorili u ovoj rundi, ako ima novina - dodajemo novine u inicijatore, i saljemo ostalima poruku o novini
			
			AppConfig.timestampedStandardPrint("DOBILI ODGOVOR OD " + senderId + "____________________________________________" + senderId);
			AppConfig.timestampedStandardPrint("PROMENE: " + changes + " _____" + senderId);
			if (repliedInitiators.size() == initiators.size()) {
				AppConfig.timestampedStandardPrint("odgovorili svi____________________________________________" + senderId);
				//krece nova runda
				repliedInitiators.clear();
				
				if (testForChangeInRound) {
					
					AppConfig.timestampedStandardPrint("ima promena, saljem svima svoje rezultate____________________________________________" + senderId);
					
					for (Integer change: changes) {
						if (!initiators.contains(change))
							initiators.add(change);
					}
					
					for (Entry<Integer, Integer> region: gatheredResults.entrySet()) {
						if (region.getKey() == AppConfig.myServentInfo.getId()) continue;
						SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(region.getKey()), gatheredResults);
						MessageUtil.sendMessage(msg);
					}
					
				} else {
					AppConfig.timestampedStandardPrint("nema promena, saljem blanko svima____________________________________________" + senderId);
					
					for (Integer initiator: initiators) {
						SumMessage msg = new SumMessage(AppConfig.myServentInfo, AppConfig.getInfoById(initiator), null);
						MessageUtil.sendMessage(msg);
					}
					
				}
				
				//gotova runda, resetovanje
				testForChangeInRound = false;
				changes.clear();
				blank.clear();
			}
			
		}
	}
	
	public boolean borderContains(int id) {
		for (LYSnapshotResult edgeNode: border) {
			if (edgeNode.getServentId() == id) return true;
		}
		return false;
	}
	
	public LYSnapshotResult getEdgeResult(int id) {
		for (LYSnapshotResult edgeNode: border) {
			if (edgeNode.getServentId() == id) return edgeNode;
		}
		return null;
	}
	
	public void addToBorder(List<LYSnapshotResult> borders) {
		this.border.addAll(borders);
	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
	}
	
	public void addAllGatheredResults(List<LYSnapshotResult> resultsToAdd) {
		for (LYSnapshotResult result: resultsToAdd) {
			collectedLYValues.put(result.getServentId(), result);
		}
	}
	
	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
