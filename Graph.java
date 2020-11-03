package version_2;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Graph {

	Problem problem;  // a graph to which problem
	int nScenario;
	int nTBar;	// number of stages in which we are allowed to take an action
	int nSBar;  // number of non-absorbing states
	ArrayList<Integer> scenarioIndexes;

	// graph data structure
	HashMap<Integer, ArrayList<Node>> tNodeMap;
	
	// result
	ArrayList<Node> path;
	double totalLength = 0;
	double elapsedTime = 0;
	int [][] strategy;
	
	Graph(Problem problem){
		this.problem = problem;
		nScenario = problem.scenarios.length;
		nTBar = problem.nStage-1;
		nSBar = problem.nState-1;
		scenarioIndexes = new ArrayList<Integer>();
		for(int i=0;i<nScenario;i++) scenarioIndexes.add(i);
		
		path = new ArrayList<Graph.Node>();
		tNodeMap =  new HashMap<Integer, ArrayList<Node>>();
		
		construct();
		strategy = new int[nTBar][nSBar];
	}
	
	class Node{
		
		int t;         // associated stage
		boolean[] pi;  // policy
		
		// Occupancy measures
		double [][] X_n;
		double [][] X_c;
		double [] Z;
		
		double obj;
		double value;
		
		Node previous;
		
		Node(int t, boolean[] pi){
			this.t = t; 
			this.pi = pi;
			X_n = new double[nScenario][nSBar];
			X_c = new double[nScenario][nSBar];
			Z = new double[nScenario];
			obj = 0;
			initOccupancyMeasure();  // it works only if t=0.
			previous = null;
		}
		
		Node(Node node){
			this.t = node.t;
			this.pi = node.pi;
			this.X_n = node.X_n;
			this.X_c = node.X_c;
			Z = node.Z;
			this.obj = node.obj;
			this.value = node.value;
			this.previous = node.previous;
		}
		
		void initOccupancyMeasure() {
			if(t==0) {
				
				// occupacy measure computation
				scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
					for(int i=0;i<nSBar;i++) {
						if(pi[i]==false) {
							X_n[l][i] = problem.priors[i];
							X_c[l][i] = 0;
						}else {
							X_n[l][i] = 0;
							X_c[l][i] = problem.priors[i];
						}
					}
					Z[l] = 0;
				});
				
				// stage-reward computation
				double[] scenarioReward = new double[nScenario];
				scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
					scenarioReward[l] = 0;
					for(int i=0;i<nSBar;i++) {
						scenarioReward[l] += X_n[l][i]*problem.scenarios[l].r[i][0] + X_c[l][i]*problem.scenarios[l].r[i][1] + Z[l] * problem.rD; 
					}
				});
				double sum_ = 0;
				for(int i=0;i<nScenario;i++) sum_ += scenarioReward[i];
				obj = sum_;
				value = obj;
			}
		}
		
		void makeDefault() {
			
			// makes the occupancy measures empty again
			X_n = new double[nScenario][nSBar];
			X_c = new double[nScenario][nSBar];
			Z = new double[nScenario];
			obj = 0;
		}
	
		void setOccupancyMeasures(double [][] xn, double [][] xc, double [] z) {
			
			// sets given measures and update the obj
			
			for(int l=0;l<nScenario;l++) {
				for(int i=0;i<nSBar;i++) {
					X_n[l][i] = xn[l][i];
					X_c[l][i] = xc[l][i];
					Z[l] = z[l];
				}
			}
			
			// update
			
			double[] scenarioReward = new double[nScenario];
			scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
				scenarioReward[l] = 0;
				for(int i=0;i<nSBar;i++) {
					scenarioReward[l] += X_n[l][i]*problem.scenarios[l].r[i][0] + X_c[l][i]*problem.scenarios[l].r[i][1] + Z[l] * problem.rD; 
				}
			});
			obj = 0;
			for(int i=0;i<nScenario;i++) obj += scenarioReward[i];

		}

		public double[] getCapacityUsage() {
			
			// computes and returns capacity usages for each scenario
			
			double[] usage = new double[nScenario];
			scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
					double use = 0;
					for(int i=0;i<nSBar;i++) {
						use += X_c[l][i];
					}
					usage[l] = problem.nPop * use;
			});
			
			return usage;
		}

		public boolean isFeasible() {
			
			// checks whether the stage policy is feasible or not
			
			double[] usages = getCapacityUsage();
			boolean flag = true;
			for(int i=0;i<nScenario;i++) {
				if(usages[i] > problem.capacity[t]) {
					flag = false;
					break;
				}
			}
			return flag;
		}
	}
	
	class Arc{
		
		Node from;
		Node to;
		
		// Occupancy measures
		double [][] X_n;
		double [][] X_c;
		double [] Z;
		
		// if to.t=T-2; otherwise null
		double [][] Y;
		double [] Z2;
		
		// length of the artc
		double length = 0;
		
		Arc(Node from, Node to){
			this.from = from;
			this.to = to;
			X_n = new double[nScenario][nSBar];
			X_c = new double[nScenario][nSBar];
			Z = new double[nScenario];
			if (to.t == problem.nStage-2) {
				Y = new double[nScenario][nSBar];
				Z2 = new double[nScenario];
			}
			setOccupancyMeasures();
			setLength();
		}
		
		void setOccupancyMeasures() {
			
			double[][] Xn_prime = from.X_n;			
			double[][] Xc_prime = from.X_c;
			double[]   Z_prime = from.Z;
			
			scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
				for(int j=0;j<nSBar;j++) {
					
					double val = 0;
					for(int i=0;i<nSBar;i++) {
						val += Xn_prime[l][i] * problem.scenarios[l].P[i][0][j] + Xc_prime[l][i] * problem.scenarios[l].P[i][1][j];
					}
					
					if(to.pi[j]==false) {
						X_n[l][j] = val;
						X_c[l][j] = 0;
					}else {
						X_n[l][j] = 0;
						X_c[l][j] = val;
					}
				}
				Z[l] = Z_prime[l];
				for(int i=0;i<nSBar;i++) {
					Z[l] += Xn_prime[l][i] * problem.scenarios[l].Q[i][0] + Xc_prime[l][i] * problem.scenarios[l].Q[i][1];
				}
			});
			
			if(to.t==problem.nStage-2) {
				scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
					for(int j=0;j<nSBar;j++) {
						Y[l][j] = 0;
						for(int i=0;i<nSBar;i++) {
							Y[l][j] += X_n[l][i] * problem.scenarios[l].P[i][0][j] + X_c[l][i] * problem.scenarios[l].P[i][1][j];
						}
					}
					Z2[l] = Z[l];
					for(int i=0;i<nSBar;i++) {
						Z2[l] += X_n[l][i] * problem.scenarios[l].Q[i][0] + X_c[l][i] * problem.scenarios[l].Q[i][1];
					}
				});
			}
		}
	
		void setLength() {
			
			double[] scenarioReward = new double[nScenario];
			scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
				scenarioReward[l] = 0;
				for(int i=0;i<nSBar;i++) {
					scenarioReward[l] += X_n[l][i]*problem.scenarios[l].r[i][0] + X_c[l][i]*problem.scenarios[l].r[i][1] + Z[l] * problem.rD; 
				}
			});
			length = 0;
			for(int i=0;i<nScenario;i++) length += scenarioReward[i];
			
			if(to.t == problem.nStage-2) {
				double[] scenarioReward2 = new double[nScenario];
				scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
					scenarioReward2[l] = 0;
					for(int i=0;i<nSBar;i++) {
						scenarioReward2[l] += Y[l][i]*problem.scenarios[l].R[i] + Z2[l]*problem.rD;
					}
				});
				for(int i=0;i<problem.scenarios.length;i++) length += scenarioReward2[i];
			}
		}
	
		public double[] getCapacityUsage() {
			
			// computes and returns capacity usages for each scenario
			
			double[] usage = new double[nScenario];
			scenarioIndexes.parallelStream().forEach((l)->{  // parallel for each scenario
					double use = 0;
					for(int i=0;i<nSBar;i++) {
						use += X_c[l][i];
					}
					usage[l] = problem.nPop * use;
			});
			
			return usage;
		}

		public boolean isFeasible() {
			
			// checks whether the stage policy is feasible or not
			
			double[] usages = getCapacityUsage();
			boolean flag = true;
			for(int i=0;i<nScenario;i++) {
				if(usages[i] > problem.capacity[to.t]) {
					flag = false;
					break;
				}
			}
			return flag;
		}
	
	}
	
	void construct() {
		
		// only nodes are constructed. Arcs will be constructed on the way
		List<List<Boolean>> x = new ArrayList<List<Boolean>>();
		List<Boolean> temp =  new ArrayList<Boolean>();
		temp.add(false);
		temp.add(true);
		for(int i=0;i<nSBar;i++) {
			x.add(temp);
		}
		List<List<Boolean>> combinations = cartesianProduct(x);
		for(int t=0;t<nTBar;t++) {
			ArrayList<Node> timeNodes = new ArrayList<Graph.Node>();
			for(List<Boolean> c: combinations) {
				boolean[] pi = new boolean[nSBar];
				for(int i=0;i<nSBar;i++) {
					pi[i] = c.get(i);
				}
				Node curr = new Node(t, pi);
				timeNodes.add(curr);
			}
			tNodeMap.put(t, timeNodes);
		}
	}
	
	void runADP() {
		
		double startTime = System.nanoTime();
		
		//INITIALIZATION: select the node among the ones related to t=0 which satisfies the feasibility
		ArrayList<Node> t_0_feasibleNodes = new ArrayList<Node>();
		for(Node node: tNodeMap.get(0)) {
			if(node.isFeasible()) t_0_feasibleNodes.add(node);
		}
		
		// ITERATIONS:
		for(int t=1;t<nTBar;t++) {
			for(Node node: tNodeMap.get(t)) {
				ArrayList<Arc> arcs = new ArrayList<Arc>();
				if(t==1) {
					for(Node node2: t_0_feasibleNodes) {
						Arc candidateArc = new Arc(node2, node);
						if (candidateArc.isFeasible()) arcs.add(candidateArc);
					}
				}else {
					for(Node node2: tNodeMap.get(t-1)) {
						if(node2.previous != null) {
							Arc candidateArc = new Arc(node2, node);
							if (candidateArc.isFeasible()) arcs.add(candidateArc);
						}
					}
				}
				if(arcs.size()==0) continue;
				else {
					HashMap<Arc, Double> arcMap = new HashMap<Arc, Double>();
					for(Arc arc: arcs) arcMap.put(arc, arc.from.value + arc.length);
					Arc bestArc = getBestArc(arcMap);
					node.value = bestArc.from.value + bestArc.length;
					node.previous = bestArc.from;
					node.setOccupancyMeasures(bestArc.X_n, bestArc.X_c, bestArc.Z);
				}
			}
		}
		
		// GET OBJ VALUE
		for(Node node: tNodeMap.get(nTBar-1)) {
			if (node.value > totalLength) totalLength = node.value;
		}
		totalLength /= nScenario;
		elapsedTime = (System.nanoTime() - startTime)/1000000000;  // in terms of seconds
	}
	
	static Arc getBestArc(HashMap<Arc, Double> arcMap) {
		Arc bestArc = null;
		Double bestVal = -1000000.0;
		for(Arc arc: arcMap.keySet()) {
			if (arcMap.get(arc)>bestVal) {
				bestArc = arc;
				bestVal = arcMap.get(arc);
			}
		}
		return bestArc;
	}
	
	static protected <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> resultLists = new ArrayList<List<T>>();
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<T>());
            return resultLists;
        } else {
            List<T> firstList = lists.get(0);
            List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (T condition : firstList) {
                for (List<T> remainingList : remainingLists) {
                    ArrayList<T> resultList = new ArrayList<T>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
	}

	double getObjVal() {
		return totalLength;
	}
	
	double getElapsedTime() {
		return elapsedTime;
	}
	
	void setPath() {
		
		// get the best node from t=T-1
		ArrayList<Node> nodesLast = tNodeMap.get(nTBar-1);
		Node currentNode = null;
		double bestLength = -10000000;
		for(Node node: nodesLast) {
			if(node.value > bestLength) {
				currentNode = node;
				bestLength = node.value;
			}
		}
		path.add(currentNode);
		
		// start recursions
		Node prevNode = new Node(currentNode.previous);
		while(true) {
			currentNode = new Node(prevNode);
			path.add(currentNode);
			if(currentNode.previous ==  null) break;
			else prevNode = new Node(currentNode.previous);
		}
		
		// reverse it
		Collections.reverse(path);
	}
	
	void setStrategy() {
		
		// be aware of that path is already constructed
		for(int t=0;t<nTBar;t++) {
			Node currentNode = path.get(t);
			for(int i=0;i<nSBar;i++) {
				if(currentNode.pi[i]) strategy[t][i] = 1;
				else strategy[t][i] = 0;
			}
		}
	}

	void displayPath() {
		for(Node node:path) {
			System.out.print("time: "+node.t + "---> ");
			for(int i=0;i<nSBar;i++) {
				if(i < nSBar -1) {
					if(node.pi[i]) System.out.print("1-");
					else System.out.print("0-");
				}else {
					if(node.pi[i]) System.out.print("1");
					else System.out.print("0");
				}
			}
			System.out.println();
		}
	}
	
	void displayStrategy() {
		for(int t=0;t<nTBar;t++) {
			for(int i=0;i<nSBar;i++) {
				if(i<nSBar-1) System.out.print(strategy[t][i]+"-");
				else System.out.print(strategy[t][i]);
			}
			System.out.println();
		}
	}

	void writeStrategy() throws IOException {
		
		int T = nTBar + 1;
		String fileName = nScenario + "-" + T + ".xlsx";
		Workbook wb = new XSSFWorkbook();
		XSSFSheet sheet = (XSSFSheet) wb.createSheet("sheet1");
		
		Row row = sheet.createRow(0);
		for(int i=0;i<nSBar;i++) {
			String name = "State_" + i;
			Cell cell = row.createCell(i); 
			cell.setCellValue(name);
		}
		
		for(int t=0;t<nTBar;t++) {
			row = sheet.createRow(t+1);
			for(int i=0;i<nSBar;i++) {
				Cell cell = row.createCell(i);
				cell.setCellValue(strategy[t][i]);
			}
		}
		
		FileOutputStream fileOut = new FileOutputStream(fileName);
		wb.write(fileOut);
		fileOut.close();
		wb.close();
	}

}
