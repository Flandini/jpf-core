import gov.nasa.jpf.Config;
import gov.nasa.jpf.ListenerAdapter;

import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.Path;
import gov.nasa.jpf.vm.Transition;
import gov.nasa.jpf.vm.RestorableVMState;

import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.search.SearchState;

import java.io.PrintWriter;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;

public class PathCountEstimator extends ListenerAdapter {
    private final PrintWriter   m_out;
    private final StringBuilder m_buffer    = new StringBuilder();
    private final Formatter     m_formatter = new Formatter(m_buffer);
    private final int           m_logPeriod;
    private       long          m_nextLog;
    private       long          m_startTime;

    private       double        m_progress;
    private       double        m_lastRecordedProgress;
    private       long          m_pathNum;
    private       long          m_lastRecordedPathNum;
    private       boolean       m_backtracked;
    private       long          m_actionNum;

    //*************************************************************************
    //
    // Monte Carlo Estimate related
    //
    //*************************************************************************
    private boolean doEstimate = false;
    private int numSamples = 1;
    private List<Integer> samples;
    private RestorableVMState originalState;

    private float seenTerminalNodes = 0;
    private float estimatedNumTerminalNodes = 1;

    private ProgressPrinter printer;

    public PathCountEstimator(Config config)
    {
        m_out       = new PrintWriter(System.out, true);
        m_logPeriod = config.getInt("jpf.path_count_estimator.log_period", 0);
        printer = new ProgressPrinter();
        samples = new LinkedList<Integer>();
    }

    @Override
    public void searchStarted(Search search)
    {
        m_nextLog     = 0;
        m_startTime   = System.currentTimeMillis();
        m_progress    = 0.0;
        m_lastRecordedProgress = -1;
        m_pathNum     = 0;
        m_lastRecordedPathNum = -1;
        m_backtracked = false;
        m_actionNum   = 0;
    }

    @Override
    public void searchFinished(Search search)
    {
      System.out.println();
      System.out.println("Estimated data: " + seenTerminalNodes + " / " + estimatedNumTerminalNodes);
      log();
    }

    private void estimateTerminalNodes() {
      float acc = 0;
      double sampleMean = 0.0;

      for (Integer sample : samples)
        acc += sample;

      sampleMean = (double) acc / samples.size();

      // Normal rounding (i.e. 3.5 -> 4, 4.49999... -> 4)
      estimatedNumTerminalNodes = (float) Math.round(sampleMean);
    }

    @Override
    public void stateAdvanced(Search search)
    {
        m_actionNum++;

        if (doEstimate) {
          System.out.println("Starting trials");
          VM vm = search.getVM();
          originalState = vm.getRestorableState();

          boolean finished = false;

          for (int i = 0; i < 50; ++i) {
            while (!finished) {
              if (vm.forward()) {
                if (vm.isEndState()) {
                  Path path = vm.getPath();
                  Integer choicesAlongPath = 1;

                  for (Transition trans : path) {
                    ChoiceGenerator cg = trans.getChoiceGenerator();
                    choicesAlongPath *= cg.getTotalNumberOfChoices();
                  }

                  samples.add(choicesAlongPath);
                  break;
                }

              } else {
                vm.restoreState(originalState);
                vm.resetNextCG();
              }
            }

            finished = false;
            vm.restoreState(originalState);
            vm.resetNextCG();
          }

          vm.restoreState(originalState);
          vm.resetNextCG();
          estimateTerminalNodes();
          doEstimate = false;
          System.out.println("Finished trials.");
        }

        if (m_backtracked) {
            if (m_nextLog > System.currentTimeMillis()) {
                return;
            }
            if (log(search)) {
                m_nextLog = m_logPeriod + System.currentTimeMillis();
            }
            m_backtracked = false;
        }

        if (search.isEndState() || search.isErrorState() || search.isVisitedState()) {
            m_pathNum++;
            updateProgress(search);
            // updateUsingEstimate(search); For MonteCarlo estimation
            return;
        }
    }

    @Override
    public void stateBacktracked(Search search)
    {
        m_actionNum++;
        m_backtracked = true;
    }

    private void updateUsingEstimate(Search search) {
      seenTerminalNodes += 1;
    }

    private void updateProgress(Search search)
    {
        VM vm = search.getVM();
        Path path = vm.getPath();
        double pathProbability = 1.0;

        for (int i = 0; i < path.size(); i++) {
            Transition transition = path.get(i);
            ChoiceGenerator cg = transition.getChoiceGenerator();
            pathProbability /= cg.getTotalNumberOfChoices();
        }

        m_progress += pathProbability;
    }

    private boolean log(Search search)
    {
        if (m_lastRecordedPathNum >= m_pathNum) {
            return false;
        }
        m_lastRecordedPathNum = m_pathNum;

        if (m_progress - m_lastRecordedProgress <= 0.01)
            return false;
        m_lastRecordedProgress = m_progress;

        log();

        return true;
    }

    private void log() {
        long expectedPathNum = (long) (m_pathNum / m_progress);

        long currentTime   = System.currentTimeMillis() - m_startTime;
        long expectedTime  = (long) (currentTime / m_progress);

        if (m_progress <= 0.0 + 0.00001) {
          printer.printProgress(0, 0, 1);
        } else {
          printer.printProgress(100.0 * m_progress, m_pathNum, expectedPathNum);
        }

        m_formatter.format("  [PATH]:  %,d / %,d (%g%%)    Time to finish:  ", m_pathNum, expectedPathNum, 100.0 * m_progress);
        formatTime(expectedTime - currentTime);

        m_out.println(m_buffer.toString());
        m_buffer.setLength(0);
    }

    private void formatTime(long time)
    {
        long days, hours, minutes, seconds;
        boolean commit;

        seconds = time / 1000;
        minutes = seconds / 60;
        hours   = minutes / 60;
        days    = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours   %= 24;

        commit = false;

        if ((commit) || (days != 0))
            {
                commit = true;
                m_buffer.append(days);
                m_buffer.append(" days");
            }

        if ((commit) || (hours != 0))
            {
                if ((commit) && (hours < 10))
                    m_buffer.append('0');

                if (commit) {
                    m_buffer.append(" ");
                }
                m_buffer.append(hours);
                m_buffer.append(" hours");
                commit = true;
            }

        if ((commit) || (minutes != 0))
            {
                if ((commit) && (minutes < 10))
                    m_buffer.append('0');

                if (commit) {
                    m_buffer.append(" ");
                }
                m_buffer.append(minutes);
                m_buffer.append(" minutes");
                commit = true;
            }

        if ((commit) && (seconds < 10))
            m_buffer.append('0');

        if (commit) {
            m_buffer.append(" ");
        }
        m_buffer.append(seconds);
        m_buffer.append(" seconds");
    }
}
