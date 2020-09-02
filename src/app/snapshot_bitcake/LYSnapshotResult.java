package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Snapshot result for servent with id serventId.
 * The amount of bitcakes on that servent is written in recordedAmount.
 * The channel messages are recorded in giveHistory and getHistory.
 * In Lai-Yang, the initiator has to reconcile the differences between
 * individual nodes, so we just let him know what we got and what we gave
 * and let him do the rest.
 * 
 * @author stefanGT44
 *
 */
public class LYSnapshotResult implements Serializable {

	private static final long serialVersionUID = 8939516333227254439L;
	
	private final int serventId;
	private final int master;
	private final int recordedAmount;
	private final Map<Integer, Integer> giveHistory;
	private final Map<Integer, Integer> getHistory;
	
	public LYSnapshotResult(int serventId, int recordedAmount,
			Map<Integer, Integer> giveHistory, Map<Integer, Integer> getHistory, int master) {
		this.serventId = serventId;
		this.recordedAmount = recordedAmount;
		this.giveHistory = new ConcurrentHashMap<>(giveHistory);
		this.getHistory = new ConcurrentHashMap<>(getHistory);
		this.master = master;
	}
	public int getServentId() {
		return serventId;
	}
	public int getRecordedAmount() {
		return recordedAmount;
	}
	public Map<Integer, Integer> getGiveHistory() {
		return giveHistory;
	}
	public Map<Integer, Integer> getGetHistory() {
		return getHistory;
	}
	public int getMaster() {
		return master;
	}
}
