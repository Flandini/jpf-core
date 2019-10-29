package gov.nasa.jpf.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.RestorableVMState;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Transition;

import java.io.PrintWriter;
import java.util.Formatter;


public class MonteCarlo extends Search {
  public MonteCarlo(Config config, VM vm) {
    super(config, vm);
  }

  @Override
  public void search () {
    int    depth = 0;
    int paths = 0;
    depth++;

    if (hasPropertyTermination()) {
      return;
    }

    //vm.forward();
    RestorableVMState init_state = vm.getRestorableState();
    notifySearchStarted();

    while (!done) {
      if ((depth < depthLimit) && forward()) {
        notifyStateAdvanced();

        long numChoices = 0;
        VM vm = getVM();
        Path path = vm.getPath();
        Transition transition = path.get(path.size() - 1);
        ChoiceGenerator cg = transition.getChoiceGenerator();
        numChoices += cg.getTotalNumberOfChoices();
        System.out.println("Num choices: " + numChoices);

        if (currentError != null){
          notifyPropertyViolated();

          if (hasPropertyTermination()) {
            return;
          }
        }

        if (isEndState()){
          return;
        }

        depth++;

      } else { // no next state or reached depth limit
        // <2do> we could check for more things here. If the last insn wasn't
        // the main return, or a System.exit() call, we could flag a JPFException
        if (depth >= depthLimit) {
          notifySearchConstraintHit("depth limit reached: " + depthLimit);
        }
        checkPropertyViolation();
        //done = (paths >= path_limit);
        paths++;
        System.out.println("paths = " + paths);
        depth = 1;
        vm.restoreState(init_state);
        vm.resetNextCG();
      }
    }
    notifySearchFinished();
  }
}
