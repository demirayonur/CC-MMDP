# CC-MMDP

The deterministic mixed integer programming equivalent `(MIP-MMDP)`, the so-called extensive-form, of the 2-stage stochastic integer programming model developed for Capacity Constrained Multi-model Markov Decision Processes `(CC-MMDP)` is implemented in Java programming language, and solved by CPLEX 12.10. It is optional to use Benders decomposition and the proposed valid inequalities. We also provide a parallel approximate dynamic programing `(PADP)` algorithm to get a good approximation to the optimal solution in reasonable time spans.

`(MIP-MMDP)` is a novel formulation, which converts the traditional MDP model with known parameters and no capacity constraints to a new model with uncertain parameters and a resource capacity constraint. The motivation behind `(CC-MMDP)` and the technical details related to `(MIP-MMDP)` and `(PADP)` are explained in detail in the following paper:

paper link: TBD

## 1. Data

The data generated by the Monte Carlo approach described in the paper are provided in the `Data` folder under which there are distinct folders for each scenario. What a scenario folder includes is nothing but just 3 *.csv* files to characterize the associated MDP components. *P.csv* consists of the transition probabilities among non-absorbing states, *Q.csv* includes the transition probabilities from non-absorbing states to the only absorbing state in the model, and *r.csv* stands for the immediate rewards. It is worth to mention the caveat that these .csv files are flattened before writing. Thus, it is necessary to reshape them after they are read. 

In order to create an `MDP` object from a scenario, it is enough to create an appropriate HashMap and use the construction method with the one taking the HashMap as an argument. Following is an example of how you can read the `MDP` from the file called `scenario_1`:

```java
String path_ = "your_path/Data/scenario_1/" // you should introduce the specific path of the Data folder in your own local
HashMap<String, String> csvFiles = new HashMap<String, String>();
csvFiles.put("P", path_ + "P.csv");
csvFiles.put("Q", path_ + "Q.csv");
csvFiles.put("r", path_ + "r.csv");
MDP our_mdp = new MDP(csvFiles);
```

Depending on your aim, a `Problem` object may include more than one scenario (generally this happens!). The desired number of scenarios is provided as an argument. Instead of randomly selecting which scenarios are involved, we count the scenario indexes starting from 1 to the desired number. For example, if the user wants to have 5 scenarios, then MDPs encoded through the following directories are read and stored in an array.

- scenario_1
- scenario_2
- scenario_3
- scenario_4
- scenario_5

The user is expected to provide the desired number of scenarios, the path for the parent directory, the prior probability distribution over the set of non-absorbing states, the total number of individuals, and the allowed capacity for each decision epoch in which the resource can be utilized. Following is an example of a `Problem` object with 200 scenarios, equal prior probabilities for each state, 1000 individuals, and 400 capacity for each decision epoch.

```java
String parentDic = "...your_path.../Data/";  // the path to the folder which includes the scenario folders
int nStage = 41;  // Let's say we have 40 stages where we can take an action
double[] priors = {1.0/6, 1.0/6, 1.0/6, 1.0/6, 1.0/6, 1.0/6};  // equal prior probabilities
int[] cap = new int[nStage-1];
for(int t=0;t<nStage-1;t++)
{
    cap[t] = 400;  // 400 capacity in each decision epoch
}
int nPop = 1000;
int nScenario = 200;
		
Problem problem = new Problem(nScenario, nPop, parentDic, cap, priors);
problem.read();
```
**Caveat:** In order to read and construct the MDPs, thus scenarios, you should employ `read()` method as in the above example.

**The data will become open to access after the paper is officially published in the journal to which it was submitted.**

## 2. Run MIP-MMDP with CPLEX

You should introduce the `Problem` object to the constructor method in `MIP.java` as follows in order to solve the MIP model developed for the problem instance.

```java
MIP solver = new MIP(problem);
```
Then, you are expected to provide your preferences for the solution algorithm. These are as follows:

- `isBenders`: *true* if you want to apply Benders decomposition algorithm; otherwise *false*
- `isValid1`: *true* if you want to append the valid inequalities in `Proposition 2.2.` in the paper; otherwise *false*
- `isValid2`: *true* if you want to append the valid inequalities in `Proposition 2.3.` in the paper; otherwise *false*
- `timeLim`: if it is greater than 0, it sets the time limit with that value; otherwise no time limit restricts the solver.

> Example Code 1: Applies Benders cuts, but does not include any valid inequalities and no time limit takes place.

```java
boolean isBender = true;
boolean isValid1 = false;
boolean isValid2 = false;
double timeLim = 0.0;
solver.solve(isBender, isValid1, isValid2, timeLim);
```

> Example Code 2: Does not use Benders cuts, but appends both of the valid inequalities and restricts the algorithm with 100 seconds.
```java
boolean isBender = false;
boolean isValid1 = true;
boolean isValid2 = true;
double timeLim = 100.0;
solver.solve(isBender, isValid1, isValid2, timeLim);
```

It is possible to extract the objective function value and elapsed time in seconds once the algorithm finds the optimal solution. If you set a time limit and
the solver does not find the optimal solution, it is also possible to get the relative Gap value. Following shows how you can extract them.

```java
double objVal = solver.getObjVal();
double elapsedTime = solver.getElapsedTime();
double relativeGap = solver.getRelativeGap();
```

## 3. Run PADP

`PADP` is designed effectively with nested classes and parallel implementations. You should first create the `Graph.java` object which transforms the problem instance into a graph structure with appropriate nodes and arcs. Then, the user is expected to call `runADP()` method. Following is an example of how you
can solve the given problem instance with `PADP`.

```java
Graph graph = new Graph(problem);
graph.runADP();
```

It is possible to extract the objective function value and elapsed time in seconds once the algorithm finds a solution as follows:

```java
double objVal = graph.getObjVal();
double elapsedTime = graph.getElapsedTime();
```

Author: Onur Demiray

e-mail: odemiray18@ku.edu.tr

For your suggestions and/or questions, please do not hesitate to contact me via my e-mail address. I would also be glad to keep in touch with you if this repository
somehow helps you in your research or business.
