import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class TaskPatternAnalyzer {
	public static ArrayList<Task> applyUserLevelSLA(ArrayList<Task> arrTasks, int slaInterval, long slaTime) {
		ArrayList<Task> arrOutTasks = new ArrayList<Task>();
		int originalSize = arrTasks.size();
		
		while(arrOutTasks.size() != originalSize) {
			//Go to each input task
			for(int count=0;count<arrTasks.size();count++) {
				//Get the task
				Task t = arrTasks.get(count);
				//Check if the task meets SLA
				boolean taskMeetsSLA = true; //Initially mark that the task is meeting SLA
				
				//Check if the number of output tasks are more than the suggested interval
				if(arrOutTasks.size() > slaInterval) {
					//Go to each task in the recent interval
					for(int count2=slaInterval-1;count2<arrOutTasks.size();count2++) {
						//Get the task
						Task t2 = arrOutTasks.get(count2);
						
						//Check if this task is from the same user and the same machine
						if(t.machine.equals(t2.machine) && t.user.equals(t2.user)) {
							//Check if they have been during the given time interval
							long diff = Math.abs(t.getTaskDate().getTime() - t2.getTaskDate().getTime());
							if(diff < slaTime) {
								//Process tasks from same machine & same user in the decided seconds interval
								taskMeetsSLA = false;
								
								System.out.println("Tasks:" + t.taskId + "," + t2.taskId + ", do not meet SLA");
							}
						}
					}
				}
				if(taskMeetsSLA || count == arrTasks.size()-1) {
					arrOutTasks.add(t);
					arrTasks.remove(count);
					break;
				}
			}
		}
		
		return arrOutTasks;
	}
	public static ArrayList<Task> analyzePatterns(ArrayList<Task> arrTasks, int numPatterns) {
		ArrayList<Task> outTasks = new ArrayList<Task>();
		
		//Go to each task and apply clustering
		ArrayList<Integer> arrTaskClusters = new ArrayList<Integer>();
		ArrayList<Task> arrTaskCentroids = new ArrayList<Task>();
		ArrayList<Integer> arrNumberTaskClusters = new ArrayList<Integer>();
		
		//Initialize the centroids
		Task.fuzzyMode = false;
		Collections.sort(arrTasks);
		
		int in_count = 0;
		int interval = Math.round(arrTasks.size() / numPatterns);
		for(int count=0;count<numPatterns;count++) {
			arrTaskCentroids.add(arrTasks.get(in_count));
			System.out.println(arrTasks.get(in_count));
			arrNumberTaskClusters.add(0);
			in_count = in_count + interval;
		}
		
		//Now perform the clustering
		for(int count=0;count<arrTasks.size();count++) {
			Task t1 = arrTasks.get(count);
			
			int bestCluster = 0;
			float bestDifference = 0;
			for(int count2=0;count2<arrTaskCentroids.size();count2++) {
				Task t2 = arrTaskCentroids.get(count2);
				//float diff = Math.abs(t2.numTasks - t1.numTasks)/Math.max(t1.numTasks, t2.numTasks);
				float diff = 0;
				diff = diff + Math.abs(t2.taskLength - t1.taskLength);
				
				if(count2 == 0) {
					bestCluster = count2;
					bestDifference = diff;
				} else {
					if(diff < bestDifference) {
						bestCluster = count2;
						bestDifference = diff;
					}
				}
			}
			arrTaskClusters.add(bestCluster);
			arrNumberTaskClusters.set(bestCluster, arrNumberTaskClusters.get(bestCluster)+1);
			System.out.println("Task " + count + " cluster " + bestCluster);
		}
		for(int count=0;count<arrNumberTaskClusters.size();count++) {
			System.out.println("Cluster:" + count + ", Number of tasks:" + arrNumberTaskClusters.get(count));
		}
		
		//Add the tasks to the out task list
		int currentCluster = 0;
		while(outTasks.size() < arrTasks.size()) {
			for(int count=0;count<arrTasks.size();count++) {
				if(arrNumberTaskClusters.get(currentCluster) > 0 && arrTaskClusters.get(count) == currentCluster && outTasks.contains(arrTasks.get(count)) == false) {
					//We have found a task which can be added to the output
					outTasks.add(arrTasks.get(count));
					arrNumberTaskClusters.set(currentCluster,arrNumberTaskClusters.get(currentCluster)-1);
				}
				currentCluster = currentCluster + 1;
				if(currentCluster == numPatterns)
					currentCluster = 0;
			}	
		}
		return outTasks;
	}
	
	public static ArrayList<Task> analyzePatternsKMeans(ArrayList<Task> arrTasks, int numPatterns) {
		ArrayList<Task> outTasks = new ArrayList<Task>();
		
		//Go to each task and apply clustering
		ArrayList<Integer> arrTaskClusters = new ArrayList<Integer>();
		ArrayList<Task> arrTaskCentroids = new ArrayList<Task>();
		ArrayList<Integer> arrNumberTaskClusters = new ArrayList<Integer>();
		
		//Initialize Random Centroids
		Random rnd = new Random();
		for(int count=0;count<numPatterns;) {
			int index = rnd.nextInt(arrTasks.size());
			Task t = arrTasks.get(index);
			if(arrTaskCentroids.contains(t) == false) {
				arrTaskCentroids.add(t);
				arrNumberTaskClusters.add(0);
				count = count + 1;
			}
		}
		
		//Now perform the clustering
		for(int count=0;count<arrTasks.size();count++) {
			Task t1 = arrTasks.get(count);
			
			int bestCluster = 0;
			float bestDifference = 0;
			for(int count2=0;count2<arrTaskCentroids.size();count2++) {
				Task t2 = arrTaskCentroids.get(count2);
				//float diff = Math.abs(t2.numTasks - t1.numTasks)/Math.max(t1.numTasks, t2.numTasks);
				float diff = 0;
				diff = diff + Math.abs(t2.taskLength - t1.taskLength);
				
				if(count2 == 0) {
					bestCluster = count2;
					bestDifference = diff;
				} else {
					if(diff < bestDifference) {
						bestCluster = count2;
						bestDifference = diff;
					}
				}
			}
			arrTaskClusters.add(bestCluster);
			arrNumberTaskClusters.set(bestCluster, arrNumberTaskClusters.get(bestCluster)+1);
			System.out.println("Task " + count + " cluster " + bestCluster);
		}
		for(int count=0;count<arrNumberTaskClusters.size();count++) {
			System.out.println("Cluster:" + count + ", Number of tasks:" + arrNumberTaskClusters.get(count));
		}
		
		//Add the tasks to the out task list
		int currentCluster = 0;
		while(outTasks.size() < arrTasks.size()) {
			for(int count=0;count<arrTasks.size();count++) {
				if(arrNumberTaskClusters.get(currentCluster) > 0 && arrTaskClusters.get(count) == currentCluster && outTasks.contains(arrTasks.get(count)) == false) {
					//We have found a task which can be added to the output
					outTasks.add(arrTasks.get(count));
					arrNumberTaskClusters.set(currentCluster,arrNumberTaskClusters.get(currentCluster)-1);
				}
				currentCluster = currentCluster + 1;
				if(currentCluster == numPatterns)
					currentCluster = 0;
			}	
		}
		return outTasks;
	}
	
	public static ArrayList<Task> analyzePatternsFCM(ArrayList<Task> arrTasks, int numPatterns) {
		ArrayList<Task> outTasks = new ArrayList<Task>();
		
		//Go to each task and apply clustering
		ArrayList<Integer> arrTaskClusters = new ArrayList<Integer>();
		ArrayList<Task> arrTaskCentroids = new ArrayList<Task>();
		ArrayList<Integer> arrNumberTaskClusters = new ArrayList<Integer>();
		
		//Initialize the centroids
		Task.fuzzyMode = true; //Initiate the fuzzy mode
		Collections.sort(arrTasks);
		
		int in_count = 0;
		int interval = Math.round(arrTasks.size() / numPatterns);
		for(int count=0;count<numPatterns;count++) {
			arrTaskCentroids.add(arrTasks.get(in_count));
			System.out.println(arrTasks.get(in_count));
			arrNumberTaskClusters.add(0);
			in_count = in_count + interval;
		}
		
		//Now perform the clustering
		for(int count=0;count<arrTasks.size();count++) {
			Task t1 = arrTasks.get(count);
			
			int bestCluster = 0;
			float bestDifference = 0;
			for(int count2=0;count2<arrTaskCentroids.size();count2++) {
				Task t2 = arrTaskCentroids.get(count2);
				float diff = 0;
				float fuzzyFactor1 = t1.numTasks*t1.taskLength;
				float fuzzyFactor2 = t2.numTasks*t2.taskLength;
				
				diff = diff + Math.abs((fuzzyFactor2) - (fuzzyFactor1));
				
				if(count2 == 0) {
					bestCluster = count2;
					bestDifference = diff;
				} else {
					if(diff < bestDifference) {
						bestCluster = count2;
						bestDifference = diff;
					}
				}
			}
			arrTaskClusters.add(bestCluster);
			arrNumberTaskClusters.set(bestCluster, arrNumberTaskClusters.get(bestCluster)+1);
			System.out.println("Task " + count + " cluster " + bestCluster);
		}
		for(int count=0;count<arrNumberTaskClusters.size();count++) {
			System.out.println("Cluster:" + count + ", Number of tasks:" + arrNumberTaskClusters.get(count));
		}
		
		//Add the tasks to the out task list
		int currentCluster = 0;
		while(outTasks.size() < arrTasks.size()) {
			for(int count=0;count<arrTasks.size();count++) {
				if(arrNumberTaskClusters.get(currentCluster) > 0 && arrTaskClusters.get(count) == currentCluster && outTasks.contains(arrTasks.get(count)) == false) {
					//We have found a task which can be added to the output
					outTasks.add(arrTasks.get(count));
					arrNumberTaskClusters.set(currentCluster,arrNumberTaskClusters.get(currentCluster)-1);
				}
				currentCluster = currentCluster + 1;
				if(currentCluster == numPatterns)
					currentCluster = 0;
			}	
		}
		return outTasks;
	}
	
	public static float findTasksDiversity(ArrayList<Task> arrTasks) {
		float taskDiversity = 0;
		for(int count=0;count<arrTasks.size()-1;count++) {
			taskDiversity = taskDiversity + Math.abs(arrTasks.get(count).taskLength - arrTasks.get(count+1).taskLength);
		}
		return taskDiversity;
	}
}
