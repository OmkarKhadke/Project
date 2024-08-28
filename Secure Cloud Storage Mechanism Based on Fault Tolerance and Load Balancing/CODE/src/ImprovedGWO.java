import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class ImprovedGWO {

	//Dataset link http://www.cs.huji.ac.il/labs/parallel/workload/l_nasa_ipsc/index.html
	public static String DATASET_FILE_NAME = "dataset/nasa_set1.csv";
	
	public static List<Cloudlet> cloudletList;
	public static List<Vm> vmList;
	private static Datacenter createDatacenter(String name) {

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store
		// our machine
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores.
		// In this example, it will have only one core.
		List<Pe> peList = new ArrayList<Pe>();

		int mips = 1000;

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

		// 4. Create Host with its id and list of PEs and add them to the list
		// of machines
		int hostId = 0;
		int ram = 2048; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		hostList.add(
			new Host(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw),
				storage,
				peList,
				new VmSchedulerTimeShared(peList)
			)
		); // This is our machine

		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	// We strongly encourage users to develop their own broker policies, to
	// submit vms and cloudlets according
	// to the specific rules of the simulated scenario
	/**
	 * Creates the broker.
	 *
	 * @return the datacenter broker
	 */
	private static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	public static void main(String[] args) 
	{
		Date d1 = new Date();
		
		try {
			ArrayList<Task> arrTasks = new ArrayList<Task>();
			BufferedReader br = new BufferedReader(new FileReader(DATASET_FILE_NAME));
			String data;
			
			int id = 0;
			long totalTaskSize = 0;
			
			System.out.print("Select number of tasks:");
			BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
			int totTasks = Integer.parseInt(br2.readLine());
			
			//Read each line
			while((data = br.readLine()) != null) {
				
				//Get the task details
				String[] data_vals = data.split(",");
				
				//Get the tasks and their lengths
				//System.out.println(data);
				try {
					int numTasks = Integer.parseInt(data_vals[2]);
					int taskLength = Integer.parseInt(data_vals[3]);
					
					//Add the task to the list
					Task t = new Task();
					t.taskId = id;
					t.taskLength = taskLength;
					t.numTasks = numTasks;
					
					//Get the total task size
					totalTaskSize = totalTaskSize + (taskLength * numTasks);
					
					arrTasks.add(t);
					if(arrTasks.size() > totTasks)
						break;
					
					id++;	
				} catch (Exception ex) {
				}
			}
			System.out.println(id + " tasks fetched, total task size:" + totalTaskSize);
			br2.read();
			//Initialize the LCA parameters
			int LEAGUE_SIZE = 10; //Max number of players or machines in each league
			int NUMBER_OF_LEAGUES = 20;
			int NUMBER_OF_SEASONS = 10;
			float LCA_LEARNING_RATE = 0.5f; //Comparison rate for the LCA
			
			//Here we modify the learning rate according to the execution of the tasks on the vm
			int NUM_TEAMS_TO_CONTEST = 100;
			Random rnd = new Random();
			int optimum_effort = 0;
			float optimum_learning_rate = 0;
			int max_effort = 0;
			
			for(int team_num=0;team_num<NUM_TEAMS_TO_CONTEST;) {
				LCA_LEARNING_RATE = rnd.nextFloat(); 
				int team_effort = 0; // Initialize team effort
				
				if(team_num == 0) {
					optimum_learning_rate = LCA_LEARNING_RATE;
				}
				
				Vm[] availableCloudVMs = new Vm[LEAGUE_SIZE];
				for(int count=0;count<LEAGUE_SIZE;count++) {
					int mips = (int) (Math.random() * 250);
					int cpus = (int) (Math.random() * 2);
					int RAM = (int) (Math.random() * 512);
					int bw = (int) (Math.random() * 1000);
					int size = (int) (Math.random() * 1000);
					
					Vm vm = new Vm(count, count, mips, cpus, RAM, bw, size, "VM:" + count, new CloudletSchedulerTimeShared());
					availableCloudVMs[count] = vm;
				}
				
				ArrayList<Boolean> arrLeaguesToChange = new ArrayList<Boolean>();
				ArrayList<Solution> arrLCASolutions = new ArrayList<Solution>();
				
				for(int league=0;league<NUMBER_OF_LEAGUES;league++) {
					//Change all leagues initially
					arrLeaguesToChange.add(true);
					arrLCASolutions.add(new Solution());
				}
				
				//Go to each season
				for(int season=1;season<=NUMBER_OF_SEASONS;season++) {
					System.out.println("Season " + season);
					//Go to each league
					for(int league=0;league<NUMBER_OF_LEAGUES;league++) {
						System.out.print("League " + league);
						if(arrLeaguesToChange.get(league) == true) {
							System.out.print(" will be changed");
							//Generate a new solution
							int numVms = rnd.nextInt(LEAGUE_SIZE);
							System.out.println(" number of VMs:" + numVms);
							while(numVms <= 2)
								numVms = rnd.nextInt(LEAGUE_SIZE);
							
							ArrayList<Vm> arrVms = new ArrayList<Vm>();
							for(int vmcount=0;vmcount<numVms;vmcount++) {
								//Get a random vm number
								int index = rnd.nextInt(LEAGUE_SIZE);
								//fetch the VM from the pool of VMs
								Vm vm = availableCloudVMs[index];
								
								//Check if this VM is already a part of the solution
								while(arrVms.contains(vm)) {
									//If yes, then regenerate a new index
									index = rnd.nextInt(LEAGUE_SIZE);
									vm = availableCloudVMs[index];
								}
								arrVms.add(vm);
							}
							
							//Add this team to the league
							Solution lcaSol = new Solution();
							lcaSol.arrVMS = arrVms;
							
							//Repace this team with the new team in the league
							arrLCASolutions.set(league,lcaSol);
						} else {
							System.out.println(" will NOT be changed");
						}
					}
					
					//We have all the leagues, now find the fitness value
					//Fitness = Total Task size / Capacity of the team 
					float totalFitness = 0;
					for(int count=0;count<arrLCASolutions.size();count++) {
						//Get the solution
						Solution lcaSol = arrLCASolutions.get(count);
						
						//Go to each VM
						long capacity = 0;
						for(int count2=0;count2<lcaSol.arrVMS.size();count2++) {
							//Get the VM from the list of VMs
							Vm vm = lcaSol.arrVMS.get(count2);
							
							//Find the capacity of the team
							capacity = capacity + (long)(vm.getMips() * vm.getNumberOfPes());
						}
						
						lcaSol.fitness = (float)totalTaskSize / (capacity+1);
						arrLCASolutions.set(count, lcaSol);
						totalFitness = totalFitness + lcaSol.fitness;
					}
					
					float meanFitness = totalFitness / arrLCASolutions.size();
					System.out.println("Mean fitness for this season:" + meanFitness);
					float lcaThreshold = meanFitness * LCA_LEARNING_RATE;
					System.out.println("LCA Threshold for this season:" + lcaThreshold);
					for(int count=0;count<arrLCASolutions.size();count++) {
						
						//Remove the teams which are not performing well
						if(arrLCASolutions.get(count).fitness > lcaThreshold) {
							arrLeaguesToChange.set(count, true);
						} else {
							//keep the teams which are good
							arrLeaguesToChange.set(count, false);
						}
					}
				}
				
				//Get the best performing team from all seasons
				float bestFitness = arrLCASolutions.get(0).fitness;
				int bestIndex = 0;
				for(int count=0;count<arrLCASolutions.size();count++) {
					
					//Remove the teams which are not performing well
					if(arrLCASolutions.get(count).fitness > bestFitness) {
						bestFitness = arrLCASolutions.get(count).fitness;
						bestIndex = count;
					}
				}
				
				//Display the best result
				ArrayList<Vm> arrBestSolution = arrLCASolutions.get(bestIndex).arrVMS;
				System.out.println("Best solution found at index:" + bestIndex);
				//System.in.read();
				try {
					// First step: Initialize the CloudSim package. It should be called
					// before creating any entities.
					int num_user = 1; // number of cloud users
					Calendar calendar = Calendar.getInstance();
					boolean trace_flag = false; // mean trace events

					// Initialize the CloudSim library
					CloudSim.init(num_user, calendar, trace_flag);

					// Second step: Create Datacenters
					// Datacenters are the resource providers in CloudSim. We need at
					// list one of them to run a CloudSim simulation
					@SuppressWarnings("unused")
					Datacenter datacenter0 = createDatacenter("Datacenter_0");

					// Third step: Create Broker
					DatacenterBroker broker = createBroker();
					int brokerId = broker.getId();

					// Fourth step: Create one virtual machine
					// submit vm list to the broker
					broker.submitVmList(arrBestSolution);

					// Fifth step: Create one Cloudlet
					cloudletList = new ArrayList<Cloudlet>();

					// Cloudlet properties
					int id1 = 0;
					long length = 1000000;
					long fileSize = 300;
					long outputSize = 300;
					UtilizationModel utilizationModel = new UtilizationModelFull();

					Cloudlet cloudlet = new Cloudlet(id1, length, 1, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
					cloudlet.setUserId(brokerId);
					cloudlet.setVmId(0);
					cloudletList.add(cloudlet);
					broker.submitCloudletList(cloudletList);
					try {
						CloudSim.startSimulation();
					} catch(Exception ex) {
						
					}
					
					int vm_number = 0;
					long vm_capacity_left = (long) (arrBestSolution.get(vm_number).getMips() * arrBestSolution.get(vm_number).getNumberOfPes());
					for(int count=0;count<arrTasks.size();count++) {
						Task t = arrTasks.get(count);
						long tot_task = t.numTasks * t.taskLength;
						if(tot_task <= vm_capacity_left) {
							System.out.println("Task " + t.taskId + ", executing on Vm:" + vm_number);
							vm_capacity_left = vm_capacity_left - tot_task;
						} else {
							if(vm_capacity_left > 0) {
								System.out.println("Task " + t.taskId + ", executing on Vm:" + vm_number + " for " + vm_capacity_left + " cycles");
							}
							vm_capacity_left = 0;
						}
						
						if(vm_capacity_left == 0) {
							//Change the vm
							vm_number++;
							if(vm_number == arrBestSolution.size())
								vm_number = 0;
							
							vm_capacity_left = (long) (arrBestSolution.get(vm_number).getMips() * arrBestSolution.get(vm_number).getNumberOfPes());
							
							//Increment the effort
							team_effort++;
						}
					}
					CloudSim.stopSimulation();

					Log.printLine("All tasks completed");
				} catch (Exception e) {
					continue;
				}
				
				if(team_num == 0) {
					optimum_effort = team_effort;
				} else {
					//Check with which particular learning rate, does the team needs minimum effort to win the game
					if(team_effort < optimum_effort) {
						optimum_effort = team_effort;
						optimum_learning_rate = LCA_LEARNING_RATE;
					} else if(team_effort > max_effort) {
						max_effort = team_effort;
					}
				}
				team_num++;
			}
			
			Date d2 = new Date();
			long t = d2.getTime()-d1.getTime();
			
			System.out.println("Most optimum learning rate with delay of "+ optimum_effort*t + " us is " + optimum_learning_rate);
			System.out.println("Delay without GWO "+ max_effort*t + " us");
			
		} catch (Exception e) {
			// TODO: handle exception
			main(args);
		}
	}
	
	
}

