import java.util.List;
import java.util.ArrayList;
import javafx.util.Pair;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CC
{
	//Execute all given transactions, using locking.
	//Each element in the transactions List represents all operations performed by one transaction, in order.
	//No operation in a transaction can be executed out of order, but operations may be interleaved with other transactions if allowed by the locking protocol.
	//The index of the transaction in the list is equivalent to the transaction ID.
	//Print the log to either the console or a file at the end of the method. Return the new db state after executing the transactions.
    private static int[] wlock;
    private final static String logname = "log.txt";
	private static ArrayList<Integer>[] rlock;
    private static ArrayList<String> log;
    private static ArrayList<String[]> allT;
    private static ArrayList<int[]> time;
    private static ArrayList<Pair<Integer,String>>[] deadlock;
	public static int[] executeSchedule(int[] db, List<String> transactions)
	{
        
        log = new ArrayList<String>();
		wlock = new int[db.length];
		rlock = new ArrayList[db.length];
		for(int i = 0; i < db.length; i++){
			wlock[i] = -1;
			rlock[i] = new ArrayList<Integer>();
		}
        allT = new ArrayList<String[]>();
        time = new ArrayList<int[]>();
		for(int i = 0; i < transactions.size(); i++){
            allT.add(transactions.get(i).split(";"));
            int[] temp = new int[allT.get(i).length];
            for(int j = 0; j < temp.length; j++)
                temp[j]=-1;
            time.add(temp);
        }
        /*for(int i = 0; i < allT.size(); i++){
            String[] k = allT.get(i);
            int[] kk = time.get(i);
            for(int j = 0; j < k.length; j++){
                System.out.print(k[j]+":"+kk[j]+"\t");
            }
            System.out.println();
        }*/
        deadlock = new ArrayList[allT.size()];
        for(int i = 0; i < allT.size(); i++){
            deadlock[i] = new ArrayList<Pair<Integer,String>>();
        }
		List<String> order = createExecOrder(db);
        executeAll(db,order);
      /*  for(int i = 0; i < deadlock.length; i++){
            System.out.print("Transaction " + i + ":\t");
            for(int j = 0; j < deadlock[i].size(); j++){
                System.out.print(deadlock[i].get(j).getValue()+"\t");
            }
            System.out.println();
        }*/
        try{
            BufferedWriter bf = new BufferedWriter(new FileWriter(logname));
            String lg = "";
            for(int i = 0; i < log.size(); i++){
                lg+=log.get(i) + "\n";
                System.out.println(log.get(i));
            }
            bf.write(lg);
            bf.close();
        } catch (IOException e){
            e.printStackTrace();
        }
		return db;
    }

    static void executeAll(int[] db, List<String> t){
        for(int i = 0; i < t.size(); i++){
            if(t.get(i).charAt(0)=='W'){
                String[] num = t.get(i).split("[W(),]");

                int j = 0;
                for(; j<num.length; j++){
                    if(!num[j].equals(""))
                        break;
                }

                //db[Integer.parseInt(num[j])] = Integer.parseInt(num[j+1]);
                
            } else if(t.get(i).charAt(0)=='R'){
                int j = 0;
                String[] num = t.get(i).split("[R()]");
                for(; j <num.length; j++){
                    if(!num[j].equals(""))
                        break;
                }
            }
        }
    }
    
    static List<String> createExecOrder(int[] db){
        List<String> order = new ArrayList<String>();

        int[] index = new int[allT.size()];
        int transIndex = 0;
		int trans = 0;
		while(true){
			int k = trans%allT.size(); //transaction number
			trans++;
			boolean finished = true;
			for(int j = 0; j < allT.size(); j++){
				if(index[j]!=-1){
					finished = false;
					break;
				}
			}
			if(finished)
				break;
			if(index[k]==-1)
				continue;
			String[] temp = allT.get(k); // string at transaction number k
			if(index[k]>=temp.length){
				index[k]= -1;
				continue;
			}
			if(execute(db,k, index[k], order, transIndex)){
                index[k]++;
                transIndex++;
                //System.out.println(log.get(log.size()-1));
			} else {
                boolean[] visited = new boolean[deadlock.length];
                boolean[] recStack = new boolean[deadlock.length];

                boolean iscycle = detectCycle(0, visited, recStack);
                //System.out.println(iscycle);
                if(iscycle){
                    ArrayList<Integer> nodes = new ArrayList<Integer>();
                    for(int i = 0; i < recStack.length; i++){
                        if(recStack[i]==true)
                            nodes.add(i);
                        //System.out.println(i+":"+recStack[i]);
                    }
                    for(int i = nodes.size()-1; i >= 0; i--){
                        unlock(nodes.get(i));
                        
                        String l = "A:" + transIndex +",T"+nodes.get(i)+",";
                        if(index[k]==0)
                            l+= "-1";
                        else
                            l+=time.get(nodes.get(i))[index[k]-1];
                        abortRollBack(nodes.get(i),db);
                        transIndex++;
                        log.add(l);
                        visited = new boolean[deadlock.length];
                        recStack = new boolean[deadlock.length];
                        if(!detectCycle(0, visited, recStack)){
                            break;
                        }
                    }
                }
            }
        }
        
        return order;
    }


    static void abortRollBack(int transNum, int[] db){
        for(int i = log.size()-1; i >=0 ;i--){
            String[] temp = log.get(i).split("[,:]");
            if(temp[0].equals("W")){
                if(temp[2].equals("T"+transNum)){
                    int d = Integer.parseInt(temp[3]);
                    int convert = Integer.parseInt(temp[4]);
                    db[d] = convert;
                }
            }
        }
        allT.set(transNum, new String[0]);
    }


	static boolean execute(int[] db, int transNum, int index, List<String> order, int transIndex){
        //System.out.println(transaction+"\t"+transNum);
        String transaction = allT.get(transNum)[index];
		if(transaction.charAt(0)=='W'){
            String[] num = transaction.trim().split("[W(),]");

            int i = 0;
            for(; i <num.length; i++){
                if(!num[i].equals(""))
                    break;
            }
            int lck = writeLock(Integer.parseInt(num[i]),transNum);
			if(lck==transNum){
               // System.out.println("Write lock data num: " + num[i] + "\ttranscation: "+ transNum + "\t" + transaction);
                order.add(transaction);
                String l = "W:" + transIndex+",T"+transNum+","+num[i]+","+db[Integer.parseInt(num[i])]+","+num[i+1]+",";
                db[Integer.parseInt(num[i])] = Integer.parseInt(num[i+1]);
                if(index==0)
                    l+= "-1";
                else
                    l+=time.get(transNum)[index-1];
                time.get(transNum)[index] = transIndex;
                log.add(l);
				return true;
            }
            
		} else if(transaction.charAt(0)=='R'){
            int i = 0;
            String[] num = transaction.split("[R()]");
            for(; i <num.length; i++){
                if(!num[i].equals(""))
                    break;
            }
            
			int lck = readLock(Integer.parseInt(num[i]),transNum);
			if(lck==transNum){
                //System.out.println("Read lock data num: " + num[i] + "\ttranscation: "+ transNum + "\t" + transaction);
                order.add(transaction);
                String l = "W:" + transIndex+",T"+transNum+","+num[i]+","+db[Integer.parseInt(num[i])]+",";
                if(index==0)
                    l+= "-1";
                else
                    l+=time.get(transNum)[index-1];
                time.get(transNum)[index] = transIndex;
                log.add(l);
				return true;
			}
		} else {
            //System.out.println("Unlock " + transNum + "\t"+transaction);
			unlock(transNum);
            order.add(transaction);
            String l = "C:" + transIndex+",T"+transNum+",";
            if(index==0)
                l+= "-1";
            else
                l+=time.get(transNum)[index-1];

            time.get(transNum)[index] = transIndex;
            log.add(l);
			return true;
        }
		return false;
	}

    static boolean detectCycle(int i, boolean[] visited, boolean[] recStack){
        if (recStack[i]) 
            return true; 
  
        if (visited[i]) 
            return false; 
              
        visited[i] = true; 
  
        recStack[i] = true; 
        ArrayList<Pair<Integer,String>> children = deadlock[i]; 
          
        for (Pair<Integer,String> c: children) 
            if (detectCycle(c.getKey(), visited, recStack)) 
                return true; 
                  
        recStack[i] = false; 
  
        return false; 
    }


    //returns transNum if ok, otherwise return the transacttion that is blocking the current transaction
	static int readLock(int x, int transNum){
        ArrayList<Integer> l = rlock[x];
        for(int i = 0; i < l.size(); i++){
            if(l.get(i) == transNum)
                return transNum;
        }
        if(wlock[x] == -1){
            rlock[x].add(transNum);
            return transNum;
        }
        if(wlock[x] == transNum){
            rlock[x].add(transNum);
            return transNum;
        }
        deadlock[transNum].add(new Pair<Integer,String>(wlock[x],"W"));
		return wlock[x];
	}

	static int writeLock(int x, int transNum){
        
        ArrayList<Integer> temp = rlock[x];
        boolean f = false;
		for(int i = 0; i < temp.size(); i++){
			if(temp.get(i)!=transNum){
                f = true;
                deadlock[transNum].add(new Pair<Integer,String>(temp.get(i),"R,"+x));
            }
        }
        if(wlock[x]==-1 && f==false){
            wlock[x]=transNum;
            return transNum;
        }
		if(wlock[x]!=transNum && wlock[x]!=-1){
            deadlock[transNum].add(new Pair<Integer,String>(wlock[x],"W,"+x));
            return wlock[x];
        }
        if(f == true){
            return -1;
        }
		wlock[x] = transNum;
		return transNum;
	}

	
	static void unlock(int transNum){
        for(int i = 0; i < deadlock.length; i++){
            if(i==transNum){
                deadlock[i]=new ArrayList<Pair<Integer,String>>();
            } else {
                ArrayList<Pair<Integer,String>> temp = new ArrayList<Pair<Integer,String>>();
                for(int j = 0; j < deadlock[i].size(); j++){
                    if(deadlock[i].get(j).getKey().intValue() != transNum)
                        temp.add(deadlock[i].get(j));
                }
                deadlock[i] = temp;
            }
        }
        Integer k = Integer.valueOf(transNum);
		for (int i = 0; i < rlock.length; i++){
            if(wlock[i]==transNum)
                wlock[i]=-1;
            for(int j = 0; j < rlock[i].size(); j++){
                rlock[i].remove(k);
            }
        }
	}


}
