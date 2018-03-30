import java.util.*;
import java.io.*;
public class Driver {
	
	private static int task_num; // total number of tasks
	private static int resource_types; // total number of resource type
	private static ArrayList<Task> task_queue = new ArrayList<Task>(); // all the tasks (running,blocked,finished)
	private static ArrayList<Integer> total_resource_units = new ArrayList<Integer>();// all the resource units
	
	// load the data into different structures
	
	public static void load(String filename) throws Exception{
		Scanner sc = new Scanner(new BufferedReader(new FileReader(filename)));
		task_num = sc.nextInt();
		resource_types = sc.nextInt();
		while (sc.hasNextInt()){
			total_resource_units.add(sc.nextInt());
		}
		
		// a hashmap checking whether a task has been initialized
		
		HashMap<Integer,Task> check_table = new HashMap<Integer,Task>();
		for (int i = 1; i < task_num + 1; i++){
			check_table.put(i, null);
		}
		
		while(sc.hasNext()){
			Activity act = new Activity(sc.next(), sc.nextInt(), sc.nextInt(), sc.nextInt(), sc.nextInt());
			int cur_task_id = act.task_id;
			if (check_table.get(cur_task_id) == null){
				Task t = new Task(cur_task_id,resource_types);
				task_queue.add(t);
				t.activity_list.add(act);
				check_table.remove(cur_task_id);
				check_table.put(cur_task_id, t);
			}else{
				Task t = check_table.get(cur_task_id);
				t.activity_list.add(act);
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public static void run_banker_algo(){
		
		int time = 0;
		ArrayList<Task> running_q = new ArrayList<Task>();				// tasks that are running
		ArrayList<Task> blocked_q = new ArrayList<Task>();				// tasks that have been blocked
		ArrayList<Task> finished_q = new ArrayList<Task>();				// tasks that have been finished
		ArrayList<Task> unblocked_q = new ArrayList<Task>();			// tasks that are unblocked in this cycle
		ArrayList<Integer> avail_resource = new ArrayList<Integer>();	// resource units that are available to allocate
		ArrayList<Integer> freed_resource = new ArrayList<Integer>();   // resource units that will be freed in the next cycle
		
		// Initialization
		
		running_q = (ArrayList<Task>) task_queue.clone();
		for (int i = 0; i < total_resource_units.size(); i++){
			int temp = total_resource_units.get(i);
			avail_resource.add(temp);
		}
		for (int i = 0; i < total_resource_units.size(); i++){
			freed_resource.add(0);
		}
		
		// keep running the program until all the tasks are either finished or aborted
		
		while(running_q.size() + blocked_q.size() != 0){
			
			// try to unblock tasks that are available to run
			
			for (int i = 0; i < blocked_q.size(); i++){
				Task cur_task = blocked_q.get(i);
				Activity cur_act = cur_task.activity_list.get(0);
				
				// copy the current task, task queue and available resource for safe stae check
				
				Task test_task = null;
				int index = cur_task.task_id;
				ArrayList<Task> test_task_queue = copy_task_queue(task_queue);
				for (int i1 = 0; i1 < test_task_queue.size(); i1++){
					Task t = test_task_queue.get(i1);
					if (t.task_id == index){
						test_task = t;
					}
				}
				ArrayList<Integer> test_avail_resource = (ArrayList<Integer>) avail_resource.clone();
				boolean check = check_safe(test_task,test_task_queue,test_avail_resource);
				if (check){
					cur_task.current_claim[cur_act.resource_id - 1] += cur_act.op_num;
					avail_resource.set(cur_act.resource_id - 1, avail_resource.get(cur_act.resource_id - 1) - cur_act.op_num);
					cur_task.activity_list.remove(0);
					cur_task.status = 1;
					unblocked_q.add(cur_task);
					blocked_q.remove(cur_task);
					i--;
				}else{
					cur_task.status = 2;
					cur_task.wait_time += 1;
				}
			}
			
			// run the program
			
			for (int i = 0; i < running_q.size(); i++){
				Task cur_task = running_q.get(i);
				Activity cur_act = cur_task.activity_list.get(0);
				if (cur_act.delay == 0){
					String act_type = cur_act.type;
					switch (act_type){
					
					case "initiate":
						
						if (cur_act.op_num <= total_resource_units.get(cur_act.resource_id - 1)){
							cur_task.status = 1;
							cur_task.initial_claim[cur_act.resource_id - 1] = cur_act.op_num;
							cur_task.activity_list.remove(0);
							
						// abort the task if the inital claim exceeds the maximum amount
							
						}else{
							cur_task.status = 3;
							running_q.remove(cur_task);
							finished_q.add(cur_task);
						}
						break;
						
					case "request":
						
						int max_request = cur_task.initial_claim[cur_act.resource_id - 1] - cur_task.current_claim[cur_act.resource_id - 1];
						
						// abort the task if the amount of request exceeds the inital claim
						
						if (max_request < cur_act.op_num){
							cur_task.status = 3;
							running_q.remove(cur_task);
							i--;
							finished_q.add(cur_task);
							for (int i1 = 0; i1 < avail_resource.size(); i1++){
								avail_resource.set(i1, avail_resource.get(i1) + cur_task.current_claim[i1]);
								cur_task.current_claim[i1] = 0;
							}
						}else{
							
							// copy the current task, task queue and available resource for safe stae check
							
							Task test_task = null;
							int index = cur_task.task_id;
							ArrayList<Task> test_task_queue = copy_task_queue(task_queue);
							for (int i1 = 0; i1 < test_task_queue.size(); i1++){
								Task t = test_task_queue.get(i1);
								if (t.task_id == index){
									test_task = t;
								}
							}
							ArrayList<Integer> test_avail_resource = (ArrayList<Integer>) avail_resource.clone();
							boolean check = check_safe(test_task,test_task_queue,test_avail_resource);
							//boolean test = true;// debugging information
							
							// if it is a safe state, grant the request
							if (check){
								cur_task.current_claim[cur_act.resource_id - 1] += cur_act.op_num;
								avail_resource.set(cur_act.resource_id - 1, avail_resource.get(cur_act.resource_id - 1) - cur_act.op_num);
								cur_task.activity_list.remove(0);
							}else{
								cur_task.status = 2;
								cur_task.wait_time ++;
								running_q.remove(cur_task);
								// there is one less element in the queue so need to reset the iterator
								i--;
								blocked_q.add(cur_task);
							}
						}
						break;
						
					// put all resource units that are going to be released into a buffer, they will be release in the next cycle
						
					case "release":
						freed_resource.set(cur_act.resource_id - 1, freed_resource.get(cur_act.resource_id - 1) + cur_act.op_num);
						cur_task.current_claim[cur_act.resource_id - 1] -= cur_act.op_num;
						cur_task.activity_list.remove(0);
						break;
						
					// terminate a task
						
					case "terminate":
						cur_task.status = 4;
						for (int i1 = 0; i1 < avail_resource.size(); i1++){
							avail_resource.set(i1, avail_resource.get(i1) + cur_task.current_claim[i1]);
							cur_task.current_claim[i1] = 0;
						}
						running_q.remove(cur_task);
						i--;
						finished_q.add(cur_task);
						cur_task.activity_list.remove(0);
						cur_task.total_time = time;
						break;
					default:
						System.out.print("no such command" + "\n");
						break;
					}
				}else{
					cur_act.delay --;
				}
			}
			
			// put the released resource units back
			
			for (int i = 0; i < avail_resource.size(); i++){
				avail_resource.set(i, avail_resource.get(i) + freed_resource.get(i));
				freed_resource.set(i, 0);
			}
			
			// unblock all the tasks in unblocked_q
			for (int i = 0; i < unblocked_q.size(); i++){
				Task t = unblocked_q.get(i);
				running_q.add(t);
			}
			unblocked_q.clear();
			
			time++;			
		}
		
		//output
		
		System.out.println("Banker's Algorithm");
		int total_time = 0;
		int wait_time = 0;
		ArrayList<Task> output = new ArrayList<Task>();
		
		// sort the finished_q in order
		
		for (int i = 1; i < task_num + 1; i++){
			for (int j = 0; j < finished_q.size(); j++){
				if(finished_q.get(j).task_id == i){
					output.add(finished_q.get(j));
				}
			}
		}
		
		for (int j = 0; j < output.size(); j++){
			Task cur_task = output.get(j);
			if (cur_task.status == 4){
				float percentage_temp =(float) cur_task.wait_time /(float) cur_task.total_time;
				int percentage = (int) (percentage_temp * 100);
				System.out.println("Task " + cur_task.task_id + "\t" + cur_task.total_time + "\t" + 
													cur_task.wait_time + "\t" + percentage + "%");
				total_time += cur_task.total_time;
				wait_time += cur_task.wait_time;
			}else if(cur_task.status == 3){
				System.out.println("Task " + cur_task.task_id + "\t" + "aborted" + "\t" );
			}else{
				System.out.println("error");
			}
		}
		
		float percentage_temp = (float) wait_time / (float) total_time;
		int percentage = (int)(percentage_temp * 100);
		System.out.println("total " + "\t" + total_time + "\t" + wait_time + "\t" + percentage +"%");
		System.out.println();
		
	}
	
	@SuppressWarnings("unchecked")
	public static void runFIFO(){
		int time = 0;
		ArrayList<Task> running_q = new ArrayList<Task>();				// tasks that are running
		ArrayList<Task> blocked_q = new ArrayList<Task>();				// tasks that have been blocked
		ArrayList<Task> unblocked_q = new ArrayList<Task>();			// tasks that are unblocked in this cycle
		ArrayList<Task> finished_q = new ArrayList<Task>();				// tasks that have been finished
		ArrayList<Integer> avail_resource = new ArrayList<Integer>();	// resource units that are available to allocate
		ArrayList<Integer> freed_resource = new ArrayList<Integer>();	// resource units that will be freed in the next cycle
		
		//Initilaizaiton
		
		running_q = (ArrayList<Task>) task_queue.clone();
		for (int i = 0; i < total_resource_units.size(); i++){
			int temp = total_resource_units.get(i);
			avail_resource.add(temp);
		}
		for (int i = 0; i < total_resource_units.size(); i++){
			freed_resource.add(0);
		}
		//int test = 0;
		while (running_q.size() + blocked_q.size() != 0){
			// debugging info 
			/*test++;
			if (test == 25){
				break;
			}*/
			
			// try to unblock tasks that are available to run
			
			for (int i = 0; i < blocked_q.size(); i++){
				Task cur_task = blocked_q.get(i);
				Activity cur_act = cur_task.activity_list.get(0);
				int index = cur_act.resource_id;
				if (cur_act.op_num <= avail_resource.get(index - 1)){
					avail_resource.set(index - 1, avail_resource.get(index - 1) - cur_act.op_num);
					cur_task.current_claim[index - 1] += cur_act.op_num;
					cur_task.status = 1;
					cur_task.activity_list.remove(cur_act);
					blocked_q.remove(cur_task);
					i--;
					unblocked_q.add(cur_task);
				}else{
					cur_task.wait_time ++;
					cur_task.status = 2;
				}
			}
			
			// run the program
			
			for (int i = 0; i < running_q.size(); i++){
				Task cur_task = running_q.get(i);
				Activity cur_act = cur_task.activity_list.get(0);
				int delay = cur_act.delay;
				if (delay == 0){
					String act_type = cur_act.type;
					switch (act_type){
					
					// ignore the initial claim
					
					case "initiate":
						cur_task.activity_list.remove(0);
						break;
							
						
					case "request":
						
						// abort the task  if the request number exceeds total units number
						if (cur_act.op_num > total_resource_units.get(cur_act.resource_id - 1)){
							cur_task.status = 3;
							running_q.remove(cur_task);
							i--;
							finished_q.add(cur_task);
							for (int i1 = 0; i1 < avail_resource.size(); i1++){
								avail_resource.set(i1, avail_resource.get(i1) + cur_task.current_claim[i1]);
								cur_task.current_claim[i1] = 0;
							}
						}else{
							// can't statisfy the task, block it
							if (cur_act.op_num > avail_resource.get(cur_act.resource_id - 1)){
								cur_task.status = 2;
								cur_task.wait_time ++;
								running_q.remove(cur_task);
								i--;
								blocked_q.add(cur_task);
							}else{
								// alocate the resource
								cur_task.current_claim[cur_act.resource_id - 1] += cur_act.op_num;
								avail_resource.set(cur_act.resource_id - 1, avail_resource.get(cur_act.resource_id - 1) - cur_act.op_num);
								cur_task.activity_list.remove(0);
							}
						}
						break;
						
					// put the released units in this cycle to a buffer	
						
					case "release":
						freed_resource.set(cur_act.resource_id - 1, freed_resource.get(cur_act.resource_id - 1) + cur_act.op_num);
						cur_task.current_claim[cur_act.resource_id - 1] -= cur_act.op_num;
						cur_task.activity_list.remove(0);
						break;
						
					// terminate tasks	
						
					case "terminate":
						cur_task.status = 4;
						for (int i1 = 0; i1 < avail_resource.size(); i1++){
							avail_resource.set(i1, avail_resource.get(i1) + cur_task.current_claim[i1]);
							cur_task.current_claim[i1] = 0;
						}
						running_q.remove(cur_task);
						i--;
						finished_q.add(cur_task);
						cur_task.activity_list.remove(0);
						cur_task.total_time = time;
						break;
					default:
						System.out.print("no such command" + "\n");
						break;
					}
					
				}else{
					cur_act.delay--;
				}
			}
			
			// unblock all the tasks that get unblocked in this cycle
			
			for (int i = 0; i < unblocked_q.size(); i++){
				Task t = unblocked_q.get(i);
				running_q.add(t);
			}
			
			unblocked_q.clear();
			
			// check deadlock
			
			if (!blocked_q.isEmpty() && running_q.isEmpty()){
				
				boolean check = check_deadlock(blocked_q,avail_resource);
				// abort the lowers number task until there is no deadlock
				while(!check){
					int lowest_number = 99999;
					int index = 0;
					for (int i = 0;i < blocked_q.size(); i++){
						if (blocked_q.get(i).task_id < lowest_number){
							lowest_number = blocked_q.get(i).task_id;
							index = i;
						}
					}
					Task aborted_task = blocked_q.get(index);
					aborted_task.status = 3;
					blocked_q.remove(aborted_task);
					finished_q.add(aborted_task);
					for (int j = 0; j < avail_resource.size(); j++){
						avail_resource.set(j, avail_resource.get(j) + aborted_task.current_claim[j]);
						aborted_task.current_claim[j] = 0;
					}
					check = check_deadlock(blocked_q,avail_resource);
				}
				
			}
			
			//put the released resource back 
			
			for (int i = 0; i < avail_resource.size(); i++){
				avail_resource.set(i, avail_resource.get(i) + freed_resource.get(i));
				freed_resource.set(i, 0);
			}
			time ++;
		}
		
		//output
		
		System.out.println("FIFO Algorithm");
		int total_time = 0;
		int wait_time = 0;
		ArrayList<Task> output = new ArrayList<Task>();
		
		// sort the finished_q
		for (int i = 1; i < task_num + 1; i++){
			for (int j = 0; j < finished_q.size(); j++){
				if(finished_q.get(j).task_id == i){
					output.add(finished_q.get(j));
				}
			}
		}
		
		for (int j = 0; j < output.size(); j++){
			Task cur_task = output.get(j);
			if (cur_task.status == 4){
				float percentage_temp =(float) cur_task.wait_time /(float) cur_task.total_time;
				int percentage = (int) (percentage_temp * 100);
				System.out.println("Task " + cur_task.task_id + "\t" + cur_task.total_time + "\t" + 
													cur_task.wait_time + "\t" + percentage + "%");
				total_time += cur_task.total_time;
				wait_time += cur_task.wait_time;
			}else if(cur_task.status == 3){
				System.out.println("Task " + cur_task.task_id + "\t" + "aborted" + "\t" );
			}else{
				System.out.println("error");
			}
		}
		float percentage_temp = (float) wait_time / (float) total_time;
		int percentage = (int)(percentage_temp * 100);
		System.out.println("total " + "\t" + total_time + "\t" + wait_time + "\t" + percentage +"%");
		System.out.println();
	}
	
	// check the whether current state is safe in banker's algorithm
	
	public static boolean check_safe(Task t, ArrayList<Task> task_queue, ArrayList<Integer> avail_resource){
		if (task_queue.size() == 0){
			return true;
		}
		Activity cur_act = t.activity_list.get(0);
		boolean check = true;
		if (cur_act.op_num > avail_resource.get(cur_act.resource_id - 1)) check =false;
		if (check == false){
			return false;
		}
		avail_resource.set(cur_act.resource_id - 1, avail_resource.get(cur_act.resource_id - 1) - cur_act.op_num);
		t.current_claim[cur_act.resource_id - 1] += cur_act.op_num;
		while(!task_queue.isEmpty()){
			int flag = 0;
			for (Task task :task_queue){
				if (check_resource_allocation(task,avail_resource)){
					for (int i = 0; i < avail_resource.size(); i++){
						avail_resource.set(i,avail_resource.get(i) + task.current_claim[i]);
						task.current_claim[i] = 0;
						task_queue.remove(task);
					}
					flag ++;
				}
				if (flag != 0) break;
			}
			if (flag == 0){
				return false;
			}
		}
		return true;
	}
	
	// check whether we can unblock the first element of the blocked_queue for FIFO
	
	public static boolean check_deadlock(ArrayList<Task> blocked, ArrayList<Integer> avail_resource){
		Task cur_task = blocked.get(0);
		Activity cur_act = cur_task.activity_list.get(0);
		if (cur_act.op_num > avail_resource.get(cur_act.resource_id - 1)){
			return false;
		}
		return true;
	}
	
	// check whether we can satisfy all Task t's inital claim in this cycle (for banker's algorithm)
	
	public static boolean check_resource_allocation(Task t, ArrayList<Integer> avail_resource){
		for (int i = 0; i < avail_resource.size(); i++){
			int temp = t.initial_claim[i] - t.current_claim[i];
			if (temp > avail_resource.get(i)){
				return false;
			}
		}
		return true;
	}
	
	// copy the task queue so that we can run simulations
	
	public static ArrayList<Task> copy_task_queue(ArrayList<Task> task_queue){
		ArrayList<Task> result = new ArrayList<Task>();
		for(int i = 0; i < task_queue.size(); i++){
			Task cur_task = task_queue.get(i);
			result.add(new Task(cur_task));
		}
		return result;
	}
	
	public static void main(String args[]) throws Exception{
		if (args.length == 1){
			String filename = args[0];
			load (filename);
			runFIFO();
			task_num = 0;
			resource_types = 0;
			task_queue.clear();
			total_resource_units.clear();
			load (filename);
			run_banker_algo();
		}else{
			System.out.println("no input");
		}
	}
}
