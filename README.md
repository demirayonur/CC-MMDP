# CC-MMDP

The deterministic mixed integer programming equivalent `(MIP-MMDP)`, the so-called extensive-form, of the 2-stage stochastic integer programming model developed for Capacity Constrained Multi-model Markov Decision Processes `(CC-MMDP)` is implemented in Java programming language, and solved by CPLEX 12.10. It is optional to use Benders decomposition and the proposed valid inequalities. We also provide a parallel approximate dynamic programing `(PADP)` algorithm to get a good approximation to the optimal solution in reasonable time spans.

`(MIP-MMDP)` is a novel formulation, which converts the traditional MDP model with known parameters and no capacity constraints to a new model with uncertain parameters and a resource capacity constraint. The motivation behind `(CC-MMDP)` and the technical details related to `(MIP-MMDP)` and `(PADP)` are explained in detail in the following paper:

paper link: TBD

