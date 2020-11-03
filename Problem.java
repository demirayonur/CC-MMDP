package version_2;


// Libraries

import java.io.*;
import java.util.HashMap;


public class Problem {
	
	/*
	 *   i. components of a problem instance
	 *   	i.i.    nState,    number of states of the MDP models we consider
	 *      i.ii.   nStage,    number of decision epochs (stages)
	 *      i.iii.  nPop,      number of patients in the cohort
	 *      i.iv.   rD,        the reward obtained for the absorbing conditions
	 *      i.v.    parentDic, the path of the directory in which all scenarios are listed
	 *      i.vi.   capacity,  the capacity for each decision epoch in which the policy-maker can take an action
	 *      i.vii.  priors,    initial probability distribution over the set of non-absorbing states
	 *      i.viii. scenarios, MDP models we consider
	 *   
	 *   ii. in the last decision epoch, the policy maker is not allowed to take an action
	 *       ii.i. therefore, dim(capacity) = nStage-1
	 *   
	 *   iii. prior distribution is defined over the set of non-absorbing states
	 *       iii.i therefore, dim(priors) = nState-1
	 */
	
	public int nState;
	public int nStage;
	public int nPop;
	public double rD;
	public String parentDic;
	public int[] capacity;
	public double[] priors;
	public MDP[] scenarios;
	
	public Problem(int nScenario, int nPop, String parentDic, int[] capacity, double[] priors) throws IOException {
		
		// The only constructor method we have
		
		nState = priors.length+1;
		nStage = capacity.length+1;
		rD = 0;
		this.nPop = nPop;
		this.parentDic = parentDic;
		this.capacity = capacity;
		this.priors = priors;
		scenarios = new MDP[nScenario];
		read();
	}
	
	public Problem(boolean isInstance, int whichScenario, int nPop, String parentDic, int[] capacity, double[] priors) throws IOException {
		
		// The only constructor method we have
		
		nState = priors.length+1;
		nStage = capacity.length+1;
		rD = 0;
		this.nPop = nPop;
		this.parentDic = parentDic;
		this.capacity = capacity;
		this.priors = priors;
		if(isInstance) {
			scenarios = new MDP[1];
			String currFile = parentDic + "Scenario_" + whichScenario + "/";
			HashMap<String, String> csvFiles = new HashMap<String, String>();
			csvFiles.put("P", currFile + "P.csv");
			csvFiles.put("Q", currFile + "Q.csv");
			csvFiles.put("r", currFile + "r.csv");
			scenarios[0] = new MDP(csvFiles);
		}		
	}

	
	public void read() throws IOException{
		
		// reads the first "nScenario" number of scenarios and stores it into "scenarios"
		
		for(int curr_scenario=1;curr_scenario<=scenarios.length;curr_scenario++)
		{
			String currFile = parentDic + "Scenario_" + curr_scenario + "/";
			HashMap<String, String> csvFiles = new HashMap<String, String>();
			csvFiles.put("P", currFile + "P.csv");
			csvFiles.put("Q", currFile + "Q.csv");
			csvFiles.put("r", currFile + "r.csv");
			scenarios[curr_scenario-1] = new MDP(csvFiles);
		}
	}
	 
	public class MDP {
		
		/*
		 *  i.  components of a stationary Markov decision process
		 *  ii. only the components which subject to the randomness
		 *      	ii.i.   P, transition probabilities among non-absorbing states, dim = |S-1|x2x|S-1|
		 *          ii.ii.  Q, transition probabilities to the absorbing state,     dim = |S-1|x2
		 *          ii.iii. r, immediate rewards,                                   dim = |S-1|x2
		 *          ii.iv.  R, terminal rewards,									dim = |S-1|  
		 */         
		
		double [][][] P; 
		double [][] Q;
		double [][] r;
		double [] R;
		
		public MDP(double [][][] P, double [][] Q, double [][] r, double [] R) {
			
			// Constructor method 1: the case where the components are specified in the arguments
			
			this.P = P;
			this.Q = Q;
			this.r = r;
			this.R = R;
		}
		
		public MDP(HashMap<String, String> csvFiles) throws IOException {
			
			// Constructor method 2: the case where the directory in which components are read is given
			
			/*
			 * More about the argument: csvFiles
			 * ---------------------------------
			 * There are 3 keys of it each of which responsible for reading P, Q or r.
			 * Keys: "P", "Q", "r"
			 * Values: csvFiles.get("P") --> the path for the .csv file related to the component P
			 *  	   csvFiles.get("Q") --> the path for the .csv file related to the component Q
			 *  	   csvFiles.get("r") --> the path for the .csv file related to the component r
			 *  
			 *  Warning 1: 
			 *  ---------
			 *  For the last component "R", there is not any .csv file since it is derived from "r".
			 *  
			 *  Warning 2:
			 *  ----------
			 *  .csv files encodes the parameters in a flattened way. Thus, reader is expected to
			 *  reshape them once it completes reading the corresponding .csv files.
			 */
			
			// initialize
			
			P = new double[nState-1][2][nState-1];
			Q = new double[nState-1][2];
			r = new double[nState-1][2];
			R = new double[nState-1];
			
			// read and prepare P
			
			BufferedReader br = new BufferedReader( new FileReader(csvFiles.get("P")));
			String strLine = "";
			double[] dummyP = new double[(nState-1)*2*(nState-1)];
			int index = 0;
			while( (strLine = br.readLine()) != null)
			{
				dummyP[index++] = Double.parseDouble(strLine);
			}
			br.close();
			P = from1Dto3D(dummyP, nState-1, 2, nState-1);
			
			// read and prepare Q
			
			br = new BufferedReader( new FileReader(csvFiles.get("Q")));
			strLine = "";
			double[] dummyQ = new double[(nState-1)*2];
			index = 0;
			while( (strLine = br.readLine()) != null)
			{
				dummyQ[index++] = Double.parseDouble(strLine);
			}
			br.close();
			Q = from1Dto2D(dummyQ, nState-1, 2);
			
			// read and prepare r

			br = new BufferedReader( new FileReader(csvFiles.get("r")));
			strLine = "";
			double[] dummyr = new double[(nState-1)*2];
			index = 0;
			while( (strLine = br.readLine()) != null)
			{
				dummyr[index++] = Double.parseDouble(strLine);
			}
			br.close();
			r = from1Dto2D(dummyr, nState-1, 2);

			// derive R from r

			for(int i=0;i<nState-1;i++)
			{
				R[i] = (r[i][0]+r[i][1])/2;
			}
		}
	}	

	public static double[][] from1Dto2D(double[] A, int m, int n){
		
		// it converts the given flattaned 1D array into 2D array within the shape (m,n), if possible.
		
		int nElem = A.length;
		if(nElem != m*n)
		{
			throw new IllegalArgumentException("Inappropriate dimensions");
		}
		double[][] B = new double[m][n];

		int index = 0;
		for(int i = 0;i<m;i++)
		{
			for(int j = 0;j<n;j++)
			{
				B[i][j] = A[index++];
			}

		}
		return B;
	}
		
	public static double[][][] from1Dto3D(double[] A, int m, int n, int k){
		
		// it converts the given flattaned 1D array into 3D array within the shape (m,n,k), if possible.
		
		int nElem = A.length;
		if(nElem != m*n*k)
		{
			throw new IllegalArgumentException("Inappropriate dimensions");
		}
		double[][][] B = new double[m][n][k];

		int index = 0;

		for(int i=0;i<m;i++)
		{
			for(int j=0;j<n;j++)
			{
				for(int q=0;q<k;q++)
				{
					B[i][j][q] = A[index++];
				}
			}
		}

		return B;
	}
	

}
