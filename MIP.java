package version_2;

//Libraries

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import version_2.Problem.MDP;


public class MIP {
	
	// cplex object
	IloCplex cplex;

	// problem parameters
	Problem problem;

	// first-stage decision variables
	IloNumVar [][] pi;

	// second-stage decision variables
	IloNumVar [][][][] X;
	IloNumVar [][] Y;
    IloNumVar [][] Z;

    // fill after the optimal solution is found
    double objVal;
	double elapsedTime;
	double relativeGap;
	
	int [][] policy; 
	
	public MIP(Problem problem) throws IloException{

		// constructor method
		
		cplex= new IloCplex();
		this.problem = problem;

		pi = new IloNumVar[problem.nStage-1][problem.nState-1];
		X = new IloNumVar[problem.scenarios.length][problem.nStage-1][problem.nState-1][2];
		Y = new IloNumVar[problem.scenarios.length][problem.nState-1];
		Z = new IloNumVar[problem.scenarios.length][problem.nStage];
		
		policy = new int[problem.nStage-1][problem.nState-1];
	}
	
	public void defineFirstStageVariables() throws IloException{
		
		// defines the decision-variables for the first stage
		
		for(int t=0;t<problem.nStage-1;t++){
			for(int i=0;i<problem.nState-1;i++){
				pi[t][i]=cplex.boolVar("pi_"+t+"_"+i);
			}
		}
	}

	public void defineSecondStageVariables() throws IloException{
		
		// defines the decision-variables for the first stage

		for(int s=0;s<problem.scenarios.length;s++){
			for(int t=0;t<problem.nStage-1;t++){
				for(int i=0;i<problem.nState-1;i++){
					X[s][t][i][0] = cplex.numVar(0, 1, "X_" + s + "_" + t + "_" + i + "_" + 0);
					X[s][t][i][1] = cplex.numVar(0, 1, "X_" + s + "_" + t + "_" + i + "_" + 1);
				}
			}
		}

		for(int s=0;s<problem.scenarios.length;s++){
			for(int t=0;t<problem.nStage;t++){
				Z[s][t] = cplex.numVar(0, 1, "Z_" + s + "_" + t);
			}
		}

		for(int s=0;s<problem.scenarios.length;s++){
			for(int i=0;i<problem.nState-1;i++){
				Y[s][i] = cplex.numVar(0, 1, "Y_" + s + "_" + i);
			}
		}
	}
	
	public void setObjectiveFunction2() throws IloException{
		
		IloLinearNumExpr objTerms=cplex.linearNumExpr()	;
		for(int s=0;s<problem.scenarios.length;s++){
			for(int t=1;t<problem.nStage;t++){
				objTerms.addTerm(Z[s][t], 1.0);
			}
		}
		cplex.addMinimize(objTerms);
	}
	
	public void setObjectiveFunction() throws IloException
	{

		IloLinearNumExpr objTerms=cplex.linearNumExpr()	;

		for(int s=0;s<problem.scenarios.length;s++){
			for(int t=1;t<problem.nStage;t++){
				objTerms.addTerm(Z[s][t], problem.rD);
			}
		}

		for(int s=0;s<problem.scenarios.length;s++)
		{
			MDP mdp = problem.scenarios[s];
			for(int t=0;t<problem.nStage-1;t++){
				for(int i=0;i<problem.nState-1;i++){
					objTerms.addTerm(X[s][t][i][0], mdp.r[i][0]);
					objTerms.addTerm(X[s][t][i][1], mdp.r[i][1]);
				}
			}
		}

		for(int s=0;s<problem.scenarios.length;s++){
			MDP mdp = problem.scenarios[s];
			for(int i=0;i<problem.nState-1;i++){
				objTerms.addTerm(Y[s][i], mdp.R[i]);
			}
		}

		// be aware of that we omit the constant 'n/L'
		cplex.addMaximize(objTerms);
	}

	public void setPriorFlows() throws IloException{
		
		for(int s=0;s<problem.scenarios.length;s++){
			for(int i=0;i<problem.nState-1;i++){
				IloLinearNumExpr out =cplex.linearNumExpr()	;
				out.addTerm(X[s][0][i][0], 1.0);
				out.addTerm(X[s][0][i][1], 1.0);
				cplex.addEq(out, problem.priors[i]);
			}
		}
	}

	public void setIntermediateFlows() throws IloException{
		
		for(int s=0;s<problem.scenarios.length;s++){
			MDP mdp = problem.scenarios[s];
			for(int t=1;t<problem.nStage-1;t++){
				for(int j=0;j<problem.nState-1;j++){
					IloLinearNumExpr in =cplex.linearNumExpr();
					for(int i=0;i<problem.nState-1;i++){
						in.addTerm(X[s][t-1][i][0], mdp.P[i][0][j]);
						in.addTerm(X[s][t-1][i][1], mdp.P[i][1][j]);
					}
					IloLinearNumExpr out =cplex.linearNumExpr();
					out.addTerm(X[s][t][j][0], 1.0);
					out.addTerm(X[s][t][j][1], 1.0);

					cplex.addEq(in, out);
				}
			}
		}
	}
	
	public void setAbsorbingFlows() throws IloException{
		
		for(int s=0;s<problem.scenarios.length;s++){
			MDP mdp = problem.scenarios[s];
			cplex.addEq(Z[s][0], 0);
			for(int t=1;t<problem.nStage;t++){
				IloLinearNumExpr in =cplex.linearNumExpr();
				for(int i=0;i<problem.nState-1;i++){
					in.addTerm(X[s][t-1][i][0], mdp.Q[i][0]);
					in.addTerm(X[s][t-1][i][1], mdp.Q[i][1]);
				}
				in.addTerm(Z[s][t-1], 1.0);
				cplex.addEq(in, Z[s][t]);
			}
		}
	}

	public void setTerminalFlows() throws IloException{
		
		for(int s=0;s<problem.scenarios.length;s++){
			MDP mdp = problem.scenarios[s];
			for(int j=0;j<problem.nState-1;j++){
				IloLinearNumExpr in =cplex.linearNumExpr();
				for(int i=0;i<problem.nState-1;i++){
					in.addTerm(X[s][problem.nStage-2][i][0], mdp.P[i][0][j]);
					in.addTerm(X[s][problem.nStage-2][i][1], mdp.P[i][1][j]);
				}
				cplex.addEq(in, Y[s][j]);
			}
		}
	}
	
	public void setCapacityConstraint() throws IloException{
		
		for(int t=0;t<problem.nStage-1;t++){
			for(int s=0;s<problem.scenarios.length;s++){
				IloLinearNumExpr expectedUse = cplex.linearNumExpr();
				for(int i=0;i<problem.nState-1;i++){
					expectedUse.addTerm(X[s][t][i][1], problem.nPop);
				}
				cplex.addLe(expectedUse, problem.capacity[t]);
			}
		}
	}

	public void linkStages() throws IloException{
		
		for(int s=0;s<problem.scenarios.length;s++){
			for(int t=0;t<problem.nStage-1;t++){
				for(int i=0;i<problem.nState-1;i++){
					cplex.addLe(X[s][t][i][1], pi[t][i]);
					cplex.addLe(X[s][t][i][0], cplex.sum(1, cplex.prod(-1, pi[t][i])));
				}
			}
		}
	}

	public void addValid1() throws IloException{
		
		for(int s=0;s<problem.scenarios.length;s++){
			for(int t=0;t<problem.nStage;t++){
				IloLinearNumExpr sum = cplex.linearNumExpr();
				if(t<problem.nStage-1){
					for(int i=0;i<problem.nState-1;i++){
						sum.addTerm(X[s][t][i][0], 1.0);
						sum.addTerm(X[s][t][i][1], 1.0);
					}
					if(t>0){
						sum.addTerm(Z[s][t], 1.0);
					}
				}else{
					for(int i=0;i<problem.nState-1;i++){
						sum.addTerm(Y[s][i], 1.0);
					}
					sum.addTerm(Z[s][t], 1.0);
				}
				cplex.addEq(sum, 1.0);
			}
		}
	}

	public void addValid2() throws IloException{
		
		double population = problem.nPop;
		double totalCap = 0;
		for(int t=0;t<problem.nStage-1;t++){
			totalCap += problem.capacity[t];
		}
		double rhs = problem.nStage - (totalCap + population) / population;

		for(int s=0;s<problem.scenarios.length;s++){
			IloLinearNumExpr lhs = cplex.linearNumExpr();
			for(int t=0;t<problem.nStage-1;t++){
				lhs.addTerm(Z[s][t], 1.0);
				for(int i=0;i<problem.nState-1;i++){
					lhs.addTerm(X[s][t][i][0], 1.0);
				}
			}
			cplex.addGe(lhs, rhs);
		}
	}

	public void solve(boolean isBender, boolean isValid1, boolean isValid2, double timeLim) throws IloException{
		
		double startTime = System.nanoTime();

		if (isBender) cplex.setParam(IloCplex.Param.Benders.Strategy, 3);

		if (timeLim>0) cplex.setParam(IloCplex.Param.TimeLimit	, timeLim);

		defineFirstStageVariables();
		defineSecondStageVariables();
		setObjectiveFunction();
		setPriorFlows();
		setIntermediateFlows();
		setAbsorbingFlows();
		setTerminalFlows();
		setCapacityConstraint();
		linkStages();

		if (isValid1) addValid1();
		if (isValid2) addValid2();

		if(cplex.solve()){
			objVal = cplex.getObjValue() / problem.scenarios.length;  // now, we just ignore "n" instead of "n/L"			
			elapsedTime = (System.nanoTime() - startTime)/1000000000;  // in terms of seconds			
			relativeGap = cplex.getMIPRelativeGap();
			for(int t=0;t<problem.nStage-1;t++) {
				for(int i=0;i<problem.nState-1;i++) {
					if(cplex.getValue(pi[t][i]) > 0.00001 && cplex.getValue(X[0][t][i][1])> 0.00001) policy[t][i] = 1;
					else policy[t][i] = 0;
				}
			}
		}
	}
	
	public double getObjVal(){
		return objVal;
	}

	public double getElapsedTime(){
		return elapsedTime;
	}

	public double getRelativeGap(){
		return relativeGap;
	}
	
	void displayStrategy() {
		for(int t=0;t<problem.nStage-1;t++) {
			for(int i=0;i<problem.nState-1;i++) {
				if(i<problem.nState-1) System.out.print(policy[t][i]+"-");
				else System.out.print(policy[t][i]);
			}
			System.out.println();
		}
	}

}
