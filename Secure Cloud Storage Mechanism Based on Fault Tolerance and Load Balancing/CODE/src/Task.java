import java.text.SimpleDateFormat;
import java.util.Date;


public class Task implements Comparable<Task> {
	String user;
	String machine;
	int taskId;
	int numTasks;
	int taskLength;
	int taskBW;
	int taskRAM;
	int deadLine;
	String date;
	
	public static boolean fuzzyMode = false;
	
	@Override
	public int compareTo(Task t) {
		if(fuzzyMode == false) {
			return ((this.taskLength) > (t.taskLength) ? -1 : 
				((this.taskLength) == (t.taskLength) ? 0 : 1));	
		} else {
			float fuzzyFactor1 = this.numTasks*this.taskLength;
			float fuzzyFactor2 = t.numTasks*t.taskLength;
			
			return ((fuzzyFactor1) > (fuzzyFactor2) ? -1 : 
				((fuzzyFactor1) == (fuzzyFactor2) ? 0 : 1));
		}
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return numTasks + "," + taskLength;
	}
	
	@Override
	public boolean equals(Object arg0) {
		// TODO Auto-generated method stub
		Task t = (Task)arg0;
		return (this.taskId == t.taskId);
	}
	
	public Date getTaskDate() {
		String sDate1=date; 
		try {
			Date parsedDate = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse(sDate1);
		    return parsedDate;	
		} catch(Exception ex) {
			return (new Date());
		}
	}
}
