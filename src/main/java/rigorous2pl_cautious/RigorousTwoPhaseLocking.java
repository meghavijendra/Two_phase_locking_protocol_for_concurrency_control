package rigorous2pl_cautious;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;


class RigorousTwoPhaseLocking {
	
	// Transaction table
	static Map<Integer, Transaction> transactionTableMap = new HashMap<>();

	// Lock table
	static Map<String, Lock> lockTableMap = new HashMap<>();
	static List<String> content = new ArrayList<>();

	public static void main(String[] args) {
		String fileName = "Data/Input 1/input.txt";
		String outputFile = "Data/Input 1/output_cautiouswait.txt";
		String inputLine = null; // reference one line at a time

		int timestamp = 0;

		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((inputLine = bufferedReader.readLine()) != null) {
				// operation = line.charAt(0);
				String line = inputLine.replace(" ", "");
				content.add(line +" - ");
				if (!line.isEmpty()) {
					if (line.charAt(0) == Constants.B.getCharValue()) {
						timestamp = timestamp + 1;
						begin(timestamp, Integer.parseInt(line.substring(1, line.indexOf(";"))));
					} else if (line.charAt(0) == Constants.R.getCharValue() || 
							line.charAt(0) == Constants.W.getCharValue()) {
					request(line.substring(line.indexOf(Constants.OPENBRACKET.getCharValue()) + 1, line.indexOf(')')),
								Integer.parseInt(line.substring(1, line.indexOf('('))), line.charAt(0) + "");
					} else if (line.charAt(0) == Constants.C.getCharValue() || 
							line.charAt(0) == Constants.E.getCharValue()) {
						commit(transactionTableMap.get(Integer.parseInt(line.substring(1, line.indexOf(";")))));
					}
				}

			}
			bufferedReader.close();
			writeFile(outputFile);
			System.out.println("You see the Output file for cautious wait in the path: "+outputFile);
		} catch (Exception ex) {
			content.add("Failed to open File!"+ex);
		}
	}

	private static void writeFile(String outputFile) {
		try {
			 File file = new File(outputFile);
			 if (file.exists() && file.isFile())
			 {
				 file.delete();
				 
			 }
			 file.createNewFile();
		     FileWriter myWriter = new FileWriter(outputFile);
			 for(String cont : content) {
				 myWriter.write(cont);
			 }
			 myWriter.close();
		}catch(Exception e) {
			content.add("Failed to write the output file\n");
		}
		
	}

	// begin transaction
	public static void begin(int timestamp, int transId) {
		Transaction transaction = new Transaction(transId, timestamp, Constants.ACTIVE.getValue());
		transactionTableMap.put(transId, transaction);
		content.add("Transaction " + transId +" has begun and it has been entered in the transaction table with its state as Active and timestamp as "+timestamp +"\n \n");
	}

	// request method which checks the state of the transactions and then calls the
	// desired functions
	public static void request(String dataItem, int transactionID, String op) {
		op = op.equals(Constants.R.getValue()) ? Constants.READ.getValue() : Constants.WRITE.getValue();
		Transaction transaction = transactionTableMap.get(transactionID); // Incoming ID

		// state of the transaction is active then
		if (transaction.state.equals(Constants.ACTIVE.getValue())) {
			active(dataItem, transaction, op);
		}
		// state of the transaction is blocked
		else if (transaction.state.equals(Constants.BLOCK.getValue())) {
			block(dataItem, transaction, op);
		}
		// state of the transaction is aborted
		else if (transaction.state.equals(Constants.ABORT.getValue())) {
			content.add("Transaction " +transaction.transId+ " is aborted\n");
		}
		// state of the transaction is committed
		else if (transaction.state.equals(Constants.COMMIT.getValue())) {
			content.add("Transaction " +transaction.transId+ " is committed\n");
		}
	}

	// if the state of transaction is active
	public static void active(String dataItem, Transaction incomming, String op) {
		if (lockTableMap.containsKey(dataItem)) {
			Lock lock = lockTableMap.get(dataItem);
			if (lock.lockState.equals(Constants.READ.getValue()) && op.equals(Constants.READ.getValue())) {
				lock = rr(dataItem, incomming, lock);
			} else if (lock.lockState.equals(Constants.READ.getValue()) && op.equals(Constants.WRITE.getValue())) {
				lock = rw(dataItem, incomming, lock);
			} else if (lock.lockState.equals(Constants.WRITE.getValue()) && op.equals(Constants.READ.getValue())) {
				lock = wr(dataItem, incomming, lock);
			} else if (lock.lockState.equals(Constants.WRITE.getValue()) && op.equals(Constants.WRITE.getValue())) {
				lock = ww(dataItem, incomming, lock);
			} else if (lock.lockState.equals("") && op.equals(Constants.READ.getValue())) {
				lock.lockState = Constants.READ.getValue();
				lock.readTransactionId.add(incomming.transId);
			} else if (lock.lockState.equals("") && op.equals(Constants.WRITE.getValue())) {
				lock.lockState = Constants.WRITE.getValue();
				content.add("Transaction"+ incomming.transId + " has acquired Write Lock on data item "+dataItem);
				lock.writeLockTransId = incomming.transId;
			}
			lockTableMap.put(dataItem, lock);
		} else {
			Lock lock = null;
			if (op.equals(Constants.READ.getValue())) {
				lock = new Lock(dataItem, op, 0);
				lock.readTransactionId.add(incomming.transId);
				content.add("The transaction state for transaction "+incomming.transId + "  is Active so entry for data item " +dataItem + " has been made in the lock table and transaction" +incomming.transId+ " has acquired Read Lock on it.\n \n");
			} else if (op.equals(Constants.WRITE.getValue())) {
				lock = new Lock(dataItem, op, incomming.transId);
				content.add("The transaction state for transaction "+incomming.transId + " is Active so entry for data item " +dataItem + " has been made in the lock table and transaction" +incomming.transId+ "has acquired Write Lock on it.\n \n");
			}

			if (!incomming.DataItems.contains(dataItem))
				incomming.DataItems.add(dataItem);
			transactionTableMap.put(incomming.transId, incomming);
			lockTableMap.put(dataItem, lock);
		}

	}

	// lock state in lock table is read and the state of incoming transaction is
	// also read
	public static Lock rr(String dataItem, Transaction transaction, Lock lock) {
		lock.readTransactionId.add(transaction.transId);
		if (!transaction.DataItems.contains(dataItem))
			transaction.DataItems.add(dataItem);
		transactionTableMap.put(transaction.transId, transaction);
		content.add("Transaction " +transaction.transId + " has been appended in the read transaction id list for item . Read Lock on " +dataItem+ " has also been acquired \n\n");
		return lock;
	}

	// lock state in lock table is write and the state of incoming transaction is
	// read
	public static Lock wr(String dataItem, Transaction in, Lock lock) {
		// transaction Id is same downgrade the lock
		if (lock.writeLockTransId == in.transId) {
			lock.lockState = Constants.READ.getValue();
			lock.writeLockTransId = 0;
			lock.readTransactionId.add(in.transId);
			if (!in.DataItems.contains(dataItem))
				in.DataItems.add(dataItem);
			transactionTableMap.put(in.transId, in);
			content.add("For the data item " +dataItem + " and transaction ID" +in.transId+ " lock has been downgraded to Read Lock.\n");
		} else {
			Transaction transaction = transactionTableMap.get(lock.writeLockTransId); // locktable ID
			if (transaction.state == Constants.BLOCK.getValue() ) {
				in.state = Constants.ABORT.getValue();
				transactionTableMap.put(in.transId, in);
				content.add(
						"Transaction" +in.transId+ " aborts because the transaction" +transaction.transId+" is in Block state and has read Lock on data item" +dataItem + "\n");
				releaseLock(in, dataItem);

			} else {
				in.state = Constants.BLOCK.getValue();
				in.waitingOperation.add(new Operation(Constants.READ.getValue(), dataItem));

				lock.waitingList.add(in.transId);

				if (!in.DataItems.contains(dataItem))
					in.DataItems.add(dataItem);
				transactionTableMap.put(in.transId, in);
				content.add("Transaction" +in.transId+ "has been blocked and Read operation" +dataItem + " has been added to the waiting operation queue in the transaction table and transaction Id" +in.transId+ " has been added to the waiting list queue in the lock table.\n");
			}
		}

		return lock;
	}

	// lock state in lock table is read and the state of incoming transaction is
	// write
	public static Lock rw(String dataItem, Transaction in, Lock lock) {
		// upgrade the lock
		if (lock.readTransactionId.size() == 1 && lock.readTransactionId.get(0).equals(in.transId)) {
			lock.lockState = Constants.WRITE.getValue();
			lock.readTransactionId.remove(0);
			lock.writeLockTransId = in.transId;
			content.add("For the data item" +dataItem +" and transaction ID" +in.transId+ " lock has been upgraded to Write Lock.\n \n");
		} else if (lock.readTransactionId.size() == 1 && !lock.readTransactionId.get(0).equals(in.transId)) {
			Transaction t1 = transactionTableMap.get(lock.writeLockTransId);//
			if (t1 != null && t1.state  == Constants.BLOCK.getValue()) {
				in.state = Constants.ABORT.getValue();
				transactionTableMap.put(in.transId, in);
				content.add("Transaction" +in.transId + " aborts because of the transaction" +t1.transId+ "is in the Block state and acquires Write Lock on data item" +dataItem+ "\n");
				releaseLock(in, dataItem);

			} else {
				in.state = Constants.BLOCK.getValue();
				in.waitingOperation.add(new Operation(Constants.WRITE.getValue(), dataItem));
				transactionTableMap.put(in.transId, in);
				lock.waitingList.add(in.transId);
				content.add("Transaction " +in.transId + "is blocked and write operation for " +dataItem + " has been added to the waiting operation queue of transaction table and the transactio ID" +in.transId+ "has been added to the waiting list queue of lock table.\n");
			}

		} else if (lock.readTransactionId.size() > 1) {
			List<Integer> readTransactionId = lock.readTransactionId;
			Collections.sort(readTransactionId);
			int first = readTransactionId.get(0);
			if (first == in.transId) {
				content.add("For transaction" +in.transId + "lock for data item" +dataItem+"has been upgraded to write.\n");
				for (int i = 1; i < readTransactionId.size(); i++) {
					Transaction t1 = transactionTableMap.get(readTransactionId.get(i));
					abort(t1);
					content.add(" Transaction" +t1.transId+ " is aborted \n \n");
				}
				lock.readTransactionId.clear();
				lock.writeLockTransId = first;
			} else if (in.transId < first) {
				content.add("Transaction" +in.transId +"has acquired write lock for data item" +dataItem+"\n");

				for (int i = 0; i < readTransactionId.size(); i++) {
					Transaction t1 = transactionTableMap.get(readTransactionId.get(i));
					abort(t1);
					content.add("Aborting transaction"+t1.transId+"\n");
				}
				lock.readTransactionId.clear();
				lock.writeLockTransId = first;
			} else {
				in.state = Constants.BLOCK.getValue();
				in.waitingOperation.add(new Operation(dataItem, Constants.WRITE.getValue()));
				content.add("transaction " +in.transId+ " has been blocked.\n \n");
				int i = 0;
				while (i < readTransactionId.size()) {
					if (in.transId >= readTransactionId.get(i))
						i++;
					else if (in.transId < readTransactionId.get(i)) {
						Transaction t1 = transactionTableMap.get(readTransactionId.get(i));
						readTransactionId.remove(i);
						abort(t1);
						content.add("Aborting transaction " +t1.transId+ "\n\n");
					}
				}
				lock.readTransactionId = readTransactionId;
			}

		}
		if (!in.DataItems.contains(dataItem)) {
			in.DataItems.add(dataItem);
			transactionTableMap.put(in.transId, in);
		}
		return lock;
	}

	// lock state in lock table is write and the state of incoming transaction is
	// also write
	public static Lock ww(String dataItem, Transaction in, Lock lock) {
		Transaction t1 = transactionTableMap.get(lock.writeLockTransId);
		if (t1.state == Constants.BLOCK.getValue()) {
			in.state = Constants.ABORT.getValue();
			transactionTableMap.put(in.transId, in);
			content.add("Transaction" +in.transId +"is aborted because the transaction" +t1.transId +"is in Block state and has acquired write lock for data item" +dataItem+"\n");
			releaseLock(in, dataItem);
		} else {
			in.state = Constants.BLOCK.getValue();
			in.waitingOperation.add(new Operation("Write", dataItem));
			transactionTableMap.put(in.transId, in);
			lock.waitingList.add(in.transId);
			content.add("Transaction"  +in.transId  +"has been blocked and write operation for" +dataItem + " has been added to the waiting operation queue of transaction table and the transactio ID" +in.transId+"has been added to the waiting list queue of lock table. \n");
		}
		if (!in.DataItems.contains(dataItem)) {
			in.DataItems.add(dataItem);
			transactionTableMap.put(in.transId, in);
		}

		return lock;
	}

	// if the transaction is blocked
	public static void block(String dataItem, Transaction in, String op) {
		if (lockTableMap.containsKey(dataItem)) {
			Lock lock = lockTableMap.get(dataItem);
			lock.waitingList.add(in.transId);
			lockTableMap.put(dataItem, lock);
		}
		if (!in.DataItems.contains(dataItem))
			in.DataItems.add(dataItem);
		in.waitingOperation.add(new Operation(op, dataItem));
		transactionTableMap.put(in.transId, in);
		content.add("Transaction "+in.transId + " is in blocked state " +op +" operation on dataitem " +dataItem +" has been added to the \n 	     waiting operation queue of transaction table " +in.transId +" and the transaction ID " +in.transId+ " \n		 has been added to the waiting list queue of lock table.\n \n");
	}

	// when file encounters c
	public static void commit(Transaction in) {

		if (in.state.equals("Active")) {
			content.add("Transaction " +in.transId+ " committed \n \n ");
			in.state = Constants.COMMIT.getValue();
			Queue<String> DataItems = in.DataItems;
			content.add("Locks acquired by transaction "+in.transId+" are released\n \n");
			while (!DataItems.isEmpty()) {
				String d = DataItems.remove();
				releaseLock(in, d);
			}
			content.add("Transaction " +in.transId+ " has been committed and locks have been released\n \n");

			// lock.readTransactionId.remove(in.transId);
			// bring the priority queue element and change the state to of that transaction
			// to active
		} else if (in.state.equals("Block")) {
			in.waitingOperation.add(new Operation("Commit", ""));
			transactionTableMap.put(in.transId, in);
			content.add(
					"Commit operation on transaction "+in.transId+" has been added to the waiting operation\n \n");
		} else if (in.state.equals("Abort")) {
			content.add("Transaction " +in.transId+ " cannot be committed because it has already been aborted.\n \n");
		}
	}

	// after transaction commits or aborts then this method is called
	public static void releaseLock(Transaction in, String dataItem) {

		Lock lock = lockTableMap.get(dataItem);

		if (lock.lockState.equals(Constants.WRITE.getValue()) || lock.readTransactionId.size() == 1) {
			Queue<Integer> wt = lock.waitingList;
			lock.lockState = "";
			if (lock.readTransactionId.size() == 1) {
				lock.readTransactionId.remove(0);
				content.add("Transaction "+in.transId +" has released read lock on " +dataItem+"\n \n");
			} else {
				content.add("Transaction "+in.transId + " has released write lock on " +dataItem+"\n \n");
			}
			lockTableMap.put(dataItem, lock);
			if (wt.isEmpty()) {
				lockTableMap.remove(dataItem);

			} else {
				while (!lock.waitingList.isEmpty()) {
					int tid = lock.waitingList.remove();
					Transaction t = transactionTableMap.get(tid);

					t = acquireLocks(t, dataItem, lock);
					transactionTableMap.put(tid, t);
					if (!t.state.equals(Constants.COMMIT.getValue())) {
						return;
					}
				}
			}

			lockTableMap.remove(dataItem);
		} else if (lock.lockState.equals("Read")) {
			List<Integer> rtids = lock.readTransactionId;
			for (int i = 0; i < rtids.size(); ++i) {
				if (rtids.get(i) == in.transId) {
					rtids.remove(i);
				}
			}
			content.add("Transaction " +in.transId + " read lock on " +dataItem+" has been released \n \n");
			lockTableMap.put(dataItem, lock);
		}
	}

	// after transaction holding the lock is committed to give the lock to the
	// waiting transaction this is called
	public static Transaction acquireLocks(Transaction transaction, String dataItem, Lock lock) {
		Queue<Operation> watingQueue = transaction.waitingOperation;
		transaction.state = Constants.ACTIVE.getValue();//
		transactionTableMap.put(transaction.transId, transaction);//

		if (!watingQueue.isEmpty()) {
			content.add("Transaction" +transaction.transId+ "has been changed from Block to Active\n");
			content.add("Running its waiting operations"+"\n");
		}
		while (!watingQueue.isEmpty()) {
			Operation operation = watingQueue.remove();
			if (operation.operation.equals("Read")) {
				request(operation.dataItem, transaction.transId, Constants.R.getValue());
			} else if (operation.operation.equals(Constants.WRITE.getValue())) {
				request(operation.dataItem, transaction.transId, "w");
			} else if (operation.operation.equals(Constants.COMMIT.getValue())) {
				commit(transaction);

			}

		}

		lockTableMap.put(dataItem, lock);

		return transaction;
	}

	// if the transaction aborts then it releases the locks by calling the release
	// lock function
	public static void abort(Transaction transaction) {
		transaction.state = Constants.ABORT.getValue();
		Queue<String> DataItems = transaction.DataItems;
		content.add("Locks acquired by transaction "+transaction.transId+" are released\n\n");
		while (!DataItems.isEmpty()) {
			String d = DataItems.remove();
			releaseLock(transaction, d);
		}
		transactionTableMap.put(transaction.transId, transaction);
	}
}