package raftProcedureUnit;

import java.util.ArrayList;
import java.util.List;
import communicationUnit.ConcurrentSocket;
import communicationUnit.Massage;
import communicationUnit.MassageQueue;
import communicationUnit.SendTask;
import communicationUnit.SocketList;
import communicationUnit.ThreadPool;
import serverUnit.LogEntry;
import serverUnit.Node;
import timerUnit.TimerThread;
import util.JSON;

public class MassageProcessThread implements Runnable {

	@Override
	public void run() {
		while (true) {
			// TODO Auto-generated method stub
			Massage massage = MassageQueue.getInstance().get_massage();
			System.out.println(massage.massage);
			List<Object> msg = JSON.JSONToArray(massage.massage);

			int status = Node.getInstance().server.status;
			int msgType = (int) msg.get(0);
			ConcurrentSocket msgSocket = massage.socket;// 获得消息来源的socket

			if (msgType == 5) {
				String ipport = (String) msg.get(1);
				SocketList.getInstance().move_welcomeSocket(msgSocket, ipport);
			}

			if (status == 0) {// 当前是follower
				if (msgType == 0) {
					int term = (int) msg.get(1);
					int lastLogIndex = (int) msg.get(2);
					int lastLogTerm = (int) msg.get(3);

					int myTerm = Node.getInstance().server.currentTerm;
					int myLastLogIndex = Node.getInstance().server.get_lastLogIndex();
					int myLastLogTerm = Node.getInstance().server.get_lastLogTerm();

					List<Object> msg1 = new ArrayList<Object>();// 选票回执
					if (term > myTerm) {
						if (lastLogTerm > myLastLogTerm
								|| lastLogTerm == myLastLogTerm && lastLogIndex >= myLastLogIndex) {
							Node.getInstance().server.currentTerm = term;// 修改自己的term
							TimerThread.getInstance().reset_timer();// 重置计时器
							// 构造选票回执
							msg1.add(1);
							msg1.add(term);
							msg1.add(true);
						} else {
							// 构造选票回执
							msg1.add(1);
							msg1.add(term);
							msg1.add(false);
						}
					} else {
						// 构造选票回执
						msg1.add(1);
						msg1.add(myTerm);
						msg1.add(false);
					}
					String massage1 = JSON.ArrayToJSON(msg1);
					SendTask task = new SendTask(msgSocket, massage1);
					ThreadPool.getInstance().addTasks(task);// 回执选票
				} else if (msgType == 1) {
					// 舍弃不管
				} else if (msgType == 2) {
					// log复制
					int term = (int) msg.get(1);
					int myTerm = Node.getInstance().server.currentTerm;

					List<Object> msg3 = new ArrayList<Object>();
					if (term >= myTerm) {
						Node.getInstance().server.currentTerm = term;// 修改自己的term
						TimerThread.getInstance().reset_timer();// 重置计时器
						// 开始log复制过程
						logCopy(msg, msgSocket);
					} else {
						// 回复改朝换代
						msg3.add(3);
						msg3.add(myTerm);
						msg3.add(false);
						msg3.add(null);
						msg3.add("agingTerm");
						msg3.add(Node.getInstance().nodeId);
						String massage3 = JSON.ArrayToJSON(msg3);
						SendTask task = new SendTask(msgSocket, massage3);
						ThreadPool.getInstance().addTasks(task);// 回复log复制消息
					}

				} else if (msgType == 3) {
					// 舍弃不管
				} else if (msgType == 4) {
					TimerThread.getInstance().reset_timer();// 重置计时器
					++Node.getInstance().server.currentTerm;// 自增自己的term
					Node.getInstance().server.status = 1;// 自己晋升为candidate
					Node.getInstance().server.grantNum = 1;// 重置grantNum，准备选举

					int myTerm = Node.getInstance().server.currentTerm;
					int myLastLogIndex = Node.getInstance().server.get_lastLogIndex();
					int myLastLogTerm = Node.getInstance().server.get_lastLogTerm();

					List<Object> msg0 = new ArrayList<Object>();
					msg0.add(0);
					msg0.add(myTerm);
					msg0.add(myLastLogIndex);
					msg0.add(myLastLogTerm);
					String massage0 = JSON.ArrayToJSON(msg0);
					SocketList.getInstance().broadcast(massage0);// 广播选举消息
				} else if (msgType == 6) {
					List<Object> msg7 = new ArrayList<Object>();
					msg7.add(7);
					msg7.add(false);
					String massage7 = JSON.ArrayToJSON(msg7);
					SendTask task = new SendTask(msgSocket, massage7);
					ThreadPool.getInstance().addTasks(task);// 回复客户端，找错了
				} else if (msgType == 8) {
					List<Object> msg7 = new ArrayList<Object>();
					msg7.add(7);
					msg7.add(false);
					String massage7 = JSON.ArrayToJSON(msg7);
					SendTask task = new SendTask(msgSocket, massage7);
					ThreadPool.getInstance().addTasks(task);// 回复客户端，找错了
				}
			} else if (status == 1) {// 当前是candidate
				if (msgType == 0) {
					int term = (int) msg.get(1);
					int lastLogIndex = (int) msg.get(2);
					int lastLogTerm = (int) msg.get(3);

					int myTerm = Node.getInstance().server.currentTerm;
					int myLastLogIndex = Node.getInstance().server.get_lastLogIndex();
					int myLastLogTerm = Node.getInstance().server.get_lastLogTerm();

					List<Object> msg1 = new ArrayList<Object>();// 选票回执
					if (term > myTerm) {
						if (lastLogTerm > myLastLogTerm
								|| lastLogTerm == myLastLogTerm && lastLogIndex >= myLastLogIndex) {
							Node.getInstance().server.status = 0;// 自己降级为follower
							Node.getInstance().server.currentTerm = term;// 修改自己的term
							TimerThread.getInstance().reset_timer();// 重置计时器
							// 构造选票回执
							msg1.add(1);
							msg1.add(term);
							msg1.add(true);
						} else {
							// 构造选票回执
							msg1.add(1);
							msg1.add(term);
							msg1.add(false);
						}
					} else {
						// 构造选票回执
						msg1.add(1);
						msg1.add(myTerm);
						msg1.add(false);
					}
					String massage1 = JSON.ArrayToJSON(msg1);
					SendTask task = new SendTask(msgSocket, massage1);
					ThreadPool.getInstance().addTasks(task);// 回执选票
				} else if (msgType == 1) {
					int term = (int) msg.get(1);
					boolean grant = (boolean) msg.get(2);
					if (grant) {// 同意自己当leader
						++Node.getInstance().server.grantNum;
						if (Node.getInstance().server.grantNum > Node.getInstance().nodeAddrListSize / 2) {
							Node.getInstance().server.status = 2;// 自己成为leader
							TimerThread.getInstance().reset_leaderTimer();// 重置Leader计时器
							for (int i = 0; i < Node.getInstance().nodeAddrListSize; ++i) {// 初始化nextIndex[]
								Node.getInstance().server.nextIndex[i] = Node.getInstance().server.get_lastLogIndex()
										+ 1;
							}
							System.out.println("i am leader");
						}
					} else {
						if (term > Node.getInstance().server.currentTerm) {
							Node.getInstance().server.status = 0;// 自己降级为follower
							Node.getInstance().server.currentTerm = term;
						}
					}
				} else if (msgType == 2) {
					// log复制
					int term = (int) msg.get(1);
					int myTerm = Node.getInstance().server.currentTerm;

					List<Object> msg3 = new ArrayList<Object>();
					if (term >= myTerm) {
						Node.getInstance().server.status = 0;// 自己降级为follower
						Node.getInstance().server.currentTerm = term;// 修改自己的term
						TimerThread.getInstance().reset_timer();// 重置计时器
						// 开始log复制过程
						logCopy(msg, msgSocket);
					} else {
						// 回复改朝换代
						msg3.add(3);
						msg3.add(myTerm);
						msg3.add(false);
						msg3.add(null);
						msg3.add("agingTerm");
						msg3.add(Node.getInstance().nodeId);
						String massage3 = JSON.ArrayToJSON(msg3);
						SendTask task = new SendTask(msgSocket, massage3);
						ThreadPool.getInstance().addTasks(task);// 回复log复制消息
					}

				} else if (msgType == 3) {
					// 舍弃不管
				} else if (msgType == 4) {
					TimerThread.getInstance().set_timer();// 重设计时器
					++Node.getInstance().server.currentTerm;// 自增term
					Node.getInstance().server.grantNum = 1;// 重置grantNum，准备重新选举

					int myTerm = Node.getInstance().server.currentTerm;
					int myLastLogIndex = Node.getInstance().server.get_lastLogIndex();
					int myLastLogTerm = Node.getInstance().server.get_lastLogTerm();

					List<Object> msg0 = new ArrayList<Object>();
					msg0.add(0);
					msg0.add(myTerm);
					msg0.add(myLastLogIndex);
					msg0.add(myLastLogTerm);
					String massage0 = JSON.ArrayToJSON(msg0);
					SocketList.getInstance().broadcast(massage0);// 广播选举消息
				} else if (msgType == 6) {
					List<Object> msg7 = new ArrayList<Object>();
					msg7.add(7);
					msg7.add(false);
					String massage7 = JSON.ArrayToJSON(msg7);
					SendTask task = new SendTask(msgSocket, massage7);
					ThreadPool.getInstance().addTasks(task);// 回复客户端，找错了
				} else if (msgType == 8) {
					List<Object> msg7 = new ArrayList<Object>();
					msg7.add(7);
					msg7.add(false);
					String massage7 = JSON.ArrayToJSON(msg7);
					SendTask task = new SendTask(msgSocket, massage7);
					ThreadPool.getInstance().addTasks(task);// 回复客户端，找错了
				}
			} else {// 当前是leader
				if (msgType == 0) {
					int term = (int) msg.get(1);
					int lastLogIndex = (int) msg.get(2);
					int lastLogTerm = (int) msg.get(3);

					int myTerm = Node.getInstance().server.currentTerm;
					int myLastLogIndex = Node.getInstance().server.get_lastLogIndex();
					int myLastLogTerm = Node.getInstance().server.get_lastLogTerm();

					List<Object> msg1 = new ArrayList<Object>();// 选票回执
					if (term > myTerm) {
						if (lastLogTerm > myLastLogTerm
								|| lastLogTerm == myLastLogTerm && lastLogIndex >= myLastLogIndex) {
							Node.getInstance().server.status = 0;// 自己降级为follower
							SocketList.getInstance().informClientClientClose();
							Node.getInstance().server.currentTerm = term;// 修改自己的term
							TimerThread.getInstance().reset_timer();// 重置计时器
							// 构造选票回执
							msg1.add(1);
							msg1.add(term);
							msg1.add(true);
						} else {
							// 构造选票回执
							msg1.add(1);
							msg1.add(term);
							msg1.add(false);
						}
					} else {
						// 构造选票回执
						msg1.add(1);
						msg1.add(myTerm);
						msg1.add(false);
					}
					String massage1 = JSON.ArrayToJSON(msg1);
					SendTask task = new SendTask(msgSocket, massage1);
					ThreadPool.getInstance().addTasks(task);// 回执选票
				} else if (msgType == 1) {
					// 舍弃不管
				} else if (msgType == 2) {
					// log复制
					int term = (int) msg.get(1);
					int myTerm = Node.getInstance().server.currentTerm;

					List<Object> msg3 = new ArrayList<Object>();
					if (term >= myTerm) {
						Node.getInstance().server.status = 0;// 自己降级为follower
						SocketList.getInstance().informClientClientClose();
						Node.getInstance().server.currentTerm = term;// 修改自己的term
						TimerThread.getInstance().reset_timer();// 重置计时器
						// 开始log复制过程
						logCopy(msg, msgSocket);
					} else {
						// 回复改朝换代
						msg3.add(3);
						msg3.add(myTerm);
						msg3.add(false);
						msg3.add(null);
						msg3.add("agingTerm");
						msg3.add(Node.getInstance().nodeId);
						String massage3 = JSON.ArrayToJSON(msg3);
						SendTask task = new SendTask(msgSocket, massage3);
						ThreadPool.getInstance().addTasks(task);// 回复log复制消息
					}

				} else if (msgType == 3) {
					// 处理log复制的回复
					int term = (int) msg.get(1);
					boolean success = (boolean) msg.get(2);
					int followerIdx = (int) msg.get(5);
					if (success) {
						int matchIndex = (int) msg.get(3);
						Node.getInstance().server.nextIndex[followerIdx] = matchIndex + 1;
						Node.getInstance().server.matchIndex[followerIdx] = matchIndex;
					} else {
						String failReason = (String) msg.get(4);
						if (failReason.equals("agingTerm")) {// 改朝换代了
							Node.getInstance().server.status = 0;// 自己降级为follower
							SocketList.getInstance().informClientClientClose();
							Node.getInstance().server.currentTerm = term;// 修改自己的term
						} else {
							--Node.getInstance().server.nextIndex[followerIdx];
						}
					}
				} else if (msgType == 4) {
					TimerThread.getInstance().reset_leaderTimer();// 重置leader计时器
					// 发送log复制消息
					int myNodeIdx = Node.getInstance().nodeId;
					for (int i = 0; i < Node.getInstance().nodeAddrListSize; ++i) {
						if (i != myNodeIdx) {
							List<Object> msg2 = new ArrayList<Object>();
							int nextIndex = Node.getInstance().server.nextIndex[i];
							int myLastLogIndex = Node.getInstance().server.get_lastLogIndex();

							int myTerm = Node.getInstance().server.currentTerm;
							int prevLogIndex = nextIndex - 1;
							int prevLogTerm = Node.getInstance().server.get_logByIndex(prevLogIndex).term;
							int leaderCommit = Node.getInstance().server.commitIndex;
							int entriesNum = myLastLogIndex - nextIndex + 1 > 0 ? myLastLogIndex - nextIndex + 1 : 0;
							msg2.add(2);
							msg2.add(myTerm);
							msg2.add(prevLogIndex);
							msg2.add(prevLogTerm);
							msg2.add(leaderCommit);
							msg2.add(entriesNum);
							for (int j = nextIndex; j <= myLastLogIndex; ++j) {
								msg2.add(Node.getInstance().server.get_logByIndex(j).command);
							}
							for (int j = nextIndex; j <= myLastLogIndex; ++j) {
								msg2.add(Node.getInstance().server.get_logByIndex(j).commandId);
							}
							for (int j = nextIndex; j <= myLastLogIndex; ++j) {
								msg2.add(Node.getInstance().server.get_logByIndex(j).term);
							}
							for (int j = nextIndex; j <= myLastLogIndex; ++j) {
								msg2.add(Node.getInstance().server.get_logByIndex(j).index);
							}

							String massage2 = JSON.ArrayToJSON(msg2);
							String remoteAddr = Node.getInstance().get_address(i);
							ConcurrentSocket cs = SocketList.getInstance().querySocket(remoteAddr);
							if (cs != null) {
								SendTask task = new SendTask(cs, massage2);
								ThreadPool.getInstance().addTasks(task);// 发送log复制消息
							}
						}
					}
				} else if (msgType == 6) {
					List<Object> msg7 = new ArrayList<Object>();
					msg7.add(7);
					msg7.add(true);
					String massage7 = JSON.ArrayToJSON(msg7);
					SendTask task = new SendTask(msgSocket, massage7);
					ThreadPool.getInstance().addTasks(task);// 回复客户端，找对了
				} else if (msgType == 8) {
					int myTerm = Node.getInstance().server.currentTerm;
					String command = (String) msg.get(1);
					String commandId = (String) msg.get(2);
					int lastIndex = Node.getInstance().server.get_lastLogIndex();
					SocketList.getInstance().move_clientSocket(msgSocket, commandId);

					LogEntry logEntry = new LogEntry(myTerm, command, commandId, lastIndex+1);
					Node.getInstance().server.log.add(logEntry);// 向log中加入新的logEntry
				}
			}
		}
	}

	private void logCopy(List<Object> msg, ConcurrentSocket msgSocket) {
		int term = (int) msg.get(1);
		int prevLogIndex = (int) msg.get(2);
		int prevLogTerm = (int) msg.get(3);
		int leaderCommit = (int) msg.get(4);
		int entriesNum = (int) msg.get(5);

		if (Node.getInstance().server.get_logByIndex(prevLogIndex) != null
				&& Node.getInstance().server.get_logByIndex(prevLogIndex).term == prevLogTerm) {
			
			Node.getInstance().server.deleteLogEntry(prevLogIndex + 1);// 把自己log中prevLogIndex之后的内容删除掉

			for (int entriesIdx = 6; entriesIdx <= 5 + entriesNum; ++entriesIdx) {
				String command = (String) msg.get(entriesIdx);
				String commandId = (String) msg.get(entriesIdx + entriesNum);
				int logTerm = (int) msg.get(entriesIdx + 2 * entriesNum);
				int index = (int) msg.get(entriesIdx + 3 * entriesNum);
				LogEntry logEntry = new LogEntry(logTerm, command, commandId, index);
				Node.getInstance().server.log.add(logEntry);
			} // 向自己的log中添加新的entries

			// 发送成功消息
			List<Object> msg3 = new ArrayList<Object>();
			msg3.add(3);
			msg3.add(term);
			msg3.add(true);
			msg3.add(Node.getInstance().server.get_lastLogIndex());
			msg3.add(null);
			msg3.add(Node.getInstance().nodeId);
			String massage3 = JSON.ArrayToJSON(msg3);
			SendTask task = new SendTask(msgSocket, massage3);
			ThreadPool.getInstance().addTasks(task);

			if (leaderCommit > Node.getInstance().server.commitIndex) {
				Node.getInstance().server.commitIndex = Math.min(leaderCommit,
						Node.getInstance().server.get_lastLogIndex());
			}
		} else {
			// 回复log不匹配
			List<Object> msg3 = new ArrayList<Object>();
			msg3.add(3);
			msg3.add(term);
			msg3.add(false);
			msg3.add(null);
			msg3.add("noMatching");
			msg3.add(Node.getInstance().nodeId);
			String massage3 = JSON.ArrayToJSON(msg3);
			SendTask task = new SendTask(msgSocket, massage3);
			ThreadPool.getInstance().addTasks(task);
		}
	}

}
