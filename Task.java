import java.util.*;

public class Task {
	
	public int status;            	// 0 for unstarted, 1 for running, 2 for blocked, 3 for aborted, 4 for terminated
	public int total_time;        	// total running time
	public int wait_time;		 	// total waiting time
	public int task_id;			  	// task number of this task
	public int[] initial_claim;     // initial claim of this task
	public int[] current_claim;     // record how many units of resource this task currently own
	public ArrayList<Activity> activity_list; // list of activities
	
	public Task(int task_id, int res_num){
		this.status = 0;
		this.total_time = 0;
		this.wait_time = 0;
		this.task_id = task_id;
		this.initial_claim = new int[res_num];
		this.current_claim = new int[res_num];
		this.activity_list = new ArrayList<Activity>();	
	}
	
	// clone a task for simulation (activities in activity list will not be cloned)
	@SuppressWarnings("unchecked")
	public Task(Task t){
		this.status = t.status;
		this.total_time = t.total_time;
		this.wait_time = t.wait_time;
		this.task_id = t.task_id;
		this.initial_claim = t.initial_claim.clone();
		this.current_claim = t.current_claim.clone();
		this.activity_list = (ArrayList<Activity>) t.activity_list.clone();
	}
}
