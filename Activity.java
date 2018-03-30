
public class Activity {
	
	public String type;     // four types: initiate, request, release, terminate
	public int task_id;     // task id of the current activity
	public int delay;       // delay
	public int resource_id; // resource id of current activity
	public int op_num;      // operation number of this activity
	
	public Activity(String type, int task_id, int delay, int resource_id, int op_num){
		this.type = type;
		this.task_id = task_id;
		this.delay = delay;
		this.resource_id = resource_id;
		this.op_num = op_num;
	}
}
